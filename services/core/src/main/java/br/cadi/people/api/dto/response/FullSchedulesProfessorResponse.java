package br.cadi.people.api.dto.response;

import br.cadi.academic.api.dto.response.ScheduleSlotResponse;
import br.cadi.academic.api.dto.response.WorkshopResponse;
import br.cadi.people.api.dto.ProfessorStudentDTO;
import lombok.*;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FullSchedulesProfessorResponse {
    private WorkshopResponse workshop;
    private List<ProfessorStudentDTO> students;
    private List<ScheduleSlotResponse> schedules;
}