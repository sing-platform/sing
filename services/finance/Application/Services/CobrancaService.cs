using Sing.Finance.Application.Contracts;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Domain;

namespace Sing.Finance.Application.Services;

public sealed class CobrancaService
{
    private readonly ICobrancaRepository _cobrancas;

    public CobrancaService(ICobrancaRepository cobrancas)
    {
        _cobrancas = cobrancas;
    }

    public async Task<Result<CobrancaResponse>> CreateAsync(CobrancaCreateRequest request, CancellationToken ct)
    {
        try
        {
            var existing = await _cobrancas.GetByMatriculaIdAsync(request.MatriculaId, ct);
            if (existing is not null)
            {
                return ResolveExisting(existing, request);
            }

            var cobranca = Cobranca.Create(
                request.MatriculaId,
                request.ValorCentavos,
                request.Vencimento,
                DateTime.UtcNow);

            await _cobrancas.AddAsync(cobranca, ct);
            try
            {
                await _cobrancas.SaveChangesAsync(ct);
            }
            catch (CobrancaConflictException)
            {
                _cobrancas.DiscardChanges();
                var raced = await _cobrancas.GetByMatriculaIdAsync(request.MatriculaId, ct);
                if (raced is null)
                {
                    throw;
                }

                return ResolveExisting(raced, request);
            }

            return Result<CobrancaResponse>.Ok(new CobrancaResponse(cobranca.Id, cobranca.Status), 201);
        }
        catch (ArgumentException ex)
        {
            return Result<CobrancaResponse>.Fail(ex.Message, 400);
        }
    }

    private static Result<CobrancaResponse> ResolveExisting(Cobranca existing, CobrancaCreateRequest request)
    {
        if (existing.Matches(request.ValorCentavos, request.Vencimento))
        {
            return Result<CobrancaResponse>.Ok(new CobrancaResponse(existing.Id, existing.Status), 200);
        }

        return Result<CobrancaResponse>.Fail("Cobrança divergente para a matrícula", 409);
    }
}
