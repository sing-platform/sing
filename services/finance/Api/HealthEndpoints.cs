using Microsoft.EntityFrameworkCore;
using Sing.Finance.Infrastructure.Persistence;

namespace Sing.Finance.Api;

public static class HealthEndpoints
{
    public static void MapHealthEndpoints(this WebApplication app)
    {
        app.MapGet("/actuator/health", async (FinanceDbContext db) =>
            {
                var checks = new Dictionary<string, string> { ["db"] = "DOWN" };
                try
                {
                    if (await db.Database.CanConnectAsync())
                    {
                        checks["db"] = "UP";
                    }
                }
                catch
                {
                    // health reporta DOWN sem derrubar o processo
                }

                var overall = checks.Values.All(v => v == "UP") ? "UP" : "DOWN";
                return Results.Json(new { status = overall, components = checks });
            })
            .WithTags("Ops")
            .WithGroupName("Ops")
            .WithSummary("Health check")
            .WithDescription(
                """
                Mesmo caminho do Actuator do core/documents.
                Via Traefik, `/actuator/health` cai no **core**. Para o finance use
                o container ou a rede interna.
                """)
            .Produces(StatusCodes.Status200OK)
            .AllowAnonymous();
    }
}
