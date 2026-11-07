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
            .RequireAuthorization("AdminOnly");

        group.MapGet("/", async (
            string? month,
            EntryDirection? direction,
            EntryStatus? status,
            string? category,
            FinancialEntryService service,
            CancellationToken ct) =>
            (await service.ListAsync(month, direction, status, category, ct)).ToHttp());

        // summary antes de {id} para não capturar "summary" como id
        group.MapGet("/summary", async (string month, FinancialEntryService service, CancellationToken ct) =>
            (await service.MonthSummaryAsync(month, ct)).ToHttp());

        group.MapGet("/{id:long}", async (long id, FinancialEntryService service, CancellationToken ct) =>
            (await service.GetByIdAsync(id, ct)).ToHttp());

        group.MapPost("/", async (
            FinancialEntryCreateRequest body,
            FinancialEntryService service,
            ClaimsPrincipal principal,
            CancellationToken ct) =>
        {
            var user = CurrentUser.From(principal);
            return (await service.CreateAsync(body, user.UserId, ct)).ToHttp();
        });

        group.MapPut("/{id:long}", async (
            long id,
            FinancialEntryUpdateRequest body,
            FinancialEntryService service,
            ClaimsPrincipal principal,
            CancellationToken ct) =>
        {
            var user = CurrentUser.From(principal);
            return (await service.UpdateAsync(id, body, user.UserId, ct)).ToHttp();
        });

        group.MapPatch("/{id:long}/pay", async (
            long id,
            MarkEntryAsPaidRequest body,
            FinancialEntryService service,
            CancellationToken ct) =>
            (await service.MarkAsPaidAsync(id, body.PaidAt, ct)).ToHttp());

        group.MapDelete("/{id:long}", async (
            long id,
            FinancialEntryService service,
            ClaimsPrincipal principal,
            CancellationToken ct) =>
        {
            var user = CurrentUser.From(principal);
            return (await service.DeleteAsync(id, user.UserId, ct)).ToHttp();
        });
    }
}
