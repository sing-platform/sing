using Microsoft.EntityFrameworkCore;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Repositories;

public sealed class FiscalDocumentRepository : IFiscalDocumentRepository
{
    private readonly FinanceDbContext _db;

    public FiscalDocumentRepository(FinanceDbContext db) => _db = db;

    public async Task<List<FiscalDocument>> ListAsync(
        FiscalDocumentType? type,
        string? documentNumber,
        CancellationToken ct)
    {
        var query = _db.FiscalDocuments.AsQueryable();
        if (!string.IsNullOrWhiteSpace(documentNumber))
        {
            var term = documentNumber.Trim().ToLowerInvariant();
            query = query.Where(x => x.DocumentNumber.ToLower().Contains(term));
        }
        else if (type is not null)
        {
            query = query.Where(x => x.DocumentType == type);
        }

        return await query.OrderByDescending(x => x.IssueDate).ToListAsync(ct);
    }

    public Task<FiscalDocument?> GetByIdAsync(long id, CancellationToken ct) =>
        _db.FiscalDocuments.FirstOrDefaultAsync(x => x.Id == id, ct);

    public async Task AddAsync(FiscalDocument document, CancellationToken ct) =>
        await _db.FiscalDocuments.AddAsync(document, ct);

    public Task SaveChangesAsync(CancellationToken ct) => _db.SaveChangesAsync(ct);
}
