using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Domain;

namespace Sing.Finance.Application.Services;

public sealed class SponsorService
{
    private readonly ISponsorRepository _sponsors;
    private readonly IStudentLookup _students;
    private readonly IFinancialEntryRepository _entries;

    public SponsorService(
        ISponsorRepository sponsors,
        IStudentLookup students,
        IFinancialEntryRepository entries)
    {
        _sponsors = sponsors;
        _students = students;
        _entries = entries;
    }

    public async Task<Result<List<SponsorResponse>>> ListAsync(CancellationToken ct)
    {
        var list = await _sponsors.ListAsync(ct);
        return Result<List<SponsorResponse>>.Ok(list.Select(s => SponsorResponse.From(s)).ToList());
    }

    public async Task<Result<SponsorResponse>> GetByIdAsync(long id, CancellationToken ct)
    {
        var sponsor = await _sponsors.GetByIdAsync(id, ct);
        if (sponsor is null)
        {
            return Result<SponsorResponse>.Fail("Padrinho não encontrado", 404);
        }

        return Result<SponsorResponse>.Ok(SponsorResponse.From(sponsor));
    }

    public async Task<Result<List<FinancialEntryResponse>>> ListEntriesAsync(long id, CancellationToken ct)
    {
        var sponsor = await _sponsors.GetByIdAsync(id, ct);
        if (sponsor is null)
        {
            return Result<List<FinancialEntryResponse>>.Fail("Padrinho não encontrado", 404);
        }

        var entries = await _entries.ListBySponsorAsync(id, ct);
        return Result<List<FinancialEntryResponse>>.Ok(entries.Select(FinancialEntryResponse.From).ToList());
    }

    public async Task<Result<SponsorResponse>> CreateAsync(
        SponsorCreateRequest request,
        string? bearer,
        long? userId,
        CancellationToken ct)
    {
        var student = await _students.FindAsync(request.StudentId, bearer, ct);
        if (student is null)
        {
            return Result<SponsorResponse>.Fail("Aluno não encontrado", 404);
        }

        try
        {
            var sponsor = Sponsor.Create(
                request.StudentId,
                request.Name,
                request.Email,
                request.Phone,
                request.Amount,
                request.Frequency,
                request.StartDate,
                userId,
                DateTime.UtcNow);

            await _sponsors.AddAsync(sponsor, ct);
            await _sponsors.SaveChangesAsync(ct);
            return Result<SponsorResponse>.Ok(SponsorResponse.From(sponsor, student.Name), 201);
        }
        catch (ArgumentException ex)
        {
            return Result<SponsorResponse>.Fail(ex.Message, 400);
        }
    }

    public async Task<Result<SponsorResponse>> UpdateAsync(
        long id,
        SponsorUpdateRequest request,
        string? bearer,
        long? userId,
        CancellationToken ct)
    {
        var sponsor = await _sponsors.GetByIdAsync(id, ct);
        if (sponsor is null)
        {
            return Result<SponsorResponse>.Fail("Padrinho não encontrado", 404);
        }

        var student = await _students.FindAsync(request.StudentId, bearer, ct);
        if (student is null)
        {
            return Result<SponsorResponse>.Fail("Aluno não encontrado", 404);
        }

        try
        {
            sponsor.Update(
                request.StudentId,
                request.Name,
                request.Email,
                request.Phone,
                request.Amount,
                request.Frequency,
                request.StartDate,
                request.Active,
                userId,
                DateTime.UtcNow);

            await _sponsors.SaveChangesAsync(ct);
            return Result<SponsorResponse>.Ok(SponsorResponse.From(sponsor, student.Name));
        }
        catch (ArgumentException ex)
        {
            return Result<SponsorResponse>.Fail(ex.Message, 400);
        }
    }

    public async Task<Result> DeleteAsync(long id, long? userId, CancellationToken ct)
    {
        var sponsor = await _sponsors.GetByIdAsync(id, ct);
        if (sponsor is null)
        {
            return Result.Fail("Padrinho não encontrado", 404);
        }

        sponsor.Deactivate(userId, DateTime.UtcNow);
        await _sponsors.SaveChangesAsync(ct);
        return Result.Ok();
    }
}
