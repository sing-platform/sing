using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;

namespace Sing.Finance.Api;

public static class CobrancaEndpoints
{
    public static void MapCobrancaEndpoints(this WebApplication app)
    {
        // Contrato N2: o core chama sem JWT na rede interna.
        app.MapPost("/cobrancas", async (
            CobrancaCreateRequest body,
            CobrancaService service,
            CancellationToken ct) =>
            (await service.CreateAsync(body, ct)).ToHttp())
            .AllowAnonymous();
    }
}
