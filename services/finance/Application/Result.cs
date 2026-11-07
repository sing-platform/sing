namespace Sing.Finance.Application;

public sealed class Result<T>
{
    public bool IsSuccess { get; }
    public T? Value { get; }
    public string? Error { get; }
    public int StatusCode { get; }

    private Result(bool isSuccess, T? value, string? error, int statusCode)
    {
        IsSuccess = isSuccess;
        Value = value;
        Error = error;
        StatusCode = statusCode;
    }

    public static Result<T> Ok(T value, int statusCode = 200) => new(true, value, null, statusCode);

    public static Result<T> Fail(string error, int statusCode) => new(false, default, error, statusCode);
}

public sealed class Result
{
    public bool IsSuccess { get; }
    public string? Error { get; }
    public int StatusCode { get; }

    private Result(bool isSuccess, string? error, int statusCode)
    {
        IsSuccess = isSuccess;
        Error = error;
        StatusCode = statusCode;
    }

    public static Result Ok() => new(true, null, 204);

    public static Result Fail(string error, int statusCode) => new(false, error, statusCode);
}
