package br.cadi.academic.infra.repositories;

import br.cadi.academic.domain.entities.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findBySchoolroomIdAndStatusTrue(Long schoolroomId);

    boolean existsBySchoolroomIdAndStatusTrue(Long schoolroomId);

    boolean existsByStudentIdAndSchoolroomIdAndStatusTrue(Long studentId, Long schoolroomId);

    long countByStudentIdAndStatusTrue(Long studentId);

    long countBySchoolroomIdAndStatusTrue(Long schoolroomId);

    @Query("SELECT e.schoolroom.id, COUNT(e) FROM Enrollment e WHERE e.status = true AND e.schoolroom.id IN :roomIds GROUP BY e.schoolroom.id")
    List<Object[]> countActiveEnrollmentsGroupedBySchoolRoomId(@Param("roomIds") Collection<Long> roomIds);

    Optional<Enrollment> findByStudentIdAndSchoolroomIdAndStatusTrue(Long studentId, Long schoolroomId);

    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.schoolroom sr " +
            "LEFT JOIN FETCH sr.teacher t " +
            "WHERE e.student.id = :studentId AND e.status = true")
    List<Enrollment> findActiveEnrollmentsWithDetails(@Param("studentId") Long studentId);
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.schoolroom sr JOIN FETCH sr.workshop WHERE e.student.id = :studentId AND e.status = true")
    List<Enrollment> findActiveByStudentWithRoomAndWorkshop(@Param("studentId") Long studentId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s WHERE e.schoolroom.id = :schoolRoomId AND e.status = true")
    List<Enrollment> findActiveBySchoolroomIdWithStudent(@Param("schoolRoomId") Long schoolRoomId);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student s WHERE e.status = true AND e.schoolroom.id IN :roomIds")
    List<Enrollment> findActiveBySchoolRoomIdsWithStudent(@Param("roomIds") Collection<Long> roomIds);

    @EntityGraph(attributePaths = {"student", "schoolroom", "schoolroom.workshop", "schoolroom.teacher"})
    @Query("SELECT e FROM Enrollment e WHERE e.schoolroom.id = :schoolRoomId AND (:status IS NULL OR e.status = :status)")
    Page<Enrollment> pageBySchoolRoomIdAndStatusOptional(
            @Param("schoolRoomId") Long schoolRoomId,
            @Param("status") Boolean status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"student", "schoolroom", "schoolroom.workshop", "schoolroom.teacher"})
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND (:status IS NULL OR e.status = :status)")
    Page<Enrollment> pageByStudentIdAndStatusOptional(
            @Param("studentId") Long studentId,
            @Param("status") Boolean status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"student", "schoolroom", "schoolroom.workshop", "schoolroom.teacher"})
    @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.schoolroom.teacher.id = :teacherId AND (:status IS NULL OR e.status = :status)")
    Page<Enrollment> pageByStudentIdAndTeacherIdAndStatusOptional(
            @Param("studentId") Long studentId,
            @Param("teacherId") Long teacherId,
            @Param("status") Boolean status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"student", "schoolroom", "schoolroom.workshop", "schoolroom.teacher"})
    @Query("SELECT e FROM Enrollment e WHERE e.schoolroom.teacher.id = :teacherId AND (:status IS NULL OR e.status = :status)")
    Page<Enrollment> pageByTeacherIdAndStatusOptional(
            @Param("teacherId") Long teacherId,
            @Param("status") Boolean status,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                student_id    AS studentId,
                student_name  AS studentName,
                student_birth_date AS studentBirthDate,
                guardian_name AS guardianName,
                school_room_id    AS schoolRoomId,
                school_room_name  AS schoolRoomName,
                workshop_name     AS workshopName,
                period            AS period,
                teacher_name      AS teacherName,
                enrollment_date   AS enrollmentDate
            FROM vw_enrollment_declaration
            WHERE student_id = :studentId
              AND school_room_id = :schoolRoomId
            """, nativeQuery = true)
    Optional<DeclaracaoMatriculaRaw> findDeclaracaoData(
            @Param("studentId") Long studentId,
            @Param("schoolRoomId") Long schoolRoomId
    );

    interface DeclaracaoMatriculaRaw {
        Long getStudentId();
        String getStudentName();
        java.sql.Date getStudentBirthDate();
        String getGuardianName();
        Long getSchoolRoomId();
        String getSchoolRoomName();
        String getWorkshopName();
        String getPeriod(); // formatted schedule phrase: durante {dias}, das HH:mm até as HH:mm
        String getTeacherName();
        java.sql.Date getEnrollmentDate();
    }

}