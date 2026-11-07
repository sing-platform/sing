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
                await db.Database.ExecuteSqlRawAsync("SELECT 1");
                checks["db"] = "UP";
            }
            catch
            {
                // health reporta DOWN sem derrubar o processo
            }

            var overall = checks.Values.All(v => v == "UP") ? "UP" : "DOWN";
            return Results.Json(new { status = overall, components = checks });
        }).AllowAnonymous();
    }
}
