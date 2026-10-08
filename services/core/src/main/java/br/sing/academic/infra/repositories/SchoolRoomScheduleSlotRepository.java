package br.sing.academic.infra.repositories;

import br.sing.academic.domain.entities.SchoolRoomScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SchoolRoomScheduleSlotRepository extends JpaRepository<SchoolRoomScheduleSlot, Long>, JpaSpecificationExecutor<SchoolRoomScheduleSlot> {
    List<SchoolRoomScheduleSlot> findBySchoolroomId(Long schoolroomId);
    Boolean existsBySchoolroomIdAndDayOfWeek(Long schoolroomId, DayOfWeek day);

}
