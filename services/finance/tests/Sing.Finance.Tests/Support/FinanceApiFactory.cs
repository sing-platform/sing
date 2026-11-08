using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.Extensions.Hosting;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Infrastructure.Persistence;

namespace Sing.Finance.Tests.Support;

public sealed class FinanceApiFactory : WebApplicationFactory<Program>
{
    public const string JwtSecret = "dGVzdC1zZWNyZXQta2V5LWF0LWxlYXN0LTMyLWJ5dGVzISE=";

    public FakeStudentLookup Students { get; } = new();

    static FinanceApiFactory()
    {
        Environment.SetEnvironmentVariable("JWT_SECRET", JwtSecret);
        Environment.SetEnvironmentVariable("Jwt__Secret", JwtSecret);
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Testing");
        builder.ConfigureAppConfiguration((_, config) =>
        {
            config.AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Jwt:Secret"] = JwtSecret,
                ["JWT_SECRET"] = JwtSecret,
                ["ConnectionStrings:Finance"] = "Server=unused;Database=unused;",
                ["Core:BaseUrl"] = "http://localhost",
                ["RabbitMq:Host"] = "localhost",
                ["RabbitMq:Port"] = "5672",
                ["RabbitMq:User"] = "sing",
                ["RabbitMq:Password"] = "sing",
                ["RabbitMq:Exchange"] = "sing.events"
            });
        });

        builder.ConfigureTestServices(services =>
        {
            services.RemoveAll<IHostedService>();

            services.RemoveAll<DbContextOptions<FinanceDbContext>>();
            services.RemoveAll<FinanceDbContext>();

            var dbName = "finance-tests-" + Guid.NewGuid().ToString("N");
            services.AddDbContext<FinanceDbContext>(options => options.UseInMemoryDatabase(dbName));

            services.RemoveAll<IStudentLookup>();
            services.AddSingleton<IStudentLookup>(Students);

            services.RemoveAll<IEventPublisher>();
            services.AddSingleton<IEventPublisher, NoOpEventPublisher>();
        });
    }

    protected override IHost CreateHost(IHostBuilder builder)
    {
        var host = base.CreateHost(builder);
        using var scope = host.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<FinanceDbContext>();
        db.Database.EnsureCreated();
        return host;
    }
}

public sealed class FakeStudentLookup : IStudentLookup
{
    private readonly Dictionary<long, StudentInfo> _students = new();

    public void Add(long id, string name) => _students[id] = new StudentInfo(id, name);

    public Task<StudentInfo?> FindAsync(long studentId, string? bearerToken, CancellationToken ct) =>
        Task.FromResult(_students.TryGetValue(studentId, out var student) ? student : null);
}

internal sealed class NoOpEventPublisher : IEventPublisher
{
    public Task PublishPendingAsync(CancellationToken ct) => Task.CompletedTask;
}
