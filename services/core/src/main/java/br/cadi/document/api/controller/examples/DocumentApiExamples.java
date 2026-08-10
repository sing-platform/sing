package br.cadi.document.api.controller.examples;

public final class DocumentApiExamples {

    private DocumentApiExamples() {}

    public static final String DOCUMENT_JSON = "{\n"
            + "  \"id\": 1,\n"
            + "  \"ownerPersonId\": 42,\n"
            + "  \"originalFileName\": \"atestado_medico.pdf\",\n"
            + "  \"mimeType\": \"application/pdf\",\n"
            + "  \"fileSize\": 204800,\n"
            + "  \"category\": \"ATESTADO_MEDICO\",\n"
            + "  \"scope\": \"STUDENT_PERSONAL\",\n"
            + "  \"description\": \"Atestado de afastamento por gripe\",\n"
            + "  \"uploadedAt\": \"2025-05-15T10:30:00Z\"\n"
            + "}";

    public static final String DOCUMENT_LIST_JSON = "[\n"
            + "  {\n"
            + "    \"id\": 1,\n"
            + "    \"ownerPersonId\": 42,\n"
            + "    \"originalFileName\": \"atestado_medico.pdf\",\n"
            + "    \"mimeType\": \"application/pdf\",\n"
            + "    \"fileSize\": 204800,\n"
            + "    \"category\": \"ATESTADO_MEDICO\",\n"
            + "    \"scope\": \"STUDENT_PERSONAL\",\n"
            + "    \"description\": \"Atestado de afastamento por gripe\",\n"
            + "    \"uploadedAt\": \"2025-05-15T10:30:00Z\"\n"
            + "  }\n"
            + "]";

    public static final String THERAPEUTIC_JSON = "{\n"
            + "  \"id\": 7,\n"
            + "  \"ownerPersonId\": 55,\n"
            + "  \"originalFileName\": \"sessao_01.pdf\",\n"
            + "  \"mimeType\": \"application/pdf\",\n"
            + "  \"fileSize\": 51200,\n"
            + "  \"category\": \"ANOTACAO_TERAPEUTICA\",\n"
            + "  \"scope\": \"THERAPEUTIC_PRIVATE\",\n"
            + "  \"description\": \"Sessão inicial de acolhimento\",\n"
            + "  \"uploadedAt\": \"2025-05-15T14:00:00Z\"\n"
            + "}";

    public static final String THERAPEUTIC_LIST_JSON = "[\n"
            + "  {\n"
            + "    \"id\": 7,\n"
            + "    \"ownerPersonId\": 55,\n"
            + "    \"originalFileName\": \"sessao_01.pdf\",\n"
            + "    \"mimeType\": \"application/pdf\",\n"
            + "    \"fileSize\": 51200,\n"
            + "    \"category\": \"ANOTACAO_TERAPEUTICA\",\n"
            + "    \"scope\": \"THERAPEUTIC_PRIVATE\",\n"
            + "    \"description\": \"Sessão inicial de acolhimento\",\n"
            + "    \"uploadedAt\": \"2025-05-15T14:00:00Z\"\n"
            + "  }\n"
            + "]";

    public static final String UNAUTHORIZED = "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Token ausente ou inválido\"}";

    public static final String FORBIDDEN = "{\"status\": 403, \"error\": \"Forbidden\", \"message\": \"Sem permissão para esta operação\"}";

    public static final String NOT_FOUND = "{\"status\": 404, \"error\": \"Not Found\", \"message\": \"Documento não encontrado\"}";

    public static final String VALIDATION_ERROR = "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"category: não deve ser nulo\"}";

    public static final String UNSUPPORTED_MEDIA = "{\"status\": 415, \"error\": \"Unsupported Media Type\", \"message\": \"Tipo de arquivo não permitido. Use PDF, JPEG, PNG ou WEBP\"}";
}
