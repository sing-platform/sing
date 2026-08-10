package br.cadi.reports.infra.export;

import br.cadi.reports.domain.ReportColumn;
import br.cadi.reports.domain.ReportResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

public final class XlsxExporter {

    private XlsxExporter() {
    }

    public static byte[] export(ReportResult result) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Relatorio");
            List<ReportColumn> columns = result.columns();
            Row header = sheet.createRow(0);
            for (int c = 0; c < columns.size(); c++) {
                header.createCell(c).setCellValue(columns.get(c).label());
            }
            int rowIdx = 1;
            for (List<Object> rowData : result.rows()) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < rowData.size(); c++) {
                    setCell(row.createCell(c), rowData.get(c));
                }
            }
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar XLSX", e);
        }
    }

    private static void setCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            return;
        }
        if (value instanceof Boolean b) {
            cell.setCellValue(b);
            return;
        }
        if (value instanceof LocalDate d) {
            cell.setCellValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            return;
        }
        if (value instanceof LocalDateTime dt) {
            cell.setCellValue(Date.from(dt.atZone(ZoneId.systemDefault()).toInstant()));
            return;
        }
        cell.setCellValue(value.toString());
    }
}
