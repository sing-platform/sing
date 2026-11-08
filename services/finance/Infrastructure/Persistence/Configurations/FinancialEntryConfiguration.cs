using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Configurations;

public sealed class FinancialEntryConfiguration : IEntityTypeConfiguration<FinancialEntry>
{
    public void Configure(EntityTypeBuilder<FinancialEntry> builder)
    {
        builder.ToTable("financial_entry");
        AuditableConfiguration.ConfigureAudit(builder);
        builder.Property(x => x.Direction).HasColumnName("direction").HasConversion<string>().HasMaxLength(20).IsRequired();
        builder.Property(x => x.Status).HasColumnName("status").HasConversion<string>().HasMaxLength(20).IsRequired();
        builder.Property(x => x.DueDate).HasColumnName("due_date").IsRequired();
        builder.Property(x => x.PaidAt).HasColumnName("paid_at");
        builder.Property(x => x.Amount).HasColumnName("amount").HasPrecision(12, 2).IsRequired();
        builder.Property(x => x.Category).HasColumnName("category").HasMaxLength(100).IsRequired();
        builder.Property(x => x.Description).HasColumnName("description").HasMaxLength(500);
        builder.Property(x => x.SponsorId).HasColumnName("sponsor_id");
        builder.Property(x => x.FiscalDocumentId).HasColumnName("fiscal_document_id");
        builder.HasOne(x => x.Sponsor).WithMany().HasForeignKey(x => x.SponsorId);
        builder.HasOne(x => x.FiscalDocument).WithMany().HasForeignKey(x => x.FiscalDocumentId);
    }
}
