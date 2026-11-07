namespace Sing.Finance.Domain;

public sealed class FiscalDocument : AuditableEntity
{
    public FiscalDocumentType DocumentType { get; private set; }
    public string DocumentNumber { get; private set; } = "";
    public DateOnly IssueDate { get; private set; }
    public decimal TotalAmount { get; private set; }
    public string? SupplierName { get; private set; }
    public string? SupplierDoc { get; private set; }
    public string? FileUrl { get; private set; }

    private FiscalDocument()
    {
    }

    public static FiscalDocument Create(
        FiscalDocumentType documentType,
        string documentNumber,
        DateOnly issueDate,
        decimal totalAmount,
        string? supplierName,
        string? supplierDoc,
        string? fileUrl,
        long? createdBy,
        DateTime createdAt)
    {
        if (string.IsNullOrWhiteSpace(documentNumber))
        {
            throw new ArgumentException("Número do documento é obrigatório.");
        }

        if (totalAmount < 0.01m)
        {
            throw new ArgumentException("Valor mínimo é 0,01.");
        }

        return new FiscalDocument
        {
            DocumentType = documentType,
            DocumentNumber = documentNumber.Trim(),
            IssueDate = issueDate,
            TotalAmount = totalAmount,
            SupplierName = supplierName,
            SupplierDoc = supplierDoc,
            FileUrl = fileUrl,
            CreatedBy = createdBy,
            CreatedAt = createdAt
        };
    }

    public void Update(
        FiscalDocumentType documentType,
        string documentNumber,
        DateOnly issueDate,
        decimal totalAmount,
        string? supplierName,
        string? supplierDoc,
        string? fileUrl)
    {
        if (string.IsNullOrWhiteSpace(documentNumber))
        {
            throw new ArgumentException("Número do documento é obrigatório.");
        }

        if (totalAmount < 0.01m)
        {
            throw new ArgumentException("Valor mínimo é 0,01.");
        }

        DocumentType = documentType;
        DocumentNumber = documentNumber.Trim();
        IssueDate = issueDate;
        TotalAmount = totalAmount;
        SupplierName = supplierName;
        SupplierDoc = supplierDoc;
        FileUrl = fileUrl;
    }
}
