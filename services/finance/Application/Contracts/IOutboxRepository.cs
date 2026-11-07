using Sing.Finance.Domain;

namespace Sing.Finance.Application.Contracts;

public interface IOutboxRepository
{
    Task AddAsync(OutboxMessage message, CancellationToken ct);
    Task<List<OutboxMessage>> ListPendingAsync(int take, CancellationToken ct);
    Task SaveChangesAsync(CancellationToken ct);
}
