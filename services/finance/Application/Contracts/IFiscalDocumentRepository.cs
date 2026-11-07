using Sing.Finance.Domain;

namespace Sing.Finance.Application.Contracts;

public interface IFiscalDocumentRepository
{
    Task<List<FiscalDocument>> ListAsync(FiscalDocumentType? type, string? documentNumber, CancellationToken ct);
    Task<FiscalDocument?> GetByIdAsync(long id, CancellationToken ct);
    Task AddAsync(FiscalDocument document, CancellationToken ct);
    Task SaveChangesAsync(CancellationToken ct);
}
