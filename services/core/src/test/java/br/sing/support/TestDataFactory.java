package br.sing.support;

import br.sing.academic.api.dto.request.ScheduleSlotRequest;
import br.sing.academic.api.dto.response.SchoolRoomResponse;
import br.sing.academic.api.dto.response.WorkshopResponse;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.Workshop;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.domain.enums.CollaboratorType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Workshop workshop(Long id, String name, boolean active) {
        return Workshop.builder().id(id).name(name).isActive(active).build();
    }

    public static SchoolRoom schoolRoom(Long id, String name, boolean active) {
        return SchoolRoom.builder().id(id).name(name).isActive(active).build();
    }

    public static Collaborator teacher(Long id, String email) {
        return Collaborator.builder()
                .id(id)
                .name("Professor " + id)
                .email(email)
                .cpf("11111111111")
                .phone("11999999999")
                .birthDate(LocalDate.of(1980, 1, 1))
                .educationLevel("Superior")
                .type(CollaboratorType.TEACHER)
                .build();
    }

    public static List<ScheduleSlotRequest> validSlots() {
        return List.of(new ScheduleSlotRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)));
    }

    public static SchoolRoomResponse schoolRoomResponse() {
        return new SchoolRoomResponse(1L, "Turma A", "Seg 08:00–10:00", List.of(),
                "Sala 1", 30, true, 1L, "Oficina", 10L, "Professor", 0, 30);
    }

    public static WorkshopResponse workshopResponse() {
        return new WorkshopResponse(1L, "Oficina A", true);
    }
}
