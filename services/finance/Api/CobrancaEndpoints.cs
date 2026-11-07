using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;

namespace Sing.Finance.Api;

public static class CobrancaEndpoints
{
    public static void MapCobrancaEndpoints(this WebApplication app)
    {
        app.MapPost("/cobrancas", async (
                CobrancaCreateRequest body,
                CobrancaService service,
                CancellationToken ct) =>
                (await service.CreateAsync(body, ct)).ToHttp())
            .WithTags("Cobranças (N2)")
            .WithGroupName("Cobranças (N2)")
            .WithSummary("Criar cobrança da matrícula")
            .WithDescription(
                """
                Contrato N2. O core chama após gravar a matrícula.

                - **201** na primeira vez para aquele `matriculaId`
                - **200** se repetir com o mesmo valor e vencimento (idempotente)
                - **409** se o mesmo `matriculaId` vier com valor ou vencimento diferente
                - Sem JWT (rede interna)
                """)
            .Produces<CobrancaResponse>(StatusCodes.Status201Created)
            .Produces<CobrancaResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status400BadRequest)
            .Produces(StatusCodes.Status409Conflict)
            .AllowAnonymous();
    }
}
