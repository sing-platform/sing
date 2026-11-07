namespace Sing.Finance.Domain;

/// <summary>
/// Movimento com comprovante. A tabela existe no monolito sem API pública;
/// mantemos o schema para a extração, sem endpoints nesta entrega.
/// </summary>
public sealed class FinancialTransaction : AuditableEntity
{
    public TransactionType Type { get; private set; }
    public DateTime DateTime { get; private set; }
    public float TransactionValue { get; private set; }
    public string? Description { get; private set; }
    public string? ReceiptUrl { get; private set; }
    public long UserId { get; private set; }

    private FinancialTransaction()
    {
    }
}
