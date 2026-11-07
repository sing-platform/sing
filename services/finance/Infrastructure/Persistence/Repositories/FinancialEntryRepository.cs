using Microsoft.EntityFrameworkCore;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Repositories;

public sealed class FinancialEntryRepository : IFinancialEntryRepository
{
    private readonly FinanceDbContext _db;

    public FinancialEntryRepository(FinanceDbContext db) => _db = db;

    public async Task<List<FinancialEntry>> SearchAsync(
        DateOnly? start,
        DateOnly? finish,
        EntryDirection? direction,
        EntryStatus? status,
        string? category,
        CancellationToken ct)
    {
        var query = _db.FinancialEntries
            .Include(x => x.Sponsor)
            .Include(x => x.FiscalDocument)
            .Where(x => x.IsActive);

        if (start is not null)
        {
            query = query.Where(x => x.DueDate >= start);
        }

        if (finish is not null)
        {
            query = query.Where(x => x.DueDate <= finish);
        }

        if (direction is not null)
        {
            query = query.Where(x => x.Direction == direction);
        }

        if (status is not null)
        {
            query = query.Where(x => x.Status == status);
        }

        if (!string.IsNullOrWhiteSpace(category))
        {
            var term = category.Trim().ToLowerInvariant();
            query = query.Where(x => x.Category.ToLower().Contains(term));
        }

        return await query.OrderByDescending(x => x.DueDate).ThenByDescending(x => x.Id).ToListAsync(ct);
    }

    public Task<FinancialEntry?> GetByIdAsync(long id, CancellationToken ct) =>
        _db.FinancialEntries
            .Include(x => x.Sponsor)
            .Include(x => x.FiscalDocument)
            .FirstOrDefaultAsync(x => x.Id == id, ct);

    public Task<List<FinancialEntry>> ListBySponsorAsync(long sponsorId, CancellationToken ct) =>
        _db.FinancialEntries
            .Include(x => x.Sponsor)
            .Include(x => x.FiscalDocument)
            .Where(x => x.SponsorId == sponsorId)
            .OrderByDescending(x => x.DueDate)
            .ToListAsync(ct);

    public Task<bool> ExistsSponsorDueDateAsync(long sponsorId, DateOnly dueDate, CancellationToken ct) =>
        _db.FinancialEntries.AnyAsync(
            x => x.SponsorId == sponsorId && x.DueDate == dueDate && x.IsActive,
            ct);

    public Task<List<FinancialEntry>> FindPendingOverdueAsync(DateOnly referenceDate, CancellationToken ct) =>
        _db.FinancialEntries
            .Where(x => x.IsActive && x.Status == EntryStatus.PENDING && x.DueDate < referenceDate)
            .ToListAsync(ct);

    public async Task AddAsync(FinancialEntry entry, CancellationToken ct) =>
        await _db.FinancialEntries.AddAsync(entry, ct);

    public Task SaveChangesAsync(CancellationToken ct) => _db.SaveChangesAsync(ct);
}
