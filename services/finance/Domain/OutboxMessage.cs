namespace Sing.Finance.Domain;

public sealed class OutboxMessage
{
    public long Id { get; set; }
    public string Exchange { get; set; } = "";
    public string RoutingKey { get; set; } = "";
    public string Payload { get; set; } = "";
    public DateTime CreatedAt { get; set; }
    public bool Processed { get; set; }
    public DateTime? ProcessedAt { get; set; }
}
