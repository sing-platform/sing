using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Domain;

namespace Sing.Finance.Application.Services;

public sealed class FiscalDocumentService
{
    private readonly IFiscalDocumentRepository _fiscals;

    public FiscalDocumentService(IFiscalDocumentRepository fiscals)
    {
        _fiscals = fiscals;
    }

    public async Task<Result<List<FiscalDocumentResponse>>> ListAsync(
        FiscalDocumentType? type,
        string? documentNumber,
        CancellationToken ct)
    {
        var list = await _fiscals.ListAsync(type, documentNumber, ct);
        return Result<List<FiscalDocumentResponse>>.Ok(list.Select(FiscalDocumentResponse.From).ToList());
    }

    public async Task<Result<FiscalDocumentResponse>> GetByIdAsync(long id, CancellationToken ct)
    {
        var doc = await _fiscals.GetByIdAsync(id, ct);
        if (doc is null)
        {
            return Result<FiscalDocumentResponse>.Fail("Documento fiscal não encontrado", 404);
        }

        return Result<FiscalDocumentResponse>.Ok(FiscalDocumentResponse.From(doc));
    }

    public async Task<Result<FiscalDocumentResponse>> CreateAsync(
        FiscalDocumentCreateRequest request,
        long? userId,
        CancellationToken ct)
    {
        try
        {
            var doc = FiscalDocument.Create(
                request.DocumentType,
                request.DocumentNumber,
                request.IssueDate,
                request.TotalAmount,
                request.SupplierName,
                request.SupplierDoc,
                request.FileUrl,
                userId,
                DateTime.UtcNow);

            await _fiscals.AddAsync(doc, ct);
            await _fiscals.SaveChangesAsync(ct);
            return Result<FiscalDocumentResponse>.Ok(FiscalDocumentResponse.From(doc), 201);
        }
        catch (ArgumentException ex)
        {
            return Result<FiscalDocumentResponse>.Fail(ex.Message, 400);
        }
    }

    public async Task<Result<FiscalDocumentResponse>> UpdateAsync(
        long id,
        FiscalDocumentUpdateRequest request,
        CancellationToken ct)
    {
        var doc = await _fiscals.GetByIdAsync(id, ct);
        if (doc is null)
        {
            return Result<FiscalDocumentResponse>.Fail("Documento fiscal não encontrado", 404);
        }

        try
        {
            doc.Update(
                request.DocumentType,
                request.DocumentNumber,
                request.IssueDate,
                request.TotalAmount,
                request.SupplierName,
                request.SupplierDoc,
                request.FileUrl);

            await _fiscals.SaveChangesAsync(ct);
            return Result<FiscalDocumentResponse>.Ok(FiscalDocumentResponse.From(doc));
        }
        catch (ArgumentException ex)
        {
            return Result<FiscalDocumentResponse>.Fail(ex.Message, 400);
        }
    }

    public async Task<Result> DeleteAsync(long id, long? userId, CancellationToken ct)
    {
        var doc = await _fiscals.GetByIdAsync(id, ct);
        if (doc is null)
        {
            return Result.Fail("Documento fiscal não encontrado", 404);
        }

        doc.Deactivate(userId, DateTime.UtcNow);
        await _fiscals.SaveChangesAsync(ct);
        return Result.Ok();
    }
}
