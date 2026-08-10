package br.cadi.academic.api.controller.examples;

/**
 * Exemplos JSON para OpenAPI.
 */
public final class AcademicApiExamples {

    public static final String WORKSHOP_JSON = "{\n"
            + "  \"id\": 1,\n"
            + "  \"name\": \"Violão\",\n"
            + "  \"active\": true\n"
            + "}";

    public static final String WORKSHOP_LIST_JSON = "[\n"
            + "  {\n"
            + "    \"id\": 1,\n"
            + "    \"name\": \"Violão\",\n"
            + "    \"active\": true\n"
            + "  }\n"
            + "]";

    public static final String SCHOOL_ROOM_JSON = "{\n"
            + "  \"id\": 10,\n"
            + "  \"name\": \"Violão — Turma A\",\n"
            + "  \"schedule\": \"Seg 08:00–09:30\",\n"
            + "  \"scheduleSlots\": [\n"
            + "    {\n"
            + "      \"id\": 100,\n"
            + "      \"dayOfWeek\": \"MONDAY\",\n"
            + "      \"startTime\": \"08:00\",\n"
            + "      \"endTime\": \"09:30\"\n"
            + "    }\n"
            + "  ],\n"
            + "  \"classroom\": \"Sala 12\",\n"
            + "  \"capacity\": 20,\n"
            + "  \"active\": true,\n"
            + "  \"workshopId\": 1,\n"
            + "  \"workshopName\": \"Violão\",\n"
            + "  \"teacherId\": 5,\n"
            + "  \"teacherName\": \"Maria Silva\",\n"
            + "  \"activeEnrollmentCount\": 12,\n"
            + "  \"vacancies\": 8\n"
            + "}";

    public static final String SCHOOLROOM_LIST_JSON = "[\n" + SCHOOL_ROOM_JSON + "\n]";

