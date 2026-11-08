using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class FinancialEntryRulesTests
{
    [Fact]
    public void Create_exige_paidAt_quando_PAID()
    {
        var error = Assert.Throws<ArgumentException>(() => FinancialEntry.Create(
            EntryDirection.RECEIVABLE,
            EntryStatus.PAID,
            new DateOnly(2026, 1, 10),
            paidAt: null,
            100m,
            "Mensalidade",
            null,
            null,
            null,
            null,
            DateTime.UtcNow));

        Assert.Contains("pagamento", error.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void MarkOverdue_so_afeta_PENDING()
    {
        var entry = FinancialEntry.Create(
            EntryDirection.RECEIVABLE,
            EntryStatus.PENDING,
            new DateOnly(2026, 1, 1),
            null,
            100m,
            "Padrinho",
            null,
            null,
            null,
            null,
            DateTime.UtcNow);

        entry.MarkOverdue();
        Assert.Equal(EntryStatus.OVERDUE, entry.Status);

        entry.MarkAsPaid(new DateOnly(2026, 1, 5));
        entry.MarkOverdue();
        Assert.Equal(EntryStatus.PAID, entry.Status);
    }
}
