package br.sing.psychosocial.infra;

import br.sing.psychosocial.domain.PatientAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientAssignmentRepository extends JpaRepository<PatientAssignment, Long> {

    @Query("SELECT a FROM PatientAssignment a "
            + "JOIN FETCH a.patient "
            + "WHERE a.psychologist.user.id = :userId AND a.isActive = true "
            + "ORDER BY a.patient.name ASC")
    List<PatientAssignment> findActiveByPsychologistUserId(@Param("userId") Long userId);

    Optional<PatientAssignment> findByPsychologist_User_IdAndPatient_IdAndIsActiveTrue(Long userId, Long patientId);

    boolean existsByPsychologist_User_IdAndPatient_IdAndIsActiveTrue(Long userId, Long patientId);
}
