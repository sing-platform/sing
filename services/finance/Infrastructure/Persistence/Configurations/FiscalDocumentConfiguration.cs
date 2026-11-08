using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Configurations;

public sealed class FiscalDocumentConfiguration : IEntityTypeConfiguration<FiscalDocument>
{
    public void Configure(EntityTypeBuilder<FiscalDocument> builder)
    {
        builder.ToTable("fiscal_document");
        AuditableConfiguration.ConfigureAudit(builder);
        builder.Property(x => x.DocumentType).HasColumnName("document_type").HasConversion<string>().HasMaxLength(30).IsRequired();
        builder.Property(x => x.DocumentNumber).HasColumnName("document_number").HasMaxLength(100).IsRequired();
        builder.Property(x => x.IssueDate).HasColumnName("issue_date").IsRequired();
        builder.Property(x => x.TotalAmount).HasColumnName("total_amount").HasPrecision(12, 2).IsRequired();
        builder.Property(x => x.SupplierName).HasColumnName("supplier_name").HasMaxLength(255);
        builder.Property(x => x.SupplierDoc).HasColumnName("supplier_doc").HasMaxLength(30);
        builder.Property(x => x.FileUrl).HasColumnName("file_url").HasMaxLength(2048);
    }
}
