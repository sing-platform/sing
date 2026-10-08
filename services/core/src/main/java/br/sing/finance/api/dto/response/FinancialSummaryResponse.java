package br.sing.finance.api.dto.response;

import java.math.BigDecimal;

public record FinancialSummaryResponse(
        BigDecimal income,
        BigDecimal expense,
        BigDecimal balance,
        BigDecimal pendingReceivable,
        BigDecimal pendingPayable
) {
}
