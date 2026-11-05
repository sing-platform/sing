namespace Sing.Finance.Api;

public static class HealthEndpoints
{
    public static void MapHealthEndpoints(this WebApplication app)
    {
        app.MapGet("/actuator/health", () => Results.Ok(new { status = "UP" }));
    }
}
