using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class SponsorRulesTests
{
    private static readonly DateTime CreatedAt = new(2026, 1, 15, 10, 0, 0, DateTimeKind.Utc);

    [Theory]
    [InlineData(SponsorFrequency.Monthly, 2026, 2, 15)]
    [InlineData(SponsorFrequency.Bimonthly, 2026, 3, 15)]
    [InlineData(SponsorFrequency.Quarterly, 2026, 4, 15)]
    [InlineData(SponsorFrequency.Semiannual, 2026, 7, 15)]
    [InlineData(SponsorFrequency.Annual, 2027, 1, 15)]
    public void NextDueDate_soma_os_meses_da_frequencia(
        SponsorFrequency frequency, int year, int month, int day)
    {
        var next = frequency.NextDueDate(new DateOnly(2026, 1, 15));

        Assert.Equal(new DateOnly(year, month, day), next);
    }

    [Fact]
    public void Create_aceita_padrinho_valido_e_corta_espaco_do_nome()
    {
        var sponsor = Sponsor.Create(
            studentId: 10,
            name: "  Maria  ",
            email: "maria@sing.local",
            phone: null,
            amount: 50.00m,
            frequency: SponsorFrequency.Monthly,
            startDate: new DateOnly(2026, 1, 1),
            createdBy: 3,
            createdAt: CreatedAt);

        Assert.Equal("Maria", sponsor.Name);
        Assert.Equal(50.00m, sponsor.Amount);
        Assert.True(sponsor.IsActive);
        Assert.Equal(3, sponsor.CreatedBy);
    }

    [Theory]
    [InlineData(0, "Maria", 50)]
    [InlineData(10, "  ", 50)]
    [InlineData(10, "Maria", 0)]
    public void Create_recusa_aluno_nome_ou_valor_invalidos(long studentId, string name, int amount)
    {
        var error = Assert.Throws<ArgumentException>(() => Sponsor.Create(
            studentId,
            name,
            email: null,
            phone: null,
            amount,
            SponsorFrequency.Monthly,
            new DateOnly(2026, 1, 1),
            createdBy: null,
            CreatedAt));

        Assert.False(string.IsNullOrWhiteSpace(error.Message));
    }

    [Fact]
    public void Deactivate_marca_inativo_com_quem_e_quando()
    {
        var sponsor = Sponsor.Create(
            10, "Maria", null, null, 50m, SponsorFrequency.Monthly,
            new DateOnly(2026, 1, 1), createdBy: 3, CreatedAt);
        var at = CreatedAt.AddDays(2);

        sponsor.Deactivate(userId: 7, at);

        Assert.False(sponsor.IsActive);
        Assert.Equal(7, sponsor.DeactivatedBy);
        Assert.Equal(at, sponsor.DeactivatedAt);
    }
}
