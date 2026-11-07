using Microsoft.EntityFrameworkCore;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Repositories;

public sealed class SponsorRepository : ISponsorRepository
{
    private readonly FinanceDbContext _db;

    public SponsorRepository(FinanceDbContext db) => _db = db;

    public Task<List<Sponsor>> ListAsync(CancellationToken ct) =>
        _db.Sponsors.OrderBy(x => x.Name).ToListAsync(ct);

    public Task<Sponsor?> GetByIdAsync(long id, CancellationToken ct) =>
        _db.Sponsors.FirstOrDefaultAsync(x => x.Id == id, ct);

    public Task<List<Sponsor>> ListActiveAsync(CancellationToken ct) =>
        _db.Sponsors.Where(x => x.IsActive).ToListAsync(ct);

    public async Task AddAsync(Sponsor sponsor, CancellationToken ct) =>
        await _db.Sponsors.AddAsync(sponsor, ct);

    public Task SaveChangesAsync(CancellationToken ct) => _db.SaveChangesAsync(ct);
}
