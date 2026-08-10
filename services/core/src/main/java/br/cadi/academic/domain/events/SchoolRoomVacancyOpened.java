package br.cadi.academic.domain.events;

import br.cadi.academic.domain.enums.VacancyCause;
import br.cadi.people.domain.enums.Shift;

import java.time.Instant;
import java.util.Set;

/**
 * Emitido quando uma turma passa de lotada para com vaga disponível.
 */
public record SchoolRoomVacancyOpened(
        Long schoolRoomId,
        Long workshopId,
        String workshopName,
        Set<Shift> shifts,
        int availableSlots,
        VacancyCause cause,
        Instant occurredAt
) {
}
