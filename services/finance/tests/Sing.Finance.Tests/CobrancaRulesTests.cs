using Sing.Finance.Domain;
using Xunit;

namespace Sing.Finance.Tests;

public class CobrancaRulesTests
{
    [Fact]
    public void Create_grava_status_pendente()
    {
        var cobranca = Cobranca.Create(9001, 15000, new DateOnly(2026, 11, 15), DateTime.UtcNow);

        Assert.Equal(9001, cobranca.MatriculaId);
        Assert.Equal(15000, cobranca.ValorCentavos);
        Assert.Equal("pendente", cobranca.Status);
        Assert.True(cobranca.Matches(15000, new DateOnly(2026, 11, 15)));
        Assert.False(cobranca.Matches(15001, new DateOnly(2026, 11, 15)));
    }

    [Theory]
    [InlineData(0, 15000)]
    [InlineData(1, 0)]
    [InlineData(1, -1)]
    public void Create_recusa_matricula_ou_valor_invalidos(long matriculaId, int valor)
    {
        Assert.Throws<ArgumentException>(() =>
            Cobranca.Create(matriculaId, valor, new DateOnly(2026, 11, 15), DateTime.UtcNow));
    }
}
