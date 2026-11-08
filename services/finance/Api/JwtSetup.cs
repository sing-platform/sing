using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;

namespace Sing.Finance.Api;

public static class JwtSetup
{
    public static IServiceCollection AddFinanceJwt(this IServiceCollection services, IConfiguration configuration)
    {
        var secret = configuration["Jwt:Secret"]
            ?? Environment.GetEnvironmentVariable("JWT_SECRET")
            ?? throw new InvalidOperationException("JWT_SECRET não configurado.");

        var key = ResolveSigningKey(secret);

        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.TokenValidationParameters = new TokenValidationParameters
                {
                    ValidateIssuer = false,
                    ValidateAudience = false,
                    ValidateIssuerSigningKey = true,
                    IssuerSigningKey = new SymmetricSecurityKey(key),
                    RoleClaimType = "role",
                    NameClaimType = "sub"
                };
            });

        services.AddAuthorization(options =>
        {
            options.AddPolicy("AdminOnly", policy =>
                policy.RequireAssertion(ctx =>
                {
                    var role = ctx.User.FindFirst("role")?.Value
                        ?? ctx.User.FindFirst(System.Security.Claims.ClaimTypes.Role)?.Value;
                    return role is "ROLE_ADMIN" or "ADMIN" or "ROLE_MANAGER" or "MANAGER";
                }));
        });

        return services;
    }

    private static byte[] ResolveSigningKey(string secret)
    {
        // Mesmo critério do core / documents: tenta Base64, senão bytes literais.
        try
        {
            return Convert.FromBase64String(secret);
        }
        catch (FormatException)
        {
            return Encoding.UTF8.GetBytes(secret);
        }
    }
}
