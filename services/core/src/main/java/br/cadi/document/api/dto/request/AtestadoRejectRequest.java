package br.cadi.document.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtestadoRejectRequest(
        @NotBlank
        @Size(max = 500, message = "Motivo da rejeicao deve ter no maximo 500 caracteres.")
        String reason
) {
}
