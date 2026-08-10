package br.cadi.people.api.dto.response;

import br.cadi.people.domain.enums.Shift;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Pre-registered student (waiting list candidate)")
public record StudentPreRegisterResponse(
        @Schema(description = "Record id", example = "1") Long id,
        @Schema(description = "Candidate full name", example = "Maria Silva") String name,
        @Schema(description = "Contact email", example = "maria@email.com") String email,
        @Schema(description = "Contact phone", example = "11999998888") String phone,
        @Schema(description = "Workshop of interest id", example = "1") Long workshopId,
        @Schema(description = "Workshop of interest name", example = "Violão") String workshopName,
        @Schema(description = "Preferred shift", example = "MORNING") Shift shift,
        @Schema(description = "Creation timestamp") LocalDateTime createdAt,
        @Schema(description = "When summons email was last sent") LocalDateTime summonedAt,
        @Schema(description = "Whether there is vacancy for this workshop/shift") boolean hasVacancy
) {
}
