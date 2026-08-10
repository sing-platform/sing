package br.cadi.document.api.dto.request;

import br.cadi.document.domain.enums.DocumentCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** Usado apenas pelo SpringDoc para renderizar o formulário de upload no Swagger UI. */
public class DocumentUploadSwaggerBody {

    @Schema(type = "string", format = "binary", description = "Arquivo (PDF, JPEG, PNG, WEBP — máx. 10 MB)", requiredMode = Schema.RequiredMode.REQUIRED)
    public MultipartFile file;

    @Schema(description = "Categoria do documento", requiredMode = Schema.RequiredMode.REQUIRED)
    public DocumentCategory category;

    @Schema(description = "Descrição opcional (máx. 500 caracteres)")
    public String description;

    @Schema(description = "Início do período coberto pelo atestado (apenas para ATESTADO_MEDICO/JUSTIFICATIVA_FALTA). Sugestão do aluno — secretaria pode editar ao aprovar.", example = "2026-05-20")
    public LocalDate referenceDateStart;

    @Schema(description = "Fim do período coberto pelo atestado (mesmo dia que start se for 1 dia só).", example = "2026-05-20")
    public LocalDate referenceDateEnd;
}
