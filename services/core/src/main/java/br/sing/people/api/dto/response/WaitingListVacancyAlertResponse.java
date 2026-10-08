package br.sing.people.api.dto.response;

import br.sing.people.domain.enums.Shift;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alerta de vaga disponível para oficina/turno com candidatos na lista de espera")
public record WaitingListVacancyAlertResponse(
        @Schema(description = "Id da oficina") Long workshopId,
        @Schema(description = "Nome da oficina") String workshopName,
        @Schema(description = "Turno") Shift shift,
        @Schema(description = "Quantidade de candidatos na fila") long waitingCount,
        @Schema(description = "Total de vagas disponíveis nas turmas compatíveis") long availableVacancies
) {
}
