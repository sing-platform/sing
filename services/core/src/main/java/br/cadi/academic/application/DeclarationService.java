package br.cadi.academic.application;

import br.cadi.academic.api.dto.response.DeclarationResponse;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.EnrollmentRepository.DeclaracaoMatriculaRaw;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DeclarationService {

    private final EnrollmentRepository enrollmentRepository;

    private static final Locale PT_BR = new Locale("pt", "BR");

    // ── Cores (RGB 0-1) ──────────────────────────────────────────────────────
    private static final float[] COR_PRIMARIA   = {0.118f, 0.251f, 0.471f}; // azul CADI
    private static final float[] COR_TEXTO      = {0.118f, 0.118f, 0.196f};
    private static final float[] COR_MUTED      = {0.392f, 0.431f, 0.510f};
    private static final float[] COR_BOX_BG     = {0.941f, 0.957f, 1.000f};
    private static final float[] COR_BOX_BORDER = {0.816f, 0.847f, 0.918f};

    // ── Geometria A4 ─────────────────────────────────────────────────────────
    private static final float PG_W   = PDRectangle.A4.getWidth();   // 595 pts
    private static final float PG_H   = PDRectangle.A4.getHeight();  // 842 pts
    private static final float MARGIN = 50f;
    private static final float COL_W  = PG_W - 2 * MARGIN;

    // =========================================================================
    // Entrada principal
    // =========================================================================

    public byte[] gerarDeclaracao(Long studentId, Long schoolRoomId) throws IOException {
        DeclaracaoMatriculaRaw raw = enrollmentRepository
                .findDeclaracaoData(studentId, schoolRoomId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Matrícula ativa não encontrada para o aluno na turma informada."));

        DeclarationResponse dto = new DeclarationResponse(
                raw.getStudentId(),
                raw.getStudentName(),
                raw.getStudentBirthDate() != null ? raw.getStudentBirthDate().toLocalDate() : null,
                raw.getGuardianName(),
                raw.getSchoolRoomId(),
                raw.getSchoolRoomName(),
                raw.getWorkshopName(),
                raw.getPeriod(),
                raw.getTeacherName(),
                raw.getEnrollmentDate() != null ? raw.getEnrollmentDate().toLocalDate() : null,
                resolveSecretaryName()
        );

        return buildPdf(dto);
    }

    // =========================================================================
    // Construção do PDF
    // =========================================================================

    private byte[] buildPdf(DeclarationResponse dto) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont italic  = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            // Tenta carregar a logo do classpath (src/main/resources/static/imgs/cadi.png)
            PDImageXObject logo = tryLoadLogo(doc);

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, PDPageContentStream.AppendMode.OVERWRITE, true, true)) {

                float y = PG_H - MARGIN;

                y = drawHeader(cs, doc, bold, regular, logo, y);
                y = drawDivider(cs, y - 15);
                y = drawCenteredText(cs, bold,   13, COR_PRIMARIA, "DECLARAÇÃO DE MATRÍCULA", y - 35);
                y = drawCenteredText(cs, italic,  8, COR_MUTED,
                        "Documento emitido eletronicamente pelo sistema CADI", y - 8);
                y = drawBody(cs, regular, bold, dto, y - 40);
                drawFooter(cs, regular, bold, dto, y - 100);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // =========================================================================
    // Cabeçalho
    // =========================================================================

    private float drawHeader(PDPageContentStream cs, PDDocument doc,
                             PDFont bold, PDFont regular,
                             PDImageXObject logo, float y) throws IOException {

        float logoSize = 60f;
        float logoX    = MARGIN;
        float logoY    = y - logoSize;

        if (logo != null) {
            // Desenha a imagem real da logo
            cs.drawImage(logo, logoX, logoY, logoSize, logoSize);
        } else {
            // Fallback: box cinza com texto
            setColor(cs, COR_BOX_BORDER, false);
            cs.addRect(logoX, logoY, logoSize, logoSize);
            cs.stroke();
            drawTextAt(cs, bold, 9, COR_MUTED, "LOGO", logoX + 20, logoY + 38);
            drawTextAt(cs, bold, 9, COR_MUTED, "CADI", logoX + 20, logoY + 25);
        }

        // Dados institucionais
        float infoX = logoX + logoSize + 16;
        float lineY = y - 14;

        drawTextAt(cs, bold, 11, COR_PRIMARIA,
                "CADI \u2013 Centro de Assist\u00eancia e Desenvolvimento Integral",
                infoX, lineY);
        lineY -= 15;
        drawTextAt(cs, regular, 9, COR_TEXTO,
                "Endere\u00e7o: Av. Holanda, 155 \u2013 Na\u00e7\u00f5es  |  CEP: 83.823-208",
                infoX, lineY);
        lineY -= 13;
        drawTextAt(cs, regular, 9, COR_TEXTO,
                "Munic\u00edpio de Fazenda Rio Grande / PR",
                infoX, lineY);
        lineY -= 13;
        drawTextAt(cs, regular, 9, COR_TEXTO,
                "Telefone: (41) 99792-0034   |   E-mail: contato@cadi.org.br",
                infoX, lineY);

        return logoY - 6;
    }

    // =========================================================================
    // Corpo
    // =========================================================================

    private float drawBody(PDPageContentStream cs, PDFont regular, PDFont bold,
                           DeclarationResponse dto, float y) throws IOException {

        String birthDate = dto.studentBirthDate() != null
                ? format(dto.studentBirthDate())
                : "data n\u00e3o informada";

        String guardian = nonBlank(dto.guardianName(), "respons\u00e1vel n\u00e3o informado");
        String workshop = nonBlank(dto.workshopName(), "oficina n\u00e3o informada");
        String roomName = nonBlank(dto.schoolRoomName(), "turma n\u00e3o informada");
        String period   = translatePeriod(nonBlank(dto.period(), ""));

        List<Object[]> segments = new ArrayList<>();
        seg(segments, "Declaro, para os devidos fins, que o(a) aluno(a) ", false);
        seg(segments, dto.studentName().toUpperCase(), true);
        seg(segments, ", nascido(a) em ", false);
        seg(segments, birthDate, true);
        seg(segments, ", filho(a) de ", false);
        seg(segments, guardian, true);
        seg(segments, ", encontra-se regularmente matriculado(a) neste estabelecimento "
                + "at\u00e9 a presente data, dentro da oficina ", false);
        seg(segments, workshop, true);
        seg(segments, ", turma ", false);
        seg(segments, roomName, true);
        seg(segments, ", ", false);
        seg(segments, period, false);
        seg(segments, ".", false);

        return drawJustifiedMixed(cs, regular, bold, 11, COR_TEXTO, segments,
                MARGIN, y, COL_W, 18);
    }


    // =========================================================================
    // Rodapé
    // =========================================================================

    private void drawFooter(PDPageContentStream cs, PDFont regular, PDFont bold,
                            DeclarationResponse dto, float y) throws IOException {

        LocalDate hoje   = LocalDate.now();
        String mes       = capitalizeFirst(hoje.getMonth().getDisplayName(TextStyle.FULL, PT_BR));
        String dataTexto = String.format("Fazenda Rio Grande, %02d de %s de %d",
                hoje.getDayOfMonth(), mes, hoje.getYear());

        // Data — alinhada à direita
        float dataW = textWidth(regular, 10, dataTexto);
        drawTextAt(cs, regular, 10, COR_TEXTO, dataTexto, PG_W - MARGIN - dataW, y);

        y -= 32;

        // Linha de assinatura
        String underline = "_".repeat(52);
        float ulW = textWidth(regular, 10, underline);
        drawTextAt(cs, regular, 10, COR_MUTED, underline, (PG_W - ulW) / 2, y);

        y -= 14;
        String sigName = "Assinado por: " + nonBlank(dto.secretaryName(), "Secretaria CADI");
        float snW = textWidth(bold, 10, sigName);
        drawTextAt(cs, bold, 10, COR_PRIMARIA, sigName, (PG_W - snW) / 2, y);

        y -= 13;
        String role = "Secretaria \u2013 CADI";
        float roleW = textWidth(regular, 9, role);
        drawTextAt(cs, regular, 9, COR_MUTED, role, (PG_W - roleW) / 2, y);
    }

    // =========================================================================
    // Utilitários de renderização
    // =========================================================================

    private void drawTextAt(PDPageContentStream cs, PDFont font, float size,
                            float[] color, String text, float x, float y) throws IOException {
        setColor(cs, color, true);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private float drawCenteredText(PDPageContentStream cs, PDFont font, float size,
                                   float[] color, String text, float y) throws IOException {
        float w = textWidth(font, size, text);
        drawTextAt(cs, font, size, color, text, (PG_W - w) / 2, y);
        return y - size - 2;
    }

    private float drawDivider(PDPageContentStream cs, float y) throws IOException {
        setColor(cs, COR_PRIMARIA, false);
        cs.setLineWidth(1.5f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(PG_W - MARGIN, y);
        cs.stroke();
        cs.setLineWidth(1f);
        return y - 4;
    }

    /**
     * Parágrafo justificado com segmentos de fonte mista (regular + bold).
     * Retorna o Y final abaixo do último baseline.
     */
    private float drawJustifiedMixed(PDPageContentStream cs,
                                     PDFont regular, PDFont bold,
                                     float size, float[] color,
                                     List<Object[]> segments,
                                     float x, float y, float maxW,
                                     float leading) throws IOException {

        record Word(String text, boolean isBold) {}
        List<Word> words = new ArrayList<>();
        for (Object[] seg : segments) {
            for (String part : ((String) seg[0]).split("(?<=\\s)|(?=\\s)")) {
                if (!part.isEmpty()) words.add(new Word(part, (boolean) seg[1]));
            }
        }

        // Quebra em linhas
        List<List<Word>> lines = new ArrayList<>();
        List<Word> current = new ArrayList<>();
        float lineW = 0;
        for (Word w : words) {
            float wW = textWidth(w.isBold() ? bold : regular, size, w.text());
            if (lineW + wW > maxW && !current.isEmpty()) {
                lines.add(current);
                current = new ArrayList<>();
                lineW = 0;
            }
            current.add(w);
            lineW += wW;
        }
        if (!current.isEmpty()) lines.add(current);

        // Renderiza
        float curY = y;
        for (int li = 0; li < lines.size(); li++) {
            List<Word> line = lines.get(li);
            boolean isLast  = li == lines.size() - 1;

            float totalW   = (float) line.stream().mapToDouble(w -> {
                try { return textWidth(w.isBold() ? bold : regular, size, w.text()); }
                catch (IOException e) { return 0; }
            }).sum();
            long spaces    = line.stream().filter(w -> w.text().equals(" ")).count();
            float extra    = (!isLast && spaces > 0) ? (maxW - totalW) / spaces : 0;

            float cx = x;
            for (Word w : line) {
                PDFont f = w.isBold() ? bold : regular;
                setColor(cs, color, true);
                cs.beginText();
                cs.setFont(f, size);
                cs.newLineAtOffset(cx, curY);
                cs.showText(w.text());
                cs.endText();
                cx += textWidth(f, size, w.text()) + (" ".equals(w.text()) ? extra : 0);
            }
            curY -= leading;
        }
        return curY;
    }

    private float textWidth(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    private void setColor(PDPageContentStream cs, float[] rgb, boolean fill) throws IOException {
        if (fill) cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        else      cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    private void seg(List<Object[]> list, String text, boolean bold) {
        list.add(new Object[]{text, bold});
    }

    // =========================================================================
    // Logo — carregada do classpath (sem caminho absoluto)
    // =========================================================================

    private PDImageXObject tryLoadLogo(PDDocument doc) {
        String[] candidates = {
                "static/imgs/logo-cadi-cortada.png",
                "static/imgs/logo-cadi.png",
                "imgs/cadi.png",
        };
        for (String path : candidates) {
            try {
                ClassPathResource resource = new ClassPathResource(path);
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        byte[] bytes = is.readAllBytes();
                        return PDImageXObject.createFromByteArray(doc, bytes, "logo");
                    }
                }
            } catch (Exception ignored) {}
        }
        return null; // fallback: box cinza
    }

    // =========================================================================
    // Helpers gerais
    // =========================================================================

    private String resolveSecretaryName() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud)
                    return ud.getUsername();
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "Secretaria CADI";
    }

    private String translatePeriod(String period) {
        if (period == null) return "";
        return switch (period.toUpperCase()) {
            case "MANHA" -> "Manh\u00e3";
            case "TARDE" -> "Tarde";
            case "NOITE" -> "Noite";
            default      -> period;
        };
    }

    private String format(LocalDate d) {
        return String.format("%02d/%02d/%d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private String nonBlank(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }

    private String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}