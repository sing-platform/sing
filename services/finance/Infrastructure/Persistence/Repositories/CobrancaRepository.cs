using Microsoft.EntityFrameworkCore;
using MySqlConnector;
using Sing.Finance.Application.Contracts;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Repositories;

public sealed class CobrancaRepository : ICobrancaRepository
{
    private readonly FinanceDbContext _db;

    public CobrancaRepository(FinanceDbContext db) => _db = db;

    public Task<Cobranca?> GetByMatriculaIdAsync(long matriculaId, CancellationToken ct) =>
        _db.Cobrancas.FirstOrDefaultAsync(x => x.MatriculaId == matriculaId, ct);

    public async Task AddAsync(Cobranca cobranca, CancellationToken ct) =>
        await _db.Cobrancas.AddAsync(cobranca, ct);

    public async Task SaveChangesAsync(CancellationToken ct)
    {
        try
        {
            await _db.SaveChangesAsync(ct);
        }
        catch (DbUpdateException ex) when (IsUniqueViolation(ex))
        {
            throw new CobrancaConflictException(ex);
        }
    }

    public void DiscardChanges() => _db.ChangeTracker.Clear();

    private static bool IsUniqueViolation(DbUpdateException ex)
    {
        for (var inner = ex.InnerException; inner is not null; inner = inner.InnerException)
        {
            if (inner is MySqlException mysql && mysql.Number == 1062)
            {
                return true;
            }
        }

        return false;
    }
}
