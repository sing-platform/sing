namespace Sing.Finance.Application.Configuration;

public sealed class RabbitMqOptions
{
    public const string SectionName = "RabbitMq";

    public string Host { get; set; } = "localhost";
    public int Port { get; set; } = 5672;
    public string User { get; set; } = "sing";
    public string Password { get; set; } = "sing";
    public string Exchange { get; set; } = "sing.events";
}
