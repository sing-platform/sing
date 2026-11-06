package br.sing.academic.api.dto.response;

import java.util.List;

public record MatriculasCriadasResponse(
        List<MatriculaItemResponse> matriculas
) {}
