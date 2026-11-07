using System.Security.Claims;
using Sing.Finance.Api.Auth;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;

namespace Sing.Finance.Api;

public static class SponsorEndpoints
{
    public static void MapSponsorEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/finance/sponsors")
            .WithTags("Padrinhos")
            .WithGroupName("Padrinhos")
            .RequireAuthorization("AdminOnly");

        group.MapGet("/", async (SponsorService service, CancellationToken ct) =>
                (await service.ListAsync(ct)).ToHttp())
            .WithSummary("Listar padrinhos")
            .WithDescription("Retorna todos os padrinhos ordenados por nome.")
            .Produces<List<SponsorResponse>>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status401Unauthorized)
            .Produces(StatusCodes.Status403Forbidden);

        group.MapGet("/{id:long}", async (long id, SponsorService service, CancellationToken ct) =>
                (await service.GetByIdAsync(id, ct)).ToHttp())
            .WithSummary("Buscar padrinho por ID")
            .Produces<SponsorResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status404NotFound);

        group.MapGet("/{id:long}/entries", async (long id, SponsorService service, CancellationToken ct) =>
                (await service.ListEntriesAsync(id, ct)).ToHttp())
            .WithSummary("Listar lançamentos do padrinho")
            .WithDescription("Recebíveis gerados (manual ou pelo job diário) vinculados ao padrinho.")
            .Produces<List<FinancialEntryResponse>>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status404NotFound);

        group.MapPost("/", async (
                SponsorCreateRequest body,
                SponsorService service,
                HttpRequest request,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                var bearer = request.Headers.Authorization.ToString();
                return (await service.CreateAsync(body, bearer, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Cadastrar padrinho")
            .WithDescription(
                """
                Confirma o aluno no **core** (`GET /api/v1/students/{id}`) com o mesmo Bearer.
                Se o core estiver indisponível ou o aluno não existir, responde **404**.
                """)
            .Produces<SponsorResponse>(StatusCodes.Status201Created)
            .Produces(StatusCodes.Status400BadRequest)
            .Produces(StatusCodes.Status404NotFound);

        group.MapPut("/{id:long}", async (
                long id,
                SponsorUpdateRequest body,
                SponsorService service,
                HttpRequest request,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                var bearer = request.Headers.Authorization.ToString();
                return (await service.UpdateAsync(id, body, bearer, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Atualizar padrinho")
            .WithDescription("Também revalida o aluno no core. `active: false` faz soft delete.")
            .Produces<SponsorResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status400BadRequest)
            .Produces(StatusCodes.Status404NotFound);

        group.MapDelete("/{id:long}", async (
                long id,
                SponsorService service,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                return (await service.DeleteAsync(id, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Desativar padrinho")
            .WithDescription("Soft delete (`is_active = false`). Não remove lançamentos já gerados.")
            .Produces(StatusCodes.Status204NoContent)
            .Produces(StatusCodes.Status404NotFound);
    }
}
