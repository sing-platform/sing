namespace Sing.Finance.Application.Contracts;

public interface IEventPublisher
{
    Task PublishPendingAsync(CancellationToken ct);
}
