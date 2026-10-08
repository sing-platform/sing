package br.sing.people.infra.repositories;

import br.sing.people.domain.entities.Guardian;
import br.sing.people.domain.entities.Student;
import br.sing.people.domain.entities.StudentGuardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface StudentGuardianRepository extends JpaRepository<StudentGuardian, Long> {

    // --- Consultas Simples ---
    StudentGuardian findByGuardian(Guardian guardian);
    List<StudentGuardian> findAllByGuardian(Guardian guardian);
    List<StudentGuardian> findAllByStudent(Student student);
    boolean existsByStudentIdAndGuardianId(Long studentId, Long guardianId);

    // --- Consultas Complexas (Árvore Familiar do "Me") ---
    @Query("SELECT sg FROM StudentGuardian sg " +
            "JOIN FETCH sg.student s " +
            "LEFT JOIN FETCH s.enrollments e " +
            "LEFT JOIN FETCH e.schoolroom sr " +
            "LEFT JOIN FETCH sr.teacher t " +
            "WHERE sg.guardian.id = :guardianId")
    List<StudentGuardian> findFamilyTreeByGuardianId(@Param("guardianId") Long guardianId);

    // --- Métodos de Exclusão (Para os Services) ---

    @Modifying
    @Transactional
    @Query("DELETE FROM StudentGuardian sg WHERE sg.guardian.id = :guardianId")
    void deleteByGuardianId(@Param("guardianId") Long guardianId);

    @Modifying
    @Transactional
    @Query("DELETE FROM StudentGuardian sg WHERE sg.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}