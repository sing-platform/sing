namespace Sing.Finance.Application.Contracts;

/// <summary>
/// Violação do índice único de <c>matriculaId</c> (corrida entre requisições).
/// </summary>
public sealed class CobrancaConflictException : Exception
{
    public CobrancaConflictException() : base("Cobrança já existe para a matrícula.")
    {
    }

    public CobrancaConflictException(Exception inner) : base("Cobrança já existe para a matrícula.", inner)
    {
    }
}
