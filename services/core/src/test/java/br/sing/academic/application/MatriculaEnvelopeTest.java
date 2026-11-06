package br.sing.academic.application;

import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.Workshop;
import br.sing.people.domain.entities.Student;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

class MatriculaEnvelopeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void envelopeSegueOContrato() throws Exception {
        Student aluno = Student.builder().id(501L).name("Aluno Exemplo Fictício").build();
        Workshop oficina = Workshop.builder().id(12L).name("Oficina Exemplo de Teatro").build();
        SchoolRoom turma = SchoolRoom.builder().id(77L).name("Turma Exemplo Manhã").workshop(oficina).build();
        Enrollment enrollment = Enrollment.builder()
                .id(9001L)
                .student(aluno)
                .schoolroom(turma)
                .enrollmentDate(LocalDate.of(2026, 11, 5))
                .status(true)
                .build();

        String json = MatriculaEnvelope.json(
                enrollment,
                "11111111-1111-4111-8111-111111111111",
                Instant.parse("2026-11-05T18:30:00Z"));
        JsonNode raiz = mapper.readTree(json);

        assertThat(nomes(raiz)).containsExactly("eventId", "eventType", "version", "occurredAt", "payload");
        assertThat(raiz.get("eventId").asText()).isEqualTo("11111111-1111-4111-8111-111111111111");
        assertThat(raiz.get("eventType").asText()).isEqualTo("matricula.criada");
        assertThat(raiz.get("version").isNumber()).isTrue();
        assertThat(raiz.get("version").intValue()).isEqualTo(1);
        assertThat(raiz.get("occurredAt").asText()).isEqualTo("2026-11-05T18:30:00Z");
        assertThat(raiz.has("aggregateId")).isFalse();
        assertThat(raiz.has("tenantId")).isFalse();

        JsonNode payload = raiz.get("payload");
        assertThat(payload.get("matriculaId").isNumber()).isTrue();
        assertThat(payload.get("matriculaId").longValue()).isEqualTo(9001L);
        assertThat(payload.get("data").asText()).isEqualTo("2026-11-05");
        assertThat(payload.get("aluno").get("id").longValue()).isEqualTo(501L);
        assertThat(payload.get("aluno").get("nome").asText()).isEqualTo("Aluno Exemplo Fictício");
        assertThat(payload.get("turma").get("nome").asText()).isEqualTo("Turma Exemplo Manhã");
        assertThat(payload.get("oficina").get("id").longValue()).isEqualTo(12L);
        assertThat(payload.has("curso")).isFalse();
        assertThat(payload.has("alunoId")).isFalse();
        assertThat(payload.has("turmaId")).isFalse();
    }

    @Test
    void instanteCortaAFracaoEUsaZ() {
        assertThat(MatriculaEnvelope.instante(Instant.parse("2026-11-05T18:30:00.987Z")))
                .isEqualTo("2026-11-05T18:30:00Z");
    }

    private static java.util.List<String> nomes(JsonNode node) {
        java.util.List<String> nomes = new java.util.ArrayList<>();
        Iterator<String> fields = node.fieldNames();
        fields.forEachRemaining(nomes::add);
        return nomes;
    }
}
