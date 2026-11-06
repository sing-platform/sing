namespace Sing.Finance.Domain;

public abstract class AuditableEntity
{
    public long Id { get; set; }
    public DateTime CreatedAt { get; set; }
    public long? CreatedBy { get; set; }
    public bool IsActive { get; set; } = true;
    public long? DeactivatedBy { get; set; }
    public DateTime? DeactivatedAt { get; set; }

    public void Deactivate(long? userId, DateTime at)
    {
        IsActive = false;
        DeactivatedBy = userId;
        DeactivatedAt = at;
    }
}
