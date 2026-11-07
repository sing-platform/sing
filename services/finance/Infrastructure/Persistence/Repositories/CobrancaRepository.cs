using Microsoft.EntityFrameworkCore;
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

    public Task SaveChangesAsync(CancellationToken ct) => _db.SaveChangesAsync(ct);
}
