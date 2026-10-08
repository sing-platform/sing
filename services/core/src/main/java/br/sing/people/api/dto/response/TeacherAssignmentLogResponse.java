package br.sing.people.api.dto.response;

import java.time.LocalDateTime;

public record TeacherAssignmentLogResponse(
        Long    schoolRoomId,
        Long    teacherId,
        String  teacherName,
        Long    changedBy,
        LocalDateTime changedAt
) {}