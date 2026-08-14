package br.cadi.academic.application;

import br.cadi.academic.domain.entities.Attendance;
import br.cadi.academic.domain.enums.AttendanceStatus;
import br.cadi.academic.infra.repositories.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aplica no livro de chamada o efeito das decisoes tomadas sobre atestados.
 *
 * <p>Antes da extracao, quem fazia isso era o {@code AtestadoApprovalService} do
 * modulo document, escrevendo direto em {@code attendance}. Agora a decisao vem
 * do servico de documentos como evento, e a escrita acontece aqui — no dono da
 * tabela.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtestadoJustificationService {

    private final AttendanceRepository attendanceRepository;

    /** Marca como JUSTIFIED as faltas do aluno dentro do periodo do atestado. */
    @Transactional
    public int applyJustifications(Long documentId, Long studentPersonId,
                                   LocalDate start, LocalDate end) {
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        List<Attendance> absences =
                attendanceRepository.findAbsencesByStudentAndPeriod(studentPersonId, from, to);

        for (Attendance a : absences) {
            a.applyStatus(AttendanceStatus.JUSTIFIED);
            a.setJustifiedByDocumentId(documentId);
            a.setObservation("Justificado pelo atestado #" + documentId);
        }
        attendanceRepository.saveAll(absences);
        log.info("Atestado {} justificou {} falta(s) do aluno {}", documentId, absences.size(), studentPersonId);
        return absences.size();
    }

    /** Desfaz as justificativas aplicadas por um atestado (rejeitado, excluido ou reaprovado). */
    @Transactional
    public int revertJustifications(Long documentId) {
        List<Attendance> justified = attendanceRepository.findByJustifiedByDocumentId(documentId);
        for (Attendance a : justified) {
            a.applyStatus(AttendanceStatus.ABSENT);
            a.setJustifiedByDocumentId(null);
            a.setObservation(null);
        }
        attendanceRepository.saveAll(justified);
        log.info("Atestado {} teve {} justificativa(s) revertida(s)", documentId, justified.size());
        return justified.size();
    }
}
