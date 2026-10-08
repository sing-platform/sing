package br.sing.psychosocial.infra;

import br.sing.psychosocial.domain.PatientRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientRecordRepository extends JpaRepository<PatientRecord, Long> {

    /**
     * Anotações de um paciente visíveis apenas para o psicólogo autor (sigilo clínico),
     * em ordem cronológica decrescente (histórico de sessões).
     */
    List<PatientRecord> findByPatient_IdAndPsychologist_User_IdAndIsActiveTrueOrderByCreatedAtDesc(
            Long patientId, Long userId);

    Optional<PatientRecord> findByIdAndIsActiveTrue(Long id);
}
