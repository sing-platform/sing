package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Vínculo aluno-turma (matrícula) com dados resumidos para gestão")
public record EnrollmentLinkResponse(
        @Schema(description = "ID da matrícula") Long enrollmentId,
        @Schema(description = "ID do aluno (person_id)") Long studentId,
        String studentName,
        String studentEmail,
        @Schema(description = "ID da turma") Long schoolRoomId,
        String schoolRoomName,
        Long workshopId,
        String workshopName,
        Long teacherId,
        String teacherName,
        LocalDate enrollmentDate,
        boolean active
) {
}
