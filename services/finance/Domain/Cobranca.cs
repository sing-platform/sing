namespace Sing.Finance.Domain;

/// <summary>
/// Cobrança da matrícula (contrato N2). Idempotente por <see cref="MatriculaId"/>.
/// </summary>
public sealed class Cobranca : AuditableEntity
{
    public const string StatusPendente = "pendente";

    public long MatriculaId { get; private set; }
    public int ValorCentavos { get; private set; }
    public DateOnly Vencimento { get; private set; }
    public string Status { get; private set; } = StatusPendente;

    private Cobranca()
    {
    }

    public static Cobranca Create(long matriculaId, int valorCentavos, DateOnly vencimento, DateTime createdAt)
    {
        if (matriculaId <= 0)
        {
            throw new ArgumentException("matriculaId é obrigatório.");
        }

        if (valorCentavos <= 0)
        {
            throw new ArgumentException("valorCentavos deve ser positivo.");
        }

        if (vencimento.Year is < 2000 or > 2100)
        {
            throw new ArgumentException("vencimento inválido.");
        }

        return new Cobranca
        {
            MatriculaId = matriculaId,
            ValorCentavos = valorCentavos,
            Vencimento = vencimento,
            Status = StatusPendente,
            CreatedAt = createdAt
        };
    }

    public bool Matches(int valorCentavos, DateOnly vencimento) =>
        ValorCentavos == valorCentavos && Vencimento == vencimento;
}
