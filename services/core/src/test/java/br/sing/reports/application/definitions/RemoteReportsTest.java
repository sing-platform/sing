package br.sing.reports.application.definitions;

import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.StudentRepository;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.remote.CommunicationClient;
import br.sing.reports.infra.remote.FinanceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteReportsTest {

    @Mock
    private FinanceClient financeClient;
    @Mock
    private CommunicationClient communicationClient;
    @Mock
    private StudentRepository studentRepository;

    private static FinanceClient.Entry entry(long id, String direction, String status, LocalDate due,
                                             String amount, String category, String sponsorName, Boolean active) {
        return new FinanceClient.Entry(id, direction, status, due, new BigDecimal(amount), category,
                "desc " + id, null, sponsorName, active);
    }

    private static List<String> keys(ReportResult result) {
        return result.columns().stream().map(ReportColumn::key).toList();
    }

    @Test
    void fluxoCaixaMantemColunasFiltraMesEOrdenaPorVencimentoEId() {
        CashFlowMonthReport report = new CashFlowMonthReport(financeClient);
        when(financeClient.listEntries("2026-10")).thenReturn(List.of(
                entry(3, "PAYABLE", "PAID", LocalDate.of(2026, 10, 9), "40.00", "Material", null, true),
                entry(2, "RECEIVABLE", "PENDING", LocalDate.of(2026, 10, 5), "100.00", "Mensalidade", null, true),
                entry(1, "RECEIVABLE", "PENDING", LocalDate.of(2026, 10, 5), "50.00", "Mensalidade", null, true),
                entry(4, "RECEIVABLE", "PENDING", LocalDate.of(2026, 11, 1), "70.00", "Fora", null, true),
                entry(5, "RECEIVABLE", "PENDING", LocalDate.of(2026, 10, 6), "70.00", "Inativo", null, false)));

        ReportResult result = report.execute(Map.of("month", "2026-10"));

        assertThat(keys(result)).containsExactly("due_date", "direction", "status", "category", "amount", "description");
        assertThat(result.columns()).extracting(ReportColumn::label).isEqualTo(keys(result));
        assertThat(result.columns()).extracting(ReportColumn::type).containsExactly(
                ReportColumnType.DATE, ReportColumnType.STRING, ReportColumnType.STRING,
                ReportColumnType.STRING, ReportColumnType.NUMBER, ReportColumnType.STRING);
        assertThat(result.rows()).hasSize(3);
        assertThat(result.rows().get(0)).containsExactly(
                LocalDate.of(2026, 10, 5), "RECEIVABLE", "PENDING", "Mensalidade", new BigDecimal("50.00"), "desc 1");
        assertThat(result.rows().get(1).get(5)).isEqualTo("desc 2");
        assertThat(result.rows().get(2).get(0)).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(result.totalRows()).isEqualTo(3);
        assertThat(result.totals()).isEmpty();
    }

    @Test
    void fluxoCaixaSemMesUsaMesAtual() {
        CashFlowMonthReport report = new CashFlowMonthReport(financeClient);
        when(financeClient.listEntries(YearMonth.now().toString())).thenReturn(List.of());

        ReportResult result = report.execute(Map.of());

        assertThat(result.rows()).isEmpty();
        verify(financeClient).listEntries(YearMonth.now().toString());
    }

    @Test
    void lancamentosPorCategoriaAgregaPeriodoQueCruzaMeses() {
        EntriesByCategoryReport report = new EntriesByCategoryReport(financeClient);
        when(financeClient.listEntries(null)).thenReturn(List.of(
                entry(1, "RECEIVABLE", "PAID", LocalDate.of(2026, 9, 30), "100.00", "Mensalidade", null, true),
                entry(2, "RECEIVABLE", "PENDING", LocalDate.of(2026, 10, 1), "200.00", "Mensalidade", null, true),
                entry(3, "PAYABLE", "PENDING", LocalDate.of(2026, 10, 15), "50.00", "Mensalidade", null, true),
                entry(4, "PAYABLE", "PAID", LocalDate.of(2026, 11, 2), "30.00", "Material", null, true),
                entry(5, "RECEIVABLE", "PAID", LocalDate.of(2026, 12, 1), "999.00", "Fora", null, true),
                entry(6, "RECEIVABLE", "PAID", LocalDate.of(2026, 10, 20), "999.00", "Inativo", null, false)));

        ReportResult result = report.execute(Map.of("start", LocalDate.of(2026, 9, 30), "end", LocalDate.of(2026, 11, 30)));

        assertThat(keys(result)).containsExactly("category", "total_receivable", "total_payable", "balance");
        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0)).containsExactly(
                "Material", BigDecimal.ZERO, new BigDecimal("30.00"), new BigDecimal("-30.00"));
        assertThat(result.rows().get(1)).containsExactly(
                "Mensalidade", new BigDecimal("300.00"), new BigDecimal("50.00"), new BigDecimal("250.00"));
    }

    @Test
    void contasVencidasIncluiPendingEOverdueComVencimentoPassado() {
        OverdueEntriesReport report = new OverdueEntriesReport(financeClient);
        LocalDate today = LocalDate.now();
        when(financeClient.listEntries(null)).thenReturn(List.of(
                entry(1, "RECEIVABLE", "PENDING", today.minusDays(2), "10.00", "A", "Padrinho", true),
                entry(2, "RECEIVABLE", "OVERDUE", today.minusDays(5), "20.00", "B", null, true),
                entry(3, "RECEIVABLE", "PENDING", today, "30.00", "Hoje", null, true),
                entry(4, "RECEIVABLE", "PENDING", today.plusDays(1), "30.00", "Futuro", null, true),
                entry(5, "RECEIVABLE", "PAID", today.minusDays(9), "30.00", "Pago", null, true),
                entry(6, "RECEIVABLE", "CANCELED", today.minusDays(9), "30.00", "Cancelado", null, true),
                entry(7, "RECEIVABLE", "OVERDUE", today.minusDays(9), "30.00", "Inativo", null, false)));

        ReportResult result = report.execute(Map.of());

        assertThat(keys(result)).containsExactly(
                "id", "due_date", "direction", "category", "amount", "description", "sponsor_name");
        assertThat(result.rows()).extracting(r -> r.get(0)).containsExactly(2L, 1L);
        assertThat(result.rows().get(1).get(6)).isEqualTo("Padrinho");
        assertThat(result.rows().get(0).get(6)).isNull();
    }

    @Test
    void padrinhosAtivosUsaNomeDoAlunoDoCoreEIgnoraInativosESemAluno() {
        ActiveSponsorsReport report = new ActiveSponsorsReport(financeClient, studentRepository);
        when(financeClient.listSponsors()).thenReturn(List.of(
                new FinanceClient.Sponsor(2L, 20L, "Zeca", new BigDecimal("80"), "MONTHLY",
                        LocalDate.of(2026, 2, 1), null, true),
                new FinanceClient.Sponsor(1L, 10L, "Ana", new BigDecimal("100"), "YEARLY",
                        LocalDate.of(2026, 1, 15), LocalDate.of(2026, 10, 15), true),
                new FinanceClient.Sponsor(3L, 30L, "Inativo", new BigDecimal("1"), "MONTHLY",
                        LocalDate.of(2026, 1, 1), null, false),
                new FinanceClient.Sponsor(4L, 40L, "Sem aluno", new BigDecimal("1"), "MONTHLY",
                        LocalDate.of(2026, 1, 1), null, true)));
        when(studentRepository.findAllById(any())).thenReturn(List.of(
                Student.builder().id(10L).name("Aluno Dez").build(),
                Student.builder().id(20L).name("Aluno Vinte").build()));

        ReportResult result = report.execute(Map.of());

        assertThat(keys(result)).containsExactly(
                "id", "sponsor_name", "student_name", "amount", "frequency", "start_date", "last_generated_date");
        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0)).containsExactly(
                1L, "Ana", "Aluno Dez", new BigDecimal("100"), "YEARLY",
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 10, 15));
        assertThat(result.rows().get(1).get(2)).isEqualTo("Aluno Vinte");
        assertThat(result.rows().get(1).get(6)).isNull();
    }

    @Test
    void engajamentoDoMuralMapeiaColunasEPassaPeriodo() {
        MuralEngagementReport report = new MuralEngagementReport(communicationClient);
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);
        when(communicationClient.muralEngagement(eq(start), eq(end))).thenReturn(List.of(
                new CommunicationClient.MuralEngagement(5L, "Novo", "2026-11-06T09:00:00", "sec", 10L, 4L),
                new CommunicationClient.MuralEngagement(4L, "Velho", "2026-11-05T10:30:00", "admin", 12L, 7L)));

        ReportResult result = report.execute(Map.of("start", start, "end", end));

        assertThat(keys(result)).containsExactly(
                "post_id", "title", "created_at", "author_username", "view_count", "read_count");
        assertThat(result.columns().get(2).type()).isEqualTo(ReportColumnType.DATETIME);
        assertThat(result.rows().get(0)).containsExactly(
                5L, "Novo", LocalDateTime.of(2026, 11, 6, 9, 0), "sec", 10L, 4L);
        assertThat(result.rows().get(1).get(2)).isEqualTo(LocalDateTime.of(2026, 11, 5, 10, 30));
        assertThat(result.totalRows()).isEqualTo(2);
    }
}
