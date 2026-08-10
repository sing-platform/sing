package br.cadi.academic.infra.repositories;

import br.cadi.academic.domain.entities.DescriptiveReport;
import br.cadi.academic.domain.enums.DescriptiveReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DescriptiveReportRepository extends JpaRepository<DescriptiveReport, Long> {

    @EntityGraph(attributePaths = {"student", "schoolRoom", "schoolRoom.workshop", "schoolRoom.teacher", "author"})
    @Query("SELECT r FROM DescriptiveReport r WHERE r.id = :id")
    Optional<DescriptiveReport> findByIdWithDetails(@Param("id") Long id);

    boolean existsByStudent_IdAndSchoolRoom_Id(Long studentId, Long schoolRoomId);

    @EntityGraph(attributePaths = {"student", "schoolRoom", "schoolRoom.workshop", "schoolRoom.teacher", "author"})
    @Query("""
            SELECT r FROM DescriptiveReport r
            WHERE (:schoolRoomId IS NULL OR r.schoolRoom.id = :schoolRoomId)
              AND (:studentId IS NULL OR r.student.id = :studentId)
              AND (:status IS NULL OR r.status = :status)
              AND (:workshopId IS NULL OR r.schoolRoom.workshop.id = :workshopId)
            ORDER BY r.updatedAt DESC
            """)
    List<DescriptiveReport> findAllWithFilters(
            @Param("schoolRoomId") Long schoolRoomId,
            @Param("studentId") Long studentId,
            @Param("status") DescriptiveReportStatus status,
            @Param("workshopId") Long workshopId
    );

    @EntityGraph(attributePaths = {"student", "schoolRoom", "schoolRoom.workshop", "schoolRoom.teacher", "author"})
    @Query("""
            SELECT r FROM DescriptiveReport r
            WHERE (:schoolRoomId IS NULL OR r.schoolRoom.id = :schoolRoomId)
              AND (:studentId IS NULL OR r.student.id = :studentId)
              AND (:status IS NULL OR r.status = :status)
              AND (:workshopId IS NULL OR r.schoolRoom.workshop.id = :workshopId)
              AND r.schoolRoom.teacher.id = :teacherId
            ORDER BY r.updatedAt DESC
            """)
    List<DescriptiveReport> findAllWithFiltersForTeacher(
            @Param("teacherId") Long teacherId,
            @Param("schoolRoomId") Long schoolRoomId,
            @Param("studentId") Long studentId,
            @Param("status") DescriptiveReportStatus status,
            @Param("workshopId") Long workshopId
    );
}
