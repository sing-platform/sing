using System.Text.Json;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using Sing.Finance.Application.Configuration;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Application.Services;

public sealed class OverduePaymentJob : BackgroundService
{
    private readonly IServiceScopeFactory _scopeFactory;
    private readonly ILogger<OverduePaymentJob> _log;

    public OverduePaymentJob(IServiceScopeFactory scopeFactory, ILogger<OverduePaymentJob> log)
    {
        _scopeFactory = scopeFactory;
        _log = log;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            try
            {
                await Task.Delay(TimeSpan.FromHours(1), stoppingToken);
                await MarkOverdueAsync(stoppingToken);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                break;
            }
            catch (Exception ex)
            {
                _log.LogError(ex, "Falha ao marcar lançamentos vencidos");
            }
        }
    }

    public async Task MarkOverdueAsync(CancellationToken ct)
    {
        using var scope = _scopeFactory.CreateScope();
        var entries = scope.ServiceProvider.GetRequiredService<IFinancialEntryRepository>();
        var outbox = scope.ServiceProvider.GetRequiredService<IOutboxRepository>();
        var publisher = scope.ServiceProvider.GetRequiredService<IEventPublisher>();
        var exchange = scope.ServiceProvider.GetRequiredService<IOptions<RabbitMqOptions>>().Value.Exchange;

        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var overdue = await entries.FindPendingOverdueAsync(today, ct);
        foreach (var entry in overdue)
        {
            entry.MarkOverdue();
            var payload = JsonSerializer.Serialize(new
            {
                eventId = Guid.NewGuid(),
                eventType = "PaymentOverdue",
                version = 1,
                occurredAt = DateTime.UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ"),
                aggregateId = entry.Id.ToString(),
                payload = new
                {
                    entryId = entry.Id,
                    amount = entry.Amount,
                    dueDate = entry.DueDate.ToString("yyyy-MM-dd"),
                    category = entry.Category,
                    sponsorId = entry.SponsorId
                }
            });

            await outbox.AddAsync(new OutboxMessage
            {
                Exchange = exchange,
                RoutingKey = "payment.overdue",
                Payload = payload,
                CreatedAt = DateTime.UtcNow,
                Processed = false
            }, ct);
        }

        if (overdue.Count > 0)
        {
            await entries.SaveChangesAsync(ct);
            await outbox.SaveChangesAsync(ct);
            await publisher.PublishPendingAsync(ct);
            _log.LogInformation("Marcou {Count} lançamentos como OVERDUE", overdue.Count);
        }
    }
}
