using System.Security.Claims;

namespace Sing.Finance.Api.Auth;

public sealed class CurrentUser
{
    public string? Email { get; init; }
    public long? UserId { get; init; }
    public string? Role { get; init; }

    public static CurrentUser From(ClaimsPrincipal principal)
    {
        var uid = principal.FindFirst("uid")?.Value
            ?? principal.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return new CurrentUser
        {
            Email = principal.FindFirst("sub")?.Value ?? principal.FindFirst(ClaimTypes.Email)?.Value,
            UserId = long.TryParse(uid, out var id) ? id : null,
            Role = principal.FindFirst("role")?.Value ?? principal.FindFirst(ClaimTypes.Role)?.Value
        };
    }
}
