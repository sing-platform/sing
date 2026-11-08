package br.sing.reports.application.definitions;

import br.sing.reports.application.ReportDefinition;
import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportColumnType;
import br.sing.reports.domain.ReportFilterSpec;
import br.sing.reports.domain.ReportResult;
import br.sing.reports.infra.remote.CommunicationClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MuralEngagementReport extends RemoteReport implements ReportDefinition {

    private static final List<ReportColumn> COLUMNS = List.of(
            column("post_id", ReportColumnType.NUMBER),
            column("title", ReportColumnType.STRING),
            column("created_at", ReportColumnType.DATETIME),
            column("author_username", ReportColumnType.STRING),
            column("view_count", ReportColumnType.NUMBER),
            column("read_count", ReportColumnType.NUMBER)
    );

    private final CommunicationClient communicationClient;

    public MuralEngagementReport(CommunicationClient communicationClient) {
        this.communicationClient = communicationClient;
    }

    @Override
    public String slug() {
        return "engajamento-mural";
    }

    @Override
    public String displayName() {
        return "Engajamento do mural";
    }

    @Override
    public String description() {
        return "Posts publicados e visualizações no período.";
    }

    @Override
    public String category() {
        return "Operacional";
    }

    @Override
    public List<ReportFilterSpec> filters() {
        return periodFilters();
    }

    @Override
    public ReportResult execute(Map<String, Object> filterValues) {
        LocalDate start = (LocalDate) filterValues.get("start");
        LocalDate end = (LocalDate) filterValues.get("end");

        List<CommunicationClient.MuralEngagement> engagement = communicationClient.muralEngagement(start, end);

        List<List<Object>> rows = new ArrayList<>(engagement.size());
        for (CommunicationClient.MuralEngagement e : engagement) {
            List<Object> row = new ArrayList<>(COLUMNS.size());
            row.add(e.postId());
            row.add(e.title());
            row.add(parseCreatedAt(e.createdAt()));
            row.add(e.authorUsername());
            row.add(e.viewCount());
            row.add(e.readCount());
            rows.add(row);
        }
        return result(COLUMNS, rows);
    }

    private static LocalDateTime parseCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        }
    }
}
