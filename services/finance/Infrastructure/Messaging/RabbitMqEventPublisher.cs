using System.Text;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;
using RabbitMQ.Client;
using Sing.Finance.Application.Configuration;
using Sing.Finance.Application.Contracts;

namespace Sing.Finance.Infrastructure.Messaging;

public sealed class RabbitMqEventPublisher : IEventPublisher
{
    private readonly IOutboxRepository _outbox;
    private readonly RabbitMqOptions _options;
    private readonly ILogger<RabbitMqEventPublisher> _log;

    public RabbitMqEventPublisher(
        IOutboxRepository outbox,
        IOptions<RabbitMqOptions> options,
        ILogger<RabbitMqEventPublisher> log)
    {
        _outbox = outbox;
        _options = options.Value;
        _log = log;
    }

    public async Task PublishPendingAsync(CancellationToken ct)
    {
        var pending = await _outbox.ListPendingAsync(50, ct);
        if (pending.Count == 0)
        {
            return;
        }

        try
        {
            var factory = new ConnectionFactory
            {
                HostName = _options.Host,
                Port = _options.Port,
                UserName = _options.User,
                Password = _options.Password,
                RequestedConnectionTimeout = TimeSpan.FromSeconds(3)
            };

            using var connection = factory.CreateConnection();
            using var channel = connection.CreateModel();
            channel.ExchangeDeclare(_options.Exchange, ExchangeType.Topic, durable: true);

            foreach (var message in pending)
            {
                var body = Encoding.UTF8.GetBytes(message.Payload);
                channel.BasicPublish(
                    exchange: message.Exchange,
                    routingKey: message.RoutingKey,
                    basicProperties: null,
                    body: body);

                message.Processed = true;
                message.ProcessedAt = DateTime.UtcNow;
            }

            await _outbox.SaveChangesAsync(ct);
        }
        catch (Exception ex)
        {
            _log.LogWarning(ex, "RabbitMQ indisponível; outbox permanece pendente");
        }
    }
}
