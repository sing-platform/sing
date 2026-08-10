package br.cadi.finance.api.dto.response;

import br.cadi.finance.domain.Sponsor;
import br.cadi.finance.domain.SponsorFrequency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SponsorResponse(
        Long id,
        Long studentId,
        String studentName,
        String name,
        String email,
        String phone,
        BigDecimal amount,
        SponsorFrequency frequency,
        LocalDate startDate,
        LocalDate lastGeneratedDate,
        boolean active
) {
    public static SponsorResponse from(Sponsor sponsor) {
        return new SponsorResponse(
                sponsor.getId(),
                sponsor.getStudent().getId(),
                sponsor.getStudent().getName(),
                sponsor.getName(),
                sponsor.getEmail(),
                sponsor.getPhone(),
                sponsor.getAmount(),
                sponsor.getFrequency(),
                sponsor.getStartDate(),
                sponsor.getLastGeneratedDate(),
                sponsor.isActive()
        );
    }
}
