using System.Security.Claims;
using Sing.Finance.Api.Auth;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;
using Sing.Finance.Domain;

namespace Sing.Finance.Api;

public static class FinancialEntryEndpoints
{
    public static void MapFinancialEntryEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/finance/entries")
            .WithTags("Lançamentos")
            .WithGroupName("Lançamentos")
            .RequireAuthorization("AdminOnly");

        group.MapGet("/", async (
                string? month,
                EntryDirection? direction,
                EntryStatus? status,
                string? category,
                FinancialEntryService service,
                CancellationToken ct) =>
                (await service.ListAsync(month, direction, status, category, ct)).ToHttp())
            .WithSummary("Listar lançamentos")
            .WithDescription(
                """
                Filtros opcionais:
                - `month` — competência `yyyy-MM`
                - `direction` — `RECEIVABLE` | `PAYABLE`
                - `status` — `PENDING` | `PAID` | `OVERDUE` | `CANCELED`
                - `category` — busca parcial (case-insensitive)
                """)
            .Produces<List<FinancialEntryResponse>>(StatusCodes.Status200OK);

        group.MapGet("/summary", async (string month, FinancialEntryService service, CancellationToken ct) =>
                (await service.MonthSummaryAsync(month, ct)).ToHttp())
            .WithSummary("Resumo do mês")
            .WithDescription(
                """
                Totais do mês (`yyyy-MM`): receitas, despesas, saldo e pendências.
                Só considera lançamentos ativos.
                """)
            .Produces<FinancialSummaryResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status400BadRequest);

        group.MapGet("/{id:long}", async (long id, FinancialEntryService service, CancellationToken ct) =>
                (await service.GetByIdAsync(id, ct)).ToHttp())
            .WithSummary("Buscar lançamento por ID")
            .Produces<FinancialEntryResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status404NotFound);

        group.MapPost("/", async (
                FinancialEntryCreateRequest body,
                FinancialEntryService service,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                return (await service.CreateAsync(body, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Criar lançamento")
            .WithDescription("Status `PAID` exige `paidAt`. Valor mínimo 0,01.")
            .Produces<FinancialEntryResponse>(StatusCodes.Status201Created)
            .Produces(StatusCodes.Status400BadRequest)
            .Produces(StatusCodes.Status404NotFound);

        group.MapPut("/{id:long}", async (
                long id,
                FinancialEntryUpdateRequest body,
                FinancialEntryService service,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                return (await service.UpdateAsync(id, body, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Atualizar lançamento")
            .Produces<FinancialEntryResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status400BadRequest)
            .Produces(StatusCodes.Status404NotFound);

        group.MapPatch("/{id:long}/pay", async (
                long id,
                MarkEntryAsPaidRequest body,
                FinancialEntryService service,
                CancellationToken ct) =>
                (await service.MarkAsPaidAsync(id, body.PaidAt, ct)).ToHttp())
            .WithSummary("Marcar como pago")
            .WithDescription("Baixa financeira local: status `PAID` e data de quitação. Não é distribuída.")
            .Produces<FinancialEntryResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status404NotFound);

        group.MapDelete("/{id:long}", async (
                long id,
                FinancialEntryService service,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                return (await service.DeleteAsync(id, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Desativar lançamento")
            .WithDescription("Soft delete (`is_active = false`).")
            .Produces(StatusCodes.Status204NoContent)
            .Produces(StatusCodes.Status404NotFound);
    }
}