    public static final String WORKSHOP_DEACTIVATION_PREVIEW_JSON = "{\n"
            + "  \"workshopId\": 1,\n"
            + "  \"workshopName\": \"Violão\",\n"
            + "  \"canProceed\": true,\n"
            + "  \"schoolRooms\": [\n"
            + "    {\n"
            + "      \"id\": 10,\n"
            + "      \"name\": \"Violão — Turma A\",\n"
            + "      \"active\": true,\n"
            + "      \"hasActiveEnrollments\": false\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String VALIDATION_SCHOOLROOMS = "{\n"
            + "  \"status\": 400,\n"
            + "  \"error\": \"Validation Failed\",\n"
            + "  \"message\": \"One or more fields are invalid\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms\",\n"
            + "  \"fields\": [\n"
            + "    {\n"
            + "      \"field\": \"name\",\n"
            + "      \"rejectedValue\": null,\n"
            + "      \"message\": \"Nome da turma é obrigatório\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String VALIDATION_SCHOOLROOMS_ID = "{\n"
            + "  \"status\": 400,\n"
            + "  \"error\": \"Validation Failed\",\n"
            + "  \"message\": \"One or more fields are invalid\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/1\",\n"
            + "  \"fields\": [\n"
            + "    {\n"
            + "      \"field\": \"name\",\n"
            + "      \"rejectedValue\": null,\n"
            + "      \"message\": \"Nome da turma é obrigatório\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String VALIDATION_WORKSHOPS = "{\n"
            + "  \"status\": 400,\n"
            + "  \"error\": \"Validation Failed\",\n"
            + "  \"message\": \"One or more fields are invalid\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops\",\n"
            + "  \"fields\": [\n"
            + "    {\n"
            + "      \"field\": \"name\",\n"
            + "      \"rejectedValue\": null,\n"
            + "      \"message\": \"Nome da oficina é obrigatório\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String VALIDATION_WORKSHOPS_ID = "{\n"
            + "  \"status\": 400,\n"
            + "  \"error\": \"Validation Failed\",\n"
            + "  \"message\": \"One or more fields are invalid\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/1\",\n"
            + "  \"fields\": [\n"
            + "    {\n"
            + "      \"field\": \"name\",\n"
            + "      \"rejectedValue\": null,\n"
            + "      \"message\": \"Nome da oficina é obrigatório\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    public static final String UNAUTHORIZED_WORKSHOPS = "{\n"
            + "  \"status\": 401,\n"
            + "  \"error\": \"Unauthorized\",\n"
            + "  \"message\": \"Authentication required\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops\"\n"
            + "}";

    public static final String UNAUTHORIZED_WORKSHOPS_ID = "{\n"
            + "  \"status\": 401,\n"
            + "  \"error\": \"Unauthorized\",\n"
            + "  \"message\": \"Authentication required\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/1\"\n"
            + "}";

    public static final String UNAUTHORIZED_WORKSHOPS_PREVIEW = "{\n"
            + "  \"status\": 401,\n"
            + "  \"error\": \"Unauthorized\",\n"
            + "  \"message\": \"Authentication required\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/1/deactivation-preview\"\n"
            + "}";

    public static final String FORBIDDEN_WORKSHOPS = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Access denied\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops\"\n"
            + "}";

    public static final String FORBIDDEN_WORKSHOPS_ID = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Access denied\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/1\"\n"
            + "}";

    public static final String NOT_FOUND_WORKSHOP = "{\n"
            + "  \"status\": 404,\n"
            + "  \"error\": \"Not Found\",\n"
            + "  \"message\": \"Oficina não encontrada\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/99\"\n"
            + "}";

    public static final String NOT_FOUND_WORKSHOP_PREVIEW = "{\n"
            + "  \"status\": 404,\n"
            + "  \"error\": \"Not Found\",\n"
            + "  \"message\": \"Oficina não encontrada\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/99/deactivation-preview\"\n"
            + "}";

    public static final String CONFLICT_WORKSHOP_NAME = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Já existe uma oficina com este nome\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops\"\n"
            + "}";

    public static final String CONFLICT_WORKSHOP_NAME_PUT = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Já existe uma oficina com este nome\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/1\"\n"
            + "}";

    public static final String CONFLICT_WORKSHOP_DEACTIVATE = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Não é possível desativar a oficina: há matrículas ativas nas turmas: Violão — Turma A\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/workshops/1\"\n"
            + "}";

    public static final String UNAUTHORIZED_SCHOOLROOMS = "{\n"
            + "  \"status\": 401,\n"
            + "  \"error\": \"Unauthorized\",\n"
            + "  \"message\": \"Authentication required\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms\"\n"
            + "}";

    public static final String UNAUTHORIZED_SCHOOLROOMS_ID = "{\n"
            + "  \"status\": 401,\n"
            + "  \"error\": \"Unauthorized\",\n"
            + "  \"message\": \"Authentication required\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10\"\n"
            + "}";

    public static final String FORBIDDEN_SCHOOLROOMS_WRITE = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Access denied\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms\"\n"
            + "}";

    public static final String FORBIDDEN_SCHOOLROOMS_ID = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Access denied\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10\"\n"
            + "}";

    public static final String FORBIDDEN_SCHOOLROOM_ENROLLMENTS = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Access denied\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10/enrollments\"\n"
            + "}";

    public static final String FORBIDDEN_SCHOOLROOM_STUDENTS = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Access denied\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10/students\"\n"
            + "}";

    public static final String NOT_FOUND_SCHOOLROOM = "{\n"
            + "  \"status\": 404,\n"
            + "  \"error\": \"Not Found\",\n"
            + "  \"message\": \"Turma não encontrada\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/99\"\n"
            + "}";

    public static final String NOT_FOUND_WORKSHOP_FOR_SCHOOLROOM = "{\n"
            + "  \"status\": 404,\n"
            + "  \"error\": \"Not Found\",\n"
            + "  \"message\": \"Oficina não encontrada\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms\"\n"
            + "}";

    public static final String CONFLICT_SCHOOLROOM_GENERIC = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Já existe turma ativa com este nome para a oficina informada\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms\"\n"
            + "}";

    public static final String CONFLICT_SCHOOLROOM_PUT = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Já existe turma ativa com este nome para a oficina informada\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10\"\n"
            + "}";

    public static final String CONFLICT_SCHOOLROOM_DEACTIVATE = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Não é possível desativar turma com matrículas ativas\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10\"\n"
            + "}";

    public static final String CONFLICT_TEACHER_ALREADY = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Professor já vinculado a esta turma\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10/teacher\"\n"
            + "}";

    public static final String CONFLICT_TEACHER_NONE = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Turma não possui professor vinculado\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10/teacher\"\n"
            + "}";

    public static final String DESCRIPTIVE_REPORT_REQUEST_JSON = "{\n"
            + "  \"studentId\": 20,\n"
            + "  \"schoolRoomId\": 40,\n"
            + "  \"content\": \"Aluno apresentou evolução consistente em leitura e participação.\"\n"
            + "}";

    public static final String DESCRIPTIVE_REPORT_RESPONSE_JSON = "{\n"
            + "  \"id\": 100,\n"
            + "  \"studentId\": 20,\n"
            + "  \"studentName\": \"Aluno B\",\n"
            + "  \"schoolRoomId\": 40,\n"
            + "  \"schoolRoomName\": \"Turma 6A\",\n"
            + "  \"workshopId\": 30,\n"
            + "  \"workshopName\": \"Robótica\",\n"
            + "  \"authorId\": 10,\n"
            + "  \"authorName\": \"Professor A\",\n"
            + "  \"content\": \"Aluno apresentou evolução consistente em leitura e participação.\",\n"
            + "  \"status\": \"DRAFT\",\n"
            + "  \"createdAt\": \"2026-05-06T10:30:00\",\n"
            + "  \"updatedAt\": \"2026-05-06T10:30:00\",\n"
            + "  \"publishedAt\": null\n"
            + "}";

    public static final String DESCRIPTIVE_REPORT_LIST_JSON = "[\n"
            + DESCRIPTIVE_REPORT_RESPONSE_JSON + "\n"
            + "]";

    public static final String UNAUTHORIZED_DESCRIPTIVE_REPORTS = "{\n"
            + "  \"status\": 401,\n"
            + "  \"error\": \"Unauthorized\",\n"
            + "  \"message\": \"Authentication required\",\n"
            + "  \"timestamp\": \"2026-05-06T10:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/descriptive-reports\"\n"
            + "}";

    public static final String FORBIDDEN_DESCRIPTIVE_REPORTS = "{\n"
            + "  \"status\": 403,\n"
            + "  \"error\": \"Forbidden\",\n"
            + "  \"message\": \"Acesso negado\",\n"
            + "  \"timestamp\": \"2026-05-06T10:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/descriptive-reports\"\n"
            + "}";

    public static final String NOT_FOUND_DESCRIPTIVE_REPORT = "{\n"
            + "  \"status\": 404,\n"
            + "  \"error\": \"Not Found\",\n"
            + "  \"message\": \"Parecer não encontrado\",\n"
            + "  \"timestamp\": \"2026-05-06T10:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/descriptive-reports/999\"\n"
            + "}";

    public static final String CONFLICT_DESCRIPTIVE_REPORT = "{\n"
            + "  \"status\": 409,\n"
            + "  \"error\": \"Conflict\",\n"
            + "  \"message\": \"Parecer publicado ou fechado não pode ser editado/excluído.\",\n"
            + "  \"timestamp\": \"2026-05-06T10:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/descriptive-reports/100\"\n"
            + "}";

    public static final String NOT_FOUND_TEACHER_ASSIGN = "{\n"
            + "  \"status\": 404,\n"
            + "  \"error\": \"Not Found\",\n"
            + "  \"message\": \"Professor não encontrado\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10/teacher\"\n"
            + "}";

    public static final String VALIDATION_TEACHER_BODY = "{\n"
            + "  \"status\": 400,\n"
            + "  \"error\": \"Validation Failed\",\n"
            + "  \"message\": \"One or more fields are invalid\",\n"
            + "  \"timestamp\": \"2026-04-13T15:30:00.123Z\",\n"
            + "  \"path\": \"/api/v1/schoolrooms/10/teacher\",\n"
            + "  \"fields\": [\n"
            + "    {\n"
            + "      \"field\": \"teacherId\",\n"
            + "      \"rejectedValue\": null,\n"
            + "      \"message\": \"Teacher ID is mandatory\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    private AcademicApiExamples() {
    }
}
