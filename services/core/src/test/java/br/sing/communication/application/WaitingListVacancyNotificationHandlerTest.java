package br.sing.communication.application;

import br.sing.academic.domain.enums.VacancyCause;
import br.sing.academic.domain.events.SchoolRoomVacancyOpened;
import br.sing.people.domain.enums.Shift;
import br.sing.people.infra.repositories.StudentPreRegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingListVacancyNotificationHandlerTest {

    @Mock
    private StudentPreRegisterRepository studentPreRegisterRepository;
    @Mock
    private WaitingListVacancyPublisher vacancyPublisher;

    private WaitingListVacancyNotificationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WaitingListVacancyNotificationHandler(studentPreRegisterRepository, vacancyPublisher);
    }

    private static SchoolRoomVacancyOpened event(Long workshopId, Set<Shift> shifts) {
        return new SchoolRoomVacancyOpened(1L, workshopId, "Violão", shifts, 2, VacancyCause.values()[0], Instant.now());
    }

    @Test
    void ignoraEventoSemOficina() {
        handler.onSchoolRoomVacancyOpened(event(null, Set.of(Shift.MORNING)));

        verifyNoInteractions(vacancyPublisher);
    }

    @Test
    void publicaApenasParaTurnosComEspera() {
        Set<Shift> shifts = new LinkedHashSet<>(List.of(Shift.MORNING, Shift.AFTERNOON));
        when(studentPreRegisterRepository.countByWorkshop_IdAndShift(4L, Shift.MORNING)).thenReturn(3L);
        when(studentPreRegisterRepository.countByWorkshop_IdAndShift(4L, Shift.AFTERNOON)).thenReturn(0L);

        handler.onSchoolRoomVacancyOpened(event(4L, shifts));

        verify(vacancyPublisher).publishIfApplicable(4L, "Violão", Shift.MORNING, 2, 3);
        org.mockito.Mockito.verifyNoMoreInteractions(vacancyPublisher);
    }
}
