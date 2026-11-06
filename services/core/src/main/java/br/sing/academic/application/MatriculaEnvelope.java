package br.sing.academic.application;

import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.Workshop;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** Envelope {@code matricula.criada} do contrato N2. O texto nasce uma vez e é republicado igual. */
public final class MatriculaEnvelope {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter INSTANTE = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(ZoneOffset.UTC);

    private MatriculaEnvelope() {
    }

    public static String instante(Instant occurredAt) {
        return INSTANTE.format(occurredAt.truncatedTo(ChronoUnit.SECONDS));
    }

    public static String json(Enrollment enrollment, String eventId, Instant occurredAt) {
        Map<String, Object> aluno = new LinkedHashMap<>();
        aluno.put("id", enrollment.getStudent().getId());
        aluno.put("nome", enrollment.getStudent().getName());

        Map<String, Object> turma = new LinkedHashMap<>();
        turma.put("id", enrollment.getSchoolroom().getId());
        turma.put("nome", enrollment.getSchoolroom().getName());

        Workshop oficinaEntidade = enrollment.getSchoolroom().getWorkshop();
        Map<String, Object> oficina = new LinkedHashMap<>();
        oficina.put("id", oficinaEntidade.getId());
        oficina.put("nome", oficinaEntidade.getName());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("matriculaId", enrollment.getId());
        payload.put("data", enrollment.getEnrollmentDate().toString());
        payload.put("aluno", aluno);
        payload.put("turma", turma);
        payload.put("oficina", oficina);

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", eventId);
        envelope.put("eventType", "matricula.criada");
        envelope.put("version", 1);
        envelope.put("occurredAt", instante(occurredAt));
        envelope.put("payload", payload);

        try {
            return MAPPER.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível montar o evento da matrícula", e);
        }
    }
}
