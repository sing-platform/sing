using Sing.Finance.Domain;

namespace Sing.Finance.Application.Dtos;

public sealed record FiscalDocumentCreateRequest(
    FiscalDocumentType DocumentType,
    string DocumentNumber,
    DateOnly IssueDate,
    decimal TotalAmount,
    string? SupplierName,
    string? SupplierDoc,
    string? FileUrl);

public sealed record FiscalDocumentUpdateRequest(
    FiscalDocumentType DocumentType,
    string DocumentNumber,
    DateOnly IssueDate,
    decimal TotalAmount,
    string? SupplierName,
    string? SupplierDoc,
    string? FileUrl);

public sealed record FiscalDocumentResponse(
    long Id,
    FiscalDocumentType DocumentType,
    string DocumentNumber,
    DateOnly IssueDate,
    decimal TotalAmount,
    string? SupplierName,
    string? SupplierDoc,
    string? FileUrl,
    bool Active)
{
    public static FiscalDocumentResponse From(FiscalDocument document) => new(
        document.Id,
        document.DocumentType,
        document.DocumentNumber,
        document.IssueDate,
        document.TotalAmount,
        document.SupplierName,
        document.SupplierDoc,
        document.FileUrl,
        document.IsActive);
}
