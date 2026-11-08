package br.sing.reports.application.definitions;

import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.StudentRepository;
import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.remote.FinanceClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ActiveSponsorsReport extends RemoteReport implements ReportDefinition {

    private static final List<ReportColumn> COLUMNS = List.of(
            column("id", ReportColumnType.NUMBER),
            column("sponsor_name", ReportColumnType.STRING),
            column("student_name", ReportColumnType.STRING),
            column("amount", ReportColumnType.NUMBER),
            column("frequency", ReportColumnType.STRING),
            column("start_date", ReportColumnType.DATE),
            column("last_generated_date", ReportColumnType.DATE)
    );

    private final FinanceClient financeClient;
    private final StudentRepository studentRepository;

    public ActiveSponsorsReport(FinanceClient financeClient, StudentRepository studentRepository) {
        this.financeClient = financeClient;
        this.studentRepository = studentRepository;
    }

    @Override
    public String slug() {
        return "padrinhos-ativos";
    }

    @Override
    public String displayName() {
        return "Padrinhos ativos";
    }

    @Override
    public String description() {
        return "Lista de padrinhos ativos com aluno vinculado e valor de contribuição.";
    }

    @Override
    public String category() {
        return "Financeiro";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return List.of();
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        List<FinanceClient.Sponsor> sponsors = financeClient.listSponsors().stream()
                .filter(FinanceClient.Sponsor::ativo)
                .filter(s -> s.studentId() != null)
                .toList();

        Set<Long> studentIds = sponsors.stream().map(FinanceClient.Sponsor::studentId).collect(Collectors.toSet());
        Map<Long, String> studentNames = new HashMap<>();
        for (Student student : studentRepository.findAllById(studentIds)) {
            studentNames.put(student.getId(), student.getName());
        }

        List<FinanceClient.Sponsor> linked = sponsors.stream()
                .filter(s -> studentNames.containsKey(s.studentId()))
                .sorted(Comparator.comparing(FinanceClient.Sponsor::name, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<List<Object>> rows = new ArrayList<>(linked.size());
        for (FinanceClient.Sponsor s : linked) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(s.id());
            row.add(s.name());
            row.add(studentNames.get(s.studentId()));
            row.add(s.amount());
            row.add(s.frequency());
            row.add(s.startDate());
            row.add(s.lastGeneratedDate());
            rows.add(row);
        }
        return result(COLUMNS, rows);
    }
}
