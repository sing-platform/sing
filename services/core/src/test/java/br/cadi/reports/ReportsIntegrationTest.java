package br.cadi.reports;

import br.cadi.reports.application.ReportRegistry;
import br.cadi.reports.application.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReportsIntegrationTest {

    @Autowired
    private ReportRegistry registry;

    @Autowired
    private ReportService reportService;

    @Test
    void catalogoContemRelatoriosEsperados() {
        assertThat(registry.all())
                .extracting(definition -> definition.slug())
                .containsExactlyInAnyOrder(
                        "alunos-em-risco",
                        "aniversariantes-mes",
                        "auditoria",
                        "cadastro-alunos",
                        "carga-horaria-professor",
                        "chamadas-pendentes",
                        "contas-vencidas",
                        "documentos-usuario",
                        "engajamento-mural",
                        "fluxo-caixa-mes",
                        "frequencia-por-turma",
                        "lancamentos-por-categoria",
                        "matriculas-por-turma",
                        "padrinhos-ativos",
                        "pareceres-por-status",
                        "pre-cadastros-lista",
                        "pre-cadastros-resumo",
                        "presenca-detalhada-aluno"
                );
    }

    @Test
    void executaRelatorioMatriculasSemErro() {
        var response = reportService.run("matriculas-por-turma", Map.of());
        assertThat(response.slug()).isEqualTo("matriculas-por-turma");
        assertThat(response.columns()).isNotEmpty();
    }

    @Test
    void executaRelatorioFrequenciaPorTurmaH2() {
        ReportDefinitionViaService result = new ReportDefinitionViaService(
                reportService.run("frequencia-por-turma", Map.of(
                        "start", "2025-01-01",
                        "end", "2025-01-31"
                ))
        );
        assertThat(result.slug).isEqualTo("frequencia-por-turma");
    }

    private record ReportDefinitionViaService(String slug) {
        ReportDefinitionViaService(br.cadi.reports.api.dto.ReportResultResponse r) {
            this(r.slug());
        }
    }
}
