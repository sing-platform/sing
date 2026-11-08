namespace Sing.Finance.Domain;

public sealed class FinancialEntry : AuditableEntity
{
    public EntryDirection Direction { get; private set; }
    public EntryStatus Status { get; private set; }
    public DateOnly DueDate { get; private set; }
    public DateOnly? PaidAt { get; private set; }
    public decimal Amount { get; private set; }
    public string Category { get; private set; } = "";
    public string? Description { get; private set; }
    public long? SponsorId { get; private set; }
    public Sponsor? Sponsor { get; private set; }
    public long? FiscalDocumentId { get; private set; }
    public FiscalDocument? FiscalDocument { get; private set; }

    private FinancialEntry()
    {
    }

    public static FinancialEntry Create(
        EntryDirection direction,
        EntryStatus status,
        DateOnly dueDate,
        DateOnly? paidAt,
        decimal amount,
        string category,
        string? description,
        Sponsor? sponsor,
        FiscalDocument? fiscalDocument,
        long? createdBy,
        DateTime createdAt)
    {
        if (amount < 0.01m)
        {
            throw new ArgumentException("Valor mínimo é 0,01.");
        }

        if (string.IsNullOrWhiteSpace(category))
        {
            throw new ArgumentException("Categoria é obrigatória.");
        }

        ValidatePaid(status, paidAt);

        return new FinancialEntry
        {
            Direction = direction,
            Status = status,
            DueDate = dueDate,
            PaidAt = paidAt,
            Amount = amount,
            Category = category.Trim(),
            Description = description,
            Sponsor = sponsor,
            SponsorId = sponsor?.Id,
            FiscalDocument = fiscalDocument,
            FiscalDocumentId = fiscalDocument?.Id,
            CreatedBy = createdBy,
            CreatedAt = createdAt
        };
    }

    public static FinancialEntry CreateSponsorBilling(Sponsor sponsor, DateOnly dueDate, DateTime createdAt)
    {
        return Create(
            EntryDirection.RECEIVABLE,
            EntryStatus.PENDING,
            dueDate,
            paidAt: null,
            sponsor.Amount,
            "Padrinho",
            "Cobrança automática de padrinho: " + sponsor.Name,
            sponsor,
            fiscalDocument: null,
            createdBy: null,
            createdAt);
    }

    public void Update(
        EntryDirection direction,
        EntryStatus status,
        DateOnly dueDate,
        DateOnly? paidAt,
        decimal amount,
        string category,
        string? description,
        Sponsor? sponsor,
        FiscalDocument? fiscalDocument,
        bool active,
        long? userId,
        DateTime at)
    {
        if (amount < 0.01m)
        {
            throw new ArgumentException("Valor mínimo é 0,01.");
        }

        if (string.IsNullOrWhiteSpace(category))
        {
            throw new ArgumentException("Categoria é obrigatória.");
        }

        ValidatePaid(status, paidAt);

        Direction = direction;
        Status = status;
        DueDate = dueDate;
        PaidAt = paidAt;
        Amount = amount;
        Category = category.Trim();
        Description = description;
        Sponsor = sponsor;
        SponsorId = sponsor?.Id;
        FiscalDocument = fiscalDocument;
        FiscalDocumentId = fiscalDocument?.Id;

        if (active)
        {
            IsActive = true;
            DeactivatedBy = null;
            DeactivatedAt = null;
        }
        else
        {
            Deactivate(userId, at);
        }
    }

    public void MarkAsPaid(DateOnly paidAt)
    {
        Status = EntryStatus.PAID;
        PaidAt = paidAt;
    }

    public void MarkOverdue()
    {
        if (Status == EntryStatus.PENDING)
        {
            Status = EntryStatus.OVERDUE;
        }
    }

    private static void ValidatePaid(EntryStatus status, DateOnly? paidAt)
    {
        if (status == EntryStatus.PAID && paidAt is null)
        {
            throw new ArgumentException("Data de pagamento é obrigatória para status PAID.");
        }
    }
}
