package br.cadi.academic.api.dto.response;

import java.util.List;

public record ScheduleBlockResponse(
        boolean available,
        String unavailableReason,
        List<ScheduleItemResponse> items
) {}