package br.sing.finance.domain;

import java.time.LocalDate;

public enum SponsorFrequency {
    MONTHLY(1),
    BIMONTHLY(2),
    QUARTERLY(3),
    SEMIANNUAL(6),
    ANNUAL(12);

    private final int months;

    SponsorFrequency(int months) {
        this.months = months;
    }

    public LocalDate nextDueDate(LocalDate from) {
        return from.plusMonths(months);
    }
}
