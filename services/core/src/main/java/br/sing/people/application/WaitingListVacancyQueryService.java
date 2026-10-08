package br.sing.people.application;

import br.sing.academic.application.ShiftResolver;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.people.api.dto.response.WaitingListVacancyAlertResponse;
import br.sing.people.domain.entities.StudentPreRegister;
import br.sing.people.domain.enums.Shift;
import br.sing.people.infra.repositories.StudentPreRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WaitingListVacancyQueryService {

    private final SchoolRoomRepository schoolRoomRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentPreRegisterRepository studentPreRegisterRepository;

    @Transactional(readOnly = true)
    public boolean hasVacancyForWorkshopShift(Long workshopId, Shift shift) {
        return countAvailableVacanciesForWorkshopShift(workshopId, shift) > 0;
    }

    @Transactional(readOnly = true)
    public long countAvailableVacanciesForWorkshopShift(Long workshopId, Shift shift) {
        List<SchoolRoom> rooms = schoolRoomRepository.findByWorkshop_Id(workshopId);
        long total = 0L;
        for (SchoolRoom room : rooms) {
            if (!Boolean.TRUE.equals(room.isActive())) {
                continue;
            }
            SchoolRoom loaded = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(room.getId()).orElse(room);
            if (!ShiftResolver.roomMatchesShift(loaded, shift)) {
                continue;
            }
            Integer capacity = loaded.getCapacity();
            if (capacity == null) {
                continue;
            }
            long count = enrollmentRepository.countBySchoolroomIdAndStatusTrue(loaded.getId());
            if (count < capacity) {
                total += capacity - count;
            }
        }
        return total;
    }

    @Transactional(readOnly = true)
    public List<WaitingListVacancyAlertResponse> listActiveAlerts() {
        List<StudentPreRegister> all = studentPreRegisterRepository.findAll();
        Map<String, WaitingListVacancyAlertResponse> alerts = new LinkedHashMap<>();

        for (StudentPreRegister pre : all) {
            Long workshopId = pre.getWorkshop().getId();
            Shift shift = pre.getShift();
            String key = workshopId + ":" + shift.name();

            if (alerts.containsKey(key)) {
                WaitingListVacancyAlertResponse existing = alerts.get(key);
                alerts.put(key, new WaitingListVacancyAlertResponse(
                        existing.workshopId(),
                        existing.workshopName(),
                        existing.shift(),
                        existing.waitingCount() + 1,
                        existing.availableVacancies()
                ));
            } else {
                long vacancies = countAvailableVacanciesForWorkshopShift(workshopId, shift);
                if (vacancies <= 0) {
                    continue;
                }
                alerts.put(key, new WaitingListVacancyAlertResponse(
                        workshopId,
                        pre.getWorkshop().getName(),
                        shift,
                        1,
                        vacancies
                ));
            }
        }

        return new ArrayList<>(alerts.values());
    }
}
