using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Application.Services;

public sealed class SponsorBillingJob : BackgroundService
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<SponsorBillingJob> _log;

    public SponsorBillingJob(IServiceScopeFactory scopeFactory, ILogger<SponsorBillingJob> log)
    {
        _scopeFactory = scopeFactory;
        _log = log;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            var now = DateTime.UtcNow;
            var nextRun = now.Date.AddDays(1).AddHours(1);
            var delay = nextRun - now;
            if (delay < TimeSpan.FromMinutes(1))
            {
                delay = TimeSpan.FromHours(24);
            }

            try
            {
                await Task.Delay(delay, stoppingToken);
                await GenerateAsync(stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _log.LogError(ex, "Falha na geração de cobranças de padrinhos");
            }
        }
    }

    public async Task GenerateAsync(CancellationToken ct)
    {
        using var scope = _scopeFactory.CreateScope();
        var sponsors = scope.ServiceProvider.GetRequiredService<ISponsorRepository>();
        var entries = scope.ServiceProvider.GetRequiredService<IFinancialEntryRepository>();

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var endOfMonth = new DateOnly(today.Year, today.Month, DateTime.DaysInMonth(today.Year, today.Month));
        var active = await sponsors.ListActiveAsync(ct);

        foreach (var sponsor in active)
        {
            var baseDate = sponsor.LastGeneratedDate
                ?? sponsor.StartDate.AddMonths(-1);
            var nextDue = sponsor.Frequency.NextDueDate(baseDate);
            if (nextDue > endOfMonth)
            {
                continue;
            }

            if (await entries.ExistsSponsorDueDateAsync(sponsor.Id, nextDue, ct))
            {
                continue;
            }

            var entry = FinancialEntry.CreateSponsorBilling(sponsor, nextDue, DateTime.UtcNow);
            await entries.AddAsync(entry, ct);
            sponsor.MarkGenerated(nextDue);
        }

        await entries.SaveChangesAsync(ct);
        await sponsors.SaveChangesAsync(ct);
        _log.LogInformation("Financeiro: geração automática de cobranças de padrinhos concluída.");
    }
}
