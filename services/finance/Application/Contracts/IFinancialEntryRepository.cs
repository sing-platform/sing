using Sing.Finance.Domain;

namespace Sing.Finance.Application.Contracts;

public interface IFinancialEntryRepository
{
    Task<List<FinancialEntry>> SearchAsync(
        DateOnly? start,
        DateOnly? finish,
        EntryDirection? direction,
        EntryStatus? status,
        string? category,
        CancellationToken ct);

    Task<FinancialEntry?> GetByIdAsync(long id, CancellationToken ct);
    Task<List<FinancialEntry>> ListBySponsorAsync(long sponsorId, CancellationToken ct);
    Task<bool> ExistsSponsorDueDateAsync(long sponsorId, DateOnly dueDate, CancellationToken ct);
    Task<List<FinancialEntry>> FindPendingOverdueAsync(DateOnly referenceDate, CancellationToken ct);
    Task AddAsync(FinancialEntry entry, CancellationToken ct);
    Task SaveChangesAsync(CancellationToken ct);
}
