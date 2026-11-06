namespace Sing.Finance.Domain;

public sealed class Sponsor : AuditableEntity
{
    public long StudentId { get; private set; }
    public string Name { get; private set; } = "";
    public string? Email { get; private set; }
    public string? Phone { get; private set; }
    public decimal Amount { get; private set; }
    public SponsorFrequency Frequency { get; private set; }
    public DateOnly StartDate { get; private set; }
    public DateOnly? LastGeneratedDate { get; private set; }

    private Sponsor()
    {
    }

    public static Sponsor Create(
        long studentId,
        string name,
        string? email,
        string? phone,
        decimal amount,
        SponsorFrequency frequency,
        DateOnly startDate,
        long? createdBy,
        DateTime createdAt)
    {
        if (studentId <= 0)
        {
            throw new ArgumentException("Aluno é obrigatório.");
        }

        if (string.IsNullOrWhiteSpace(name))
        {
            throw new ArgumentException("Nome é obrigatório.");
        }

        if (amount < 0.01m)
        {
            throw new ArgumentException("Valor mínimo é 0,01.");
        }

        return new Sponsor
        {
            StudentId = studentId,
            Name = name.Trim(),
            Email = email,
            Phone = phone,
            Amount = amount,
            Frequency = frequency,
            StartDate = startDate,
            CreatedBy = createdBy,
            CreatedAt = createdAt
        };
    }
}
