using Microsoft.EntityFrameworkCore;
using Sing.Finance.Application.Configuration;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Services;
using Sing.Finance.Infrastructure.Core;
using Sing.Finance.Infrastructure.Messaging;
using Sing.Finance.Infrastructure.Persistence;
using Sing.Finance.Infrastructure.Persistence.Repositories;

namespace Sing.Finance.Infrastructure;

public static class DependencyInjection
{
    public static IServiceCollection AddFinance(this IServiceCollection services, IConfiguration configuration)
    {
        services.Configure<RabbitMqOptions>(configuration.GetSection(RabbitMqOptions.SectionName));
        services.Configure<CoreOptions>(configuration.GetSection(CoreOptions.SectionName));

        var connectionString = configuration.GetConnectionString("Finance")
            ?? throw new InvalidOperationException("ConnectionStrings:Finance não configurada.");

        services.AddDbContext<FinanceDbContext>(options =>
            options.UseMySql(connectionString, new MySqlServerVersion(new Version(8, 0, 36))));

        services.AddScoped<ISponsorRepository, SponsorRepository>();
        services.AddScoped<IFinancialEntryRepository, FinancialEntryRepository>();
        services.AddScoped<IFiscalDocumentRepository, FiscalDocumentRepository>();
        services.AddScoped<ICobrancaRepository, CobrancaRepository>();
        services.AddScoped<IOutboxRepository, OutboxRepository>();
        services.AddScoped<IEventPublisher, RabbitMqEventPublisher>();
        services.AddHttpClient<IStudentLookup, HttpStudentLookup>();

        services.AddScoped<SponsorService>();
        services.AddScoped<FinancialEntryService>();
        services.AddScoped<FiscalDocumentService>();
        services.AddScoped<CobrancaService>();

        services.AddHostedService<SponsorBillingJob>();
        services.AddHostedService<OverduePaymentJob>();

        return services;
    }

    public static async Task EnsureFinanceSchemaAsync(this IServiceProvider services)
    {
        using var scope = services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<FinanceDbContext>();
        await db.Database.MigrateAsync();
    }
}
