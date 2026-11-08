using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;
using Sing.Finance.Domain;

namespace Sing.Finance.Infrastructure.Persistence.Configurations;

public sealed class OutboxMessageConfiguration : IEntityTypeConfiguration<OutboxMessage>
{
    public void Configure(EntityTypeBuilder<OutboxMessage> builder)
    {
        builder.ToTable("message_outbox");
        builder.HasKey(x => x.Id);
        builder.Property(x => x.Id).HasColumnName("id");
        builder.Property(x => x.Exchange).HasColumnName("exchange").HasMaxLength(255).IsRequired();
        builder.Property(x => x.RoutingKey).HasColumnName("routing_key").HasMaxLength(255).IsRequired();
        builder.Property(x => x.Payload).HasColumnName("payload").HasColumnType("text").IsRequired();
        builder.Property(x => x.CreatedAt).HasColumnName("created_at").IsRequired();
        builder.Property(x => x.Processed).HasColumnName("processed").IsRequired();
        builder.Property(x => x.ProcessedAt).HasColumnName("processed_at");
    }
}
