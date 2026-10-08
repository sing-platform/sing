package br.sing.academic.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EnrollmentRequest(

        @NotNull(message = "O ID do aluno é obrigatório")
        Long studentId,

        @NotEmpty(message = "Informe ao menos uma turma")
        @Size(min = 1, max = 2, message = "Informe no máximo duas turmas por requisição")
        List<@NotNull(message = "ID de turma inválido") Long> schoolRoomIds

) {}
