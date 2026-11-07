using Sing.Finance.Domain;

namespace Sing.Finance.Application.Contracts;

public interface ICobrancaRepository
{
    Task<Cobranca?> GetByMatriculaIdAsync(long matriculaId, CancellationToken ct);
    Task AddAsync(Cobranca cobranca, CancellationToken ct);
    Task SaveChangesAsync(CancellationToken ct);
}
