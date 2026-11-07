using Sing.Finance.Domain;

namespace Sing.Finance.Application.Contracts;

public interface ISponsorRepository
{
    Task<List<Sponsor>> ListAsync(CancellationToken ct);
    Task<Sponsor?> GetByIdAsync(long id, CancellationToken ct);
    Task<List<Sponsor>> ListActiveAsync(CancellationToken ct);
    Task AddAsync(Sponsor sponsor, CancellationToken ct);
    Task SaveChangesAsync(CancellationToken ct);
}
