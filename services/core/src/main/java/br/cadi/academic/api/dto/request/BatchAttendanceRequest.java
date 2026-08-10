package br.cadi.academic.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record BatchAttendanceRequest(
        @NotNull Long schoolroomId,
        @NotNull LocalDate date,
        String topic,
        @NotEmpty List<AttendanceItemRequest> attendances
) {}

