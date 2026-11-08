using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Domain;

namespace Sing.Finance.Application.Services;

public sealed class FinancialEntryService
{
    private readonly IFinancialEntryRepository _entries;
    private readonly ISponsorRepository _sponsors;
    private readonly IFiscalDocumentRepository _fiscals;

    public FinancialEntryService(
        IFinancialEntryRepository entries,
        ISponsorRepository sponsors,
        IFiscalDocumentRepository fiscals)
    {
        _entries = entries;
        _sponsors = sponsors;
        _fiscals = fiscals;
    }

    public async Task<Result<List<FinancialEntryResponse>>> ListAsync(
        string? month,
        EntryDirection? direction,
        EntryStatus? status,
        string? category,
        CancellationToken ct)
    {
        DateOnly? start = null;
        DateOnly? finish = null;
        if (!string.IsNullOrWhiteSpace(month))
        {
            if (!DateOnly.TryParseExact(month + "-01", "yyyy-MM-dd", out var first))
            {
                return Result<List<FinancialEntryResponse>>.Fail("Mês inválido. Use yyyy-MM.", 400);
            }

            start = first;
            finish = first.AddMonths(1).AddDays(-1);
        }

        var list = await _entries.SearchAsync(start, finish, direction, status, category, ct);
        return Result<List<FinancialEntryResponse>>.Ok(list.Select(FinancialEntryResponse.From).ToList());
    }

    public async Task<Result<FinancialEntryResponse>> GetByIdAsync(long id, CancellationToken ct)
    {
        var entry = await _entries.GetByIdAsync(id, ct);
        if (entry is null)
        {
            return Result<FinancialEntryResponse>.Fail("Lançamento financeiro não encontrado", 404);
        }

        return Result<FinancialEntryResponse>.Ok(FinancialEntryResponse.From(entry));
    }

    public async Task<Result<FinancialEntryResponse>> CreateAsync(
        FinancialEntryCreateRequest request,
        long? userId,
        CancellationToken ct)
    {
        try
        {
            var sponsor = await ResolveSponsorAsync(request.SponsorId, ct);
            if (request.SponsorId is not null && sponsor is null)
            {
                return Result<FinancialEntryResponse>.Fail("Padrinho não encontrado", 404);
            }

            var fiscal = await ResolveFiscalAsync(request.FiscalDocumentId, ct);
            if (request.FiscalDocumentId is not null && fiscal is null)
            {
                return Result<FinancialEntryResponse>.Fail("Documento fiscal não encontrado", 404);
            }

            var entry = FinancialEntry.Create(
                request.Direction,
                request.Status,
                request.DueDate,
                request.PaidAt,
                request.Amount,
                request.Category,
                request.Description,
                sponsor,
                fiscal,
                userId,
                DateTime.UtcNow);

            await _entries.AddAsync(entry, ct);
            await _entries.SaveChangesAsync(ct);
            return Result<FinancialEntryResponse>.Ok(FinancialEntryResponse.From(entry), 201);
        }
        catch (ArgumentException ex)
        {
            return Result<FinancialEntryResponse>.Fail(ex.Message, 400);
        }
    }

    public async Task<Result<FinancialEntryResponse>> UpdateAsync(
        long id,
        FinancialEntryUpdateRequest request,
        long? userId,
        CancellationToken ct)
    {
        var entry = await _entries.GetByIdAsync(id, ct);
        if (entry is null)
        {
            return Result<FinancialEntryResponse>.Fail("Lançamento financeiro não encontrado", 404);
        }

        try
        {
            var sponsor = await ResolveSponsorAsync(request.SponsorId, ct);
            if (request.SponsorId is not null && sponsor is null)
            {
                return Result<FinancialEntryResponse>.Fail("Padrinho não encontrado", 404);
            }

            var fiscal = await ResolveFiscalAsync(request.FiscalDocumentId, ct);
            if (request.FiscalDocumentId is not null && fiscal is null)
            {
                return Result<FinancialEntryResponse>.Fail("Documento fiscal não encontrado", 404);
            }

            entry.Update(
                request.Direction,
                request.Status,
                request.DueDate,
                request.PaidAt,
                request.Amount,
                request.Category,
                request.Description,
                sponsor,
                fiscal,
                request.Active,
                userId,
                DateTime.UtcNow);

            await _entries.SaveChangesAsync(ct);
            return Result<FinancialEntryResponse>.Ok(FinancialEntryResponse.From(entry));
        }
        catch (ArgumentException ex)
        {
            return Result<FinancialEntryResponse>.Fail(ex.Message, 400);
        }
    }

    public async Task<Result<FinancialEntryResponse>> MarkAsPaidAsync(
        long id,
        DateOnly paidAt,
        CancellationToken ct)
    {
        var entry = await _entries.GetByIdAsync(id, ct);
        if (entry is null)
        {
            return Result<FinancialEntryResponse>.Fail("Lançamento financeiro não encontrado", 404);
        }

        entry.MarkAsPaid(paidAt);
        await _entries.SaveChangesAsync(ct);
        return Result<FinancialEntryResponse>.Ok(FinancialEntryResponse.From(entry));
    }

    public async Task<Result> DeleteAsync(long id, long? userId, CancellationToken ct)
    {
        var entry = await _entries.GetByIdAsync(id, ct);
        if (entry is null)
        {
            return Result.Fail("Lançamento financeiro não encontrado", 404);
        }

        entry.Deactivate(userId, DateTime.UtcNow);
        await _entries.SaveChangesAsync(ct);
        return Result.Ok();
    }

    public async Task<Result<FinancialSummaryResponse>> MonthSummaryAsync(string month, CancellationToken ct)
    {
        if (!DateOnly.TryParseExact(month + "-01", "yyyy-MM-dd", out var first))
        {
            return Result<FinancialSummaryResponse>.Fail("Mês inválido. Use yyyy-MM.", 400);
        }

        var finish = first.AddMonths(1).AddDays(-1);
        var entries = await _entries.SearchAsync(first, finish, null, null, null, ct);

        decimal Sum(EntryDirection direction, EntryStatus? status) =>
            entries
                .Where(e => e.Direction == direction && (status is null || e.Status == status))
                .Sum(e => e.Amount);

        var income = Sum(EntryDirection.RECEIVABLE, null);
        var expense = Sum(EntryDirection.PAYABLE, null);
        return Result<FinancialSummaryResponse>.Ok(new FinancialSummaryResponse(
            income,
            expense,
            income - expense,
            Sum(EntryDirection.RECEIVABLE, EntryStatus.PENDING),
            Sum(EntryDirection.PAYABLE, EntryStatus.PENDING)));
    }

    private async Task<Sponsor?> ResolveSponsorAsync(long? sponsorId, CancellationToken ct)
    {
        if (sponsorId is null)
        {
            return null;
        }

        return await _sponsors.GetByIdAsync(sponsorId.Value, ct);
    }

    private async Task<FiscalDocument?> ResolveFiscalAsync(long? fiscalId, CancellationToken ct)
    {
        if (fiscalId is null)
        {
            return null;
        }

        return await _fiscals.GetByIdAsync(fiscalId.Value, ct);
    }
}
