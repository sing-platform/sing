package br.cadi.academic.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pré-visualização antes de desativar oficina e turmas em cascata")
public record WorkshopDeactivationPreviewResponse(
        @Schema(description = "ID da oficina", example = "1") Long workshopId,
        @Schema(description = "Nome da oficina", example = "Violão") String workshopName,
        @Schema(description = "Se a desativação em cascata pode prosseguir (ex.: sem matrículas ativas bloqueando)", example = "true") boolean canProceed,
        @Schema(description = "Turmas vinculadas e impacto") List<SchoolRoomDeactivationImpactRow> schoolRooms
) {
}
