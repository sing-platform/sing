using Sing.Finance.Application;

namespace Sing.Finance.Api;

public static class HttpResultExtensions
{
    public static IResult ToHttp<T>(this Result<T> result)
    {
        if (result.IsSuccess)
        {
            return Results.Json(result.Value, statusCode: result.StatusCode);
        }

        return Results.Json(new { message = result.Error }, statusCode: result.StatusCode);
    }

    public static IResult ToHttp(this Result result)
    {
        if (result.IsSuccess)
        {
            return Results.StatusCode(result.StatusCode);
        }

        return Results.Json(new { message = result.Error }, statusCode: result.StatusCode);
    }
}
