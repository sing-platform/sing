using Sing.Finance.Domain;

namespace Sing.Finance.Application.Dtos;

public sealed record FinancialEntryCreateRequest(
    EntryDirection Direction,
    EntryStatus Status,
    DateOnly DueDate,
    DateOnly? PaidAt,
    decimal Amount,
    string Category,
    string? Description,
    long? SponsorId,
    long? FiscalDocumentId);

public sealed record FinancialEntryUpdateRequest(
    EntryDirection Direction,
    EntryStatus Status,
    DateOnly DueDate,
    DateOnly? PaidAt,
    decimal Amount,
    string Category,
    string? Description,
    long? SponsorId,
    long? FiscalDocumentId,
    bool Active);

public sealed record MarkEntryAsPaidRequest(DateOnly PaidAt);

public sealed record FinancialSummaryResponse(
    decimal Income,
    decimal Expense,
    decimal Balance,
    decimal PendingReceivable,
    decimal PendingPayable);

public sealed record FinancialEntryResponse(
    long Id,
    EntryDirection Direction,
    EntryStatus Status,
    DateOnly DueDate,
    DateOnly? PaidAt,
    decimal Amount,
    string Category,
    string? Description,
    long? SponsorId,
    string? SponsorName,
    long? FiscalDocumentId,
    string? FiscalDocumentNumber,
    bool Active)
{
    public static FinancialEntryResponse From(FinancialEntry entry) => new(
        entry.Id,
        entry.Direction,
        entry.Status,
        entry.DueDate,
        entry.PaidAt,
        entry.Amount,
        entry.Category,
        entry.Description,
        entry.SponsorId,
        entry.Sponsor?.Name,
        entry.FiscalDocumentId,
        entry.FiscalDocument?.DocumentNumber,
        entry.IsActive);
}
