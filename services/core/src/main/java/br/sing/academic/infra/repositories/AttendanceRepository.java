package br.sing.academic.infra.repositories;

import br.sing.academic.domain.entities.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByLectureId(Long lectureId);
    List<Attendance> findByStudentId(Long studentId);
    Optional<Attendance> findByLectureIdAndStudentId(Long lectureId, Long studentId);

    // Busca todas as presenças de uma turma em um período
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.lecture l
        JOIN FETCH a.student s
        WHERE l.schoolroom.id = :schoolRoomId
          AND l.dateTime >= :start
          AND l.dateTime < :end
        ORDER BY l.dateTime ASC
    """)
    List<Attendance> findBySchoolRoomAndPeriod(
            @Param("schoolRoomId") Long schoolRoomId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Busca todas as presenças de um aluno em um período
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.lecture l
        JOIN FETCH a.student s
        WHERE a.student.id = :studentId
          AND l.dateTime >= :start
          AND l.dateTime < :end
        ORDER BY l.dateTime ASC
    """)
    List<Attendance> findByStudentAndPeriod(
            @Param("studentId") Long studentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Busca presenças de um aluno em uma turma específica em um período
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.lecture l
        WHERE a.student.id = :studentId
          AND l.schoolroom.id = :schoolRoomId
          AND l.dateTime >= :start
          AND l.dateTime < :end
        ORDER BY l.dateTime ASC
    """)
    List<Attendance> findByStudentAndSchoolRoomAndPeriod(
            @Param("studentId") Long studentId,
            @Param("schoolRoomId") Long schoolRoomId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Faltas (status = ABSENT) de um aluno em um periodo - usado para aplicar justificativa por atestado
    @Query("""
        SELECT a FROM Attendance a
        JOIN FETCH a.lecture l
        WHERE a.student.id = :studentId
          AND a.status = br.sing.academic.domain.enums.AttendanceStatus.ABSENT
          AND l.dateTime >= :start
          AND l.dateTime < :end
    """)
    List<Attendance> findAbsencesByStudentAndPeriod(
            @Param("studentId") Long studentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Registros justificados por um documento especifico - usado para rollback ao excluir/re-aprovar
    List<Attendance> findByJustifiedByDocumentId(Long documentId);
}