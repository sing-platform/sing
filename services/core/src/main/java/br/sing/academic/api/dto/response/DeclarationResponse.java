package br.sing.academic.api.dto.response;
import java.time.LocalDate;


public record DeclarationResponse(
        Long studentId,
        String studentName,
        LocalDate studentBirthDate,
        String guardianName,
        Long schoolRoomId,
        String schoolRoomName,
        String workshopName,
        String period,
        String teacherName,
        LocalDate enrollmentDate,
        // Nome do secretário logado — preenchido no service a partir do SecurityContext
        String secretaryName
) {

    public String formattedSchedule() {
        return period != null && !period.isBlank() ? period.trim() : "durante horário não informado";
    }
}