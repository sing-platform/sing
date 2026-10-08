package br.sing.reports.infra.export;

import br.sing.reports.domain.ReportColumn;
import br.sing.reports.domain.ReportResult;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CsvExporter {

    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private CsvExporter() {
    }

    public static byte[] export(ReportResult result) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(UTF8_BOM);
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            List<ReportColumn> columns = result.columns();
            writer.write(String.join(";", columns.stream().map(ReportColumn::label).toList()));
            writer.write("\r\n");
            for (List<Object> row : result.rows()) {
                writer.write(String.join(";", row.stream().map(CsvExporter::format).toList()));
                writer.write("\r\n");
            }
            writer.flush();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar CSV", e);
        }
        return out.toByteArray();
    }

    private static String format(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDate d) {
            return d.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        String s = value.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
