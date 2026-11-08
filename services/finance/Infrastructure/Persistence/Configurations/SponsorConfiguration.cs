using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Configurations;

public sealed class SponsorConfiguration : IEntityTypeConfiguration<Sponsor>
{
    public void Configure(EntityTypeBuilder<Sponsor> builder)
    {
        builder.ToTable("sponsor");
        AuditableConfiguration.ConfigureAudit(builder);
        builder.Property(x => x.StudentId).HasColumnName("student_id").IsRequired();
        builder.Property(x => x.Name).HasColumnName("name").HasMaxLength(255).IsRequired();
        builder.Property(x => x.Email).HasColumnName("email").HasMaxLength(255);
        builder.Property(x => x.Phone).HasColumnName("phone").HasMaxLength(50);
        builder.Property(x => x.Amount).HasColumnName("amount").HasPrecision(12, 2).IsRequired();
        builder.Property(x => x.Frequency).HasColumnName("frequency").HasConversion<string>().HasMaxLength(20).IsRequired();
        builder.Property(x => x.StartDate).HasColumnName("start_date").IsRequired();
        builder.Property(x => x.LastGeneratedDate).HasColumnName("last_generated_date");
    }
}
