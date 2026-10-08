package br.sing.communication.application;

import br.sing.academic.domain.events.SchoolRoomVacancyOpened;
import br.sing.people.domain.enums.Shift;
import br.sing.people.infra.repositories.StudentPreRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WaitingListVacancyNotificationHandler {

    private final StudentPreRegisterRepository studentPreRegisterRepository;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSchoolRoomVacancyOpened(SchoolRoomVacancyOpened event) {
        if (event.workshopId() == null) {
            return;
        }
        for (Shift shift : event.shifts()) {
            long waiting = studentPreRegisterRepository.countByWorkshop_IdAndShift(event.workshopId(), shift);
            if (waiting == 0) {
                continue;
            }

            notificationService.createWaitingListVacancyIfAbsent(
                    event.workshopId(),
                    event.workshopName(),
                    shift,
                    event.availableSlots(),
                    waiting
            );
        }
    }
}
