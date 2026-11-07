using Microsoft.EntityFrameworkCore;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Repositories;

public sealed class OutboxRepository : IOutboxRepository
{
    private readonly FinanceDbContext _db;

    public OutboxRepository(FinanceDbContext db) => _db = db;

    public async Task AddAsync(OutboxMessage message, CancellationToken ct) =>
        await _db.OutboxMessages.AddAsync(message, ct);

    public Task<List<OutboxMessage>> ListPendingAsync(int take, CancellationToken ct) =>
        _db.OutboxMessages
            .Where(x => !x.Processed)
            .OrderBy(x => x.Id)
            .Take(take)
            .ToListAsync(ct);

    public Task SaveChangesAsync(CancellationToken ct) => _db.SaveChangesAsync(ct);
}
