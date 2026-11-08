using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Configurations;

public sealed class CobrancaConfiguration : IEntityTypeConfiguration<Cobranca>
{
    public void Configure(EntityTypeBuilder<Cobranca> builder)
    {
        builder.ToTable("cobranca");
        AuditableConfiguration.ConfigureAudit(builder);
        builder.Property(x => x.MatriculaId).HasColumnName("matricula_id").IsRequired();
        builder.Property(x => x.ValorCentavos).HasColumnName("valor_centavos").IsRequired();
        builder.Property(x => x.Vencimento).HasColumnName("vencimento").IsRequired();
        builder.Property(x => x.Status).HasColumnName("status").HasMaxLength(20).IsRequired();
        builder.HasIndex(x => x.MatriculaId).IsUnique();
    }
}
