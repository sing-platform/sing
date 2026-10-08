package br.sing.academic.application;

import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.enums.VacancyCause;
import br.sing.academic.domain.events.SchoolRoomVacancyOpened;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.people.domain.enums.Shift;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SchoolRoomVacancyService {

    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void notifyIfVacancyOpened(SchoolRoom room, long countBefore, long countAfter, VacancyCause cause) {
        if (room == null || room.getCapacity() == null) {
            return;
        }
        int capacity = room.getCapacity();
        boolean wasFullBefore = countBefore >= capacity;
        boolean hasVacancyNow = countAfter < capacity;
        if (!wasFullBefore || !hasVacancyNow) {
            return;
        }

        Set<Shift> shifts = ShiftResolver.shiftsFromRoom(room);
        if (shifts.isEmpty()) {
            return;
        }

        String workshopName = room.getWorkshop() != null && room.getWorkshop().getName() != null
                ? room.getWorkshop().getName()
                : "";

        eventPublisher.publishEvent(new SchoolRoomVacancyOpened(
                room.getId(),
                room.getWorkshop() != null ? room.getWorkshop().getId() : null,
                workshopName,
                shifts,
                (int) Math.max(0L, capacity - countAfter),
                cause,
                Instant.now()
        ));
    }

    public void notifyIfVacancyOpened(Long schoolRoomId, long countBefore, long countAfter, VacancyCause cause) {
        schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoomId)
                .ifPresent(room -> notifyIfVacancyOpened(room, countBefore, countAfter, cause));
    }

    public void notifyIfCapacityIncreaseOpenedVacancy(
            SchoolRoom room,
            Integer previousCapacity,
            Integer newCapacity
    ) {
        if (room == null || previousCapacity == null || newCapacity == null || newCapacity <= previousCapacity) {
            return;
        }
        long count = enrollmentRepository.countBySchoolroomIdAndStatusTrue(room.getId());
        boolean wasFullBefore = count >= previousCapacity;
        boolean hasVacancyNow = count < newCapacity;
        if (!wasFullBefore || !hasVacancyNow) {
            return;
        }

        Set<Shift> shifts = ShiftResolver.shiftsFromRoom(room);
        if (shifts.isEmpty()) {
            return;
        }

        String workshopName = room.getWorkshop() != null && room.getWorkshop().getName() != null
                ? room.getWorkshop().getName()
                : "";

        eventPublisher.publishEvent(new SchoolRoomVacancyOpened(
                room.getId(),
                room.getWorkshop() != null ? room.getWorkshop().getId() : null,
                workshopName,
                shifts,
                (int) Math.max(0L, newCapacity - count),
                VacancyCause.CAPACITY_INCREASE,
                Instant.now()
        ));
    }
}
