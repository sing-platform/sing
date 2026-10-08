package br.sing.reports.application;

import br.sing.exception.BusinessException;
import br.sing.reports.application.definitions.StudentAttendanceDetailReport;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.infra.jdbc.ReportJdbcRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ReportFilterValidatorTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void validaPeriodoMaximo() {
        ReportJdbcRepository jdbc = new ReportJdbcRepository(jdbcTemplate);
        StudentAttendanceDetailReport report = new StudentAttendanceDetailReport(jdbc);
        ReportFilterValidator validator = new ReportFilterValidator();

        assertThatThrownBy(() -> validator.validateAndNormalize(report, Map.of(
                "start", "2020-01-01",
                "end", "2025-12-31"
        ))).isInstanceOf(BusinessException.class);
    }

    @Test
    void normalizaDatasObrigatorias() {
        ReportJdbcRepository jdbc = new ReportJdbcRepository(jdbcTemplate);
        StudentAttendanceDetailReport report = new StudentAttendanceDetailReport(jdbc);
        ReportFilterValidator validator = new ReportFilterValidator();

        Map<String, Object> normalized = validator.validateAndNormalize(report, Map.of(
                "start", "2025-01-01",
                "end", "2025-01-31"
        ));

        assertThat(normalized.get("start")).isNotNull();
        assertThat(normalized.get("end")).isNotNull();
    }
}
