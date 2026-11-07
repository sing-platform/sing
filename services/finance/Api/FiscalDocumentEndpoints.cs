using System.Security.Claims;
using Sing.Finance.Api.Auth;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;
using Sing.Finance.Domain;

namespace Sing.Finance.Api;

public static class FiscalDocumentEndpoints
{
    public static void MapFiscalDocumentEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/finance/fiscal-documents")
            .RequireAuthorization("AdminOnly");

        group.MapGet("/", async (
            FiscalDocumentType? type,
            string? documentNumber,
            FiscalDocumentService service,
            CancellationToken ct) =>
            (await service.ListAsync(type, documentNumber, ct)).ToHttp());

        group.MapGet("/{id:long}", async (long id, FiscalDocumentService service, CancellationToken ct) =>
            (await service.GetByIdAsync(id, ct)).ToHttp());

        group.MapPost("/", async (
            FiscalDocumentCreateRequest body,
            FiscalDocumentService service,
            ClaimsPrincipal principal,
            CancellationToken ct) =>
        {
            var user = CurrentUser.From(principal);
            return (await service.CreateAsync(body, user.UserId, ct)).ToHttp();
        });

        group.MapPut("/{id:long}", async (
            long id,
            FiscalDocumentUpdateRequest body,
            FiscalDocumentService service,
            CancellationToken ct) =>
            (await service.UpdateAsync(id, body, ct)).ToHttp());

        group.MapDelete("/{id:long}", async (
            long id,
            FiscalDocumentService service,
            ClaimsPrincipal principal,
            CancellationToken ct) =>
        {
            var user = CurrentUser.From(principal);
            return (await service.DeleteAsync(id, user.UserId, ct)).ToHttp();
        });
    }
}
