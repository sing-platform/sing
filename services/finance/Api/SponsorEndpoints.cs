using System.Security.Claims;
using Microsoft.AspNetCore.Mvc;
using Sing.Finance.Api.Auth;
using Sing.Finance.Application.Dtos;
using Sing.Finance.Application.Services;

namespace Sing.Finance.Api;

public static class SponsorEndpoints
{
    public static void MapSponsorEndpoints(this WebApplication app)
    {
        var group = app.MapGroup("/api/v1/finance/sponsors")
            .RequireAuthorization("AdminOnly");

        group.MapGet("/", async (SponsorService service, CancellationToken ct) =>
            (await service.ListAsync(ct)).ToHttp());

        group.MapGet("/{id:long}", async (long id, SponsorService service, CancellationToken ct) =>
            (await service.GetByIdAsync(id, ct)).ToHttp());

        group.MapGet("/{id:long}/entries", async (long id, SponsorService service, CancellationToken ct) =>
            (await service.ListEntriesAsync(id, ct)).ToHttp());

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
        });

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
        });

        group.MapDelete("/{id:long}", async (
            long id,
            SponsorService service,
            ClaimsPrincipal principal,
            CancellationToken ct) =>
        {
            var user = CurrentUser.From(principal);
            return (await service.DeleteAsync(id, user.UserId, ct)).ToHttp();
        });
    }
}
