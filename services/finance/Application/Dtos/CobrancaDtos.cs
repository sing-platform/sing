namespace Sing.Finance.Application.Dtos;

public sealed record CobrancaCreateRequest(long MatriculaId, int ValorCentavos, DateOnly Vencimento);

public sealed record CobrancaResponse(long Id, string Status);
