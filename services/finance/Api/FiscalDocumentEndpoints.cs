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
            .WithTags("Documentos fiscais")
            .WithGroupName("Documentos fiscais")
            .RequireAuthorization("AdminOnly");

        group.MapGet("/", async (
                FiscalDocumentType? type,
                string? documentNumber,
                FiscalDocumentService service,
                CancellationToken ct) =>
                (await service.ListAsync(type, documentNumber, ct)).ToHttp())
            .WithSummary("Listar documentos fiscais")
            .WithDescription(
                """
                Filtros opcionais: `type` (`NFE`, `NFSE`, `RECEIPT`, `INVOICE`, `OTHER`)
                ou `documentNumber` (busca parcial).
                """)
            .Produces<List<FiscalDocumentResponse>>(StatusCodes.Status200OK);

        group.MapGet("/{id:long}", async (long id, FiscalDocumentService service, CancellationToken ct) =>
                (await service.GetByIdAsync(id, ct)).ToHttp())
            .WithSummary("Buscar documento fiscal por ID")
            .Produces<FiscalDocumentResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status404NotFound);

        group.MapPost("/", async (
                FiscalDocumentCreateRequest body,
                FiscalDocumentService service,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                return (await service.CreateAsync(body, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Registrar documento fiscal")
            .WithDescription("`fileUrl` aponta para o arquivo (ex.: MinIO/S3). Valor mínimo 0,01.")
            .Produces<FiscalDocumentResponse>(StatusCodes.Status201Created)
            .Produces(StatusCodes.Status400BadRequest);

        group.MapPut("/{id:long}", async (
                long id,
                FiscalDocumentUpdateRequest body,
                FiscalDocumentService service,
                CancellationToken ct) =>
                (await service.UpdateAsync(id, body, ct)).ToHttp())
            .WithSummary("Atualizar documento fiscal")
            .Produces<FiscalDocumentResponse>(StatusCodes.Status200OK)
            .Produces(StatusCodes.Status400BadRequest)
            .Produces(StatusCodes.Status404NotFound);

        group.MapDelete("/{id:long}", async (
                long id,
                FiscalDocumentService service,
                ClaimsPrincipal principal,
                CancellationToken ct) =>
            {
                var user = CurrentUser.From(principal);
                return (await service.DeleteAsync(id, user.UserId, ct)).ToHttp();
            })
            .WithSummary("Desativar documento fiscal")
            .WithDescription("Soft delete. Não apaga o arquivo no storage.")
            .Produces(StatusCodes.Status204NoContent)
            .Produces(StatusCodes.Status404NotFound);
    }
}
