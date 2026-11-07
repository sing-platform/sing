namespace Sing.Finance.Application.Configuration;

public sealed class CoreOptions
{
    public const string SectionName = "Core";

    public string BaseUrl { get; set; } = "http://localhost:8080";
    public int TimeoutSeconds { get; set; } = 5;
}
