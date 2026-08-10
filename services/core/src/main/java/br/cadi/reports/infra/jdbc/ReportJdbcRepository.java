package br.cadi.reports.infra.jdbc;

import br.cadi.reports.domain.ReportColumn;
import br.cadi.reports.domain.ReportColumnType;
import br.cadi.reports.domain.ReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ReportJdbcRepository {

    public static final int MAX_ROWS = 50_000;

    private final NamedParameterJdbcTemplate jdbc;

    public ReportResult query(String sql, Map<String, ?> params, List<ReportColumn> columns) {
        List<List<Object>> rows = jdbc.query(sql, params, (rs, rowNum) -> mapRow(rs, columns.size()));
        if (rows.size() > MAX_ROWS) {
            rows = rows.subList(0, MAX_ROWS);
        }
        return new ReportResult(columns, rows, Map.of("truncated", rows.size() >= MAX_ROWS), rows.size());
    }

    public ReportResult queryAutoColumns(String sql, Map<String, ?> params) {
        return jdbc.query(sql, params, rs -> {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            List<ReportColumn> columns = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                String label = meta.getColumnLabel(i);
                columns.add(new ReportColumn(toKey(label), label, guessType(meta.getColumnType(i))));
            }
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next() && rows.size() < MAX_ROWS) {
                rows.add(mapRow(rs, count));
            }
            return new ReportResult(columns, rows, Map.of(), rows.size());
        });
    }

    private List<Object> mapRow(ResultSet rs, int columnCount) throws SQLException {
        List<Object> row = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            Object value = rs.getObject(i);
            if (value instanceof Timestamp ts) {
                value = ts.toLocalDateTime();
            }
            row.add(value);
        }
        return row;
    }

    private static String toKey(String label) {
        return label == null ? "col" : label.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
    }

    private static ReportColumnType guessType(int sqlType) {
        return switch (sqlType) {
            case java.sql.Types.INTEGER, java.sql.Types.BIGINT, java.sql.Types.SMALLINT,
                 java.sql.Types.TINYINT, java.sql.Types.FLOAT, java.sql.Types.DOUBLE,
                 java.sql.Types.NUMERIC, java.sql.Types.DECIMAL -> ReportColumnType.NUMBER;
            case java.sql.Types.DATE -> ReportColumnType.DATE;
            case java.sql.Types.TIMESTAMP, java.sql.Types.TIMESTAMP_WITH_TIMEZONE -> ReportColumnType.DATETIME;
            case java.sql.Types.BOOLEAN, java.sql.Types.BIT -> ReportColumnType.BOOLEAN;
            default -> ReportColumnType.STRING;
        };
    }

    public static Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(value.toString());
    }

    public static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate d) return d;
        if (value instanceof LocalDateTime dt) return dt.toLocalDate();
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}
