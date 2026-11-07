using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;

namespace Sing.Finance.Tests.Support;

internal static class TestJwt
{
    public static string CreateAdmin(string secret = FinanceApiFactory.JwtSecret)
    {
        var key = Convert.FromBase64String(secret);
        var credentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256);
        var token = new JwtSecurityToken(
            claims:
            [
                new Claim("sub", "admin@sing.local"),
                new Claim("uid", "1"),
                new Claim("role", "ROLE_ADMIN")
            ],
            expires: DateTime.UtcNow.AddHours(1),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }
}
