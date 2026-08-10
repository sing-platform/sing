package br.cadi.people.infra.repositories;

import br.cadi.people.domain.entities.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    @Query("SELECT DISTINCT s FROM Student s " +
            "LEFT JOIN FETCH s.address " +
            "LEFT JOIN FETCH s.user " +
            "LEFT JOIN FETCH s.guardians sg " +
            "LEFT JOIN FETCH sg.guardian")
    List<Student> findAllWithDetails();

    @Query(value = """
            SELECT s.id FROM Student s
            WHERE (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:periodStudies IS NULL OR LOWER(COALESCE(s.periodStudies, '')) LIKE LOWER(CONCAT('%', :periodStudies, '%')))
            AND (:enrollmentFilter = 'ANY'
              OR (:enrollmentFilter = 'NONE' AND (SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = s.id AND e.status = true) = 0)
              OR (:enrollmentFilter = 'ONE' AND (SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = s.id AND e.status = true) = 1)
              OR (:enrollmentFilter = 'TWO' AND (SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = s.id AND e.status = true) = 2))
            """,
            countQuery = """
            SELECT COUNT(s.id) FROM Student s
            WHERE (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            AND (:periodStudies IS NULL OR LOWER(COALESCE(s.periodStudies, '')) LIKE LOWER(CONCAT('%', :periodStudies, '%')))
            AND (:enrollmentFilter = 'ANY'
              OR (:enrollmentFilter = 'NONE' AND (SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = s.id AND e.status = true) = 0)
              OR (:enrollmentFilter = 'ONE' AND (SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = s.id AND e.status = true) = 1)
              OR (:enrollmentFilter = 'TWO' AND (SELECT COUNT(e) FROM Enrollment e WHERE e.student.id = s.id AND e.status = true) = 2))
            """
    )
    Page<Long> pageIdsForEnrollmentList(
            @Param("search") String search,
            @Param("periodStudies") String periodStudies,
            @Param("enrollmentFilter") String enrollmentFilter,
            Pageable pageable
    );

    @Query("SELECT DISTINCT s FROM Student s "
            + "LEFT JOIN FETCH s.address "
            + "LEFT JOIN FETCH s.user "
            + "LEFT JOIN FETCH s.guardians sg "
            + "LEFT JOIN FETCH sg.guardian "
            + "WHERE s.id IN :ids")
    List<Student> findAllWithDetailsByIds(@Param("ids") List<Long> ids);

    @Query("SELECT s FROM Student s "
            + "WHERE :search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "ORDER BY s.name ASC")
    List<Student> searchByName(@Param("search") String search);
}