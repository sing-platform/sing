using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Configurations;

public sealed class FinancialTransactionConfiguration : IEntityTypeConfiguration<FinancialTransaction>
{
    public void Configure(EntityTypeBuilder<FinancialTransaction> builder)
    {
        builder.ToTable("financial_transaction");
        AuditableConfiguration.ConfigureAudit(builder);
        builder.Property(x => x.Type).HasColumnName("type").HasConversion<string>().HasMaxLength(30).IsRequired();
        builder.Property(x => x.DateTime).HasColumnName("datetime").IsRequired();
        builder.Property(x => x.TransactionValue).HasColumnName("value_transaction").IsRequired();
        builder.Property(x => x.Description).HasColumnName("description").HasMaxLength(500);
        builder.Property(x => x.ReceiptUrl).HasColumnName("receipt_url").HasMaxLength(2048);
        builder.Property(x => x.UserId).HasColumnName("user_id").IsRequired();
    }
}
