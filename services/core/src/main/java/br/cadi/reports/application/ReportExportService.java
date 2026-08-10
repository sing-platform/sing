package br.cadi.reports.application;

import br.cadi.reports.domain.ReportResult;
import br.cadi.reports.infra.export.CsvExporter;
import br.cadi.reports.infra.export.XlsxExporter;
import org.springframework.stereotype.Service;

@Service
public class ReportExportService {

    public enum ExportFormat {
        CSV, XLSX
    }

    public byte[] export(ReportResult result, ExportFormat format) {
        return switch (format) {
            case CSV -> CsvExporter.export(result);
            case XLSX -> XlsxExporter.export(result);
        };
    }

    public String contentType(ExportFormat format) {
        return switch (format) {
            case CSV -> "text/csv; charset=UTF-8";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }

    public String fileExtension(ExportFormat format) {
        return switch (format) {
            case CSV -> "csv";
            case XLSX -> "xlsx";
        };
    }
}
