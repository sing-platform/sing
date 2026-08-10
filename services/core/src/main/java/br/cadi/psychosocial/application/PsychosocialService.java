package br.cadi.psychosocial.application;

import br.cadi.common.domain.AuditableSoftDeleteEntity;
import br.cadi.exception.ForbiddenException;
import br.cadi.exception.ResourceNotFoundException;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.UserRepository;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.domain.enums.CollaboratorType;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import br.cadi.people.infra.repositories.StudentRepository;
import br.cadi.psychosocial.api.dto.request.PatientRecordRequest;
import br.cadi.psychosocial.api.dto.response.PatientClinicalResponse;
import br.cadi.psychosocial.api.dto.response.PatientRecordResponse;
import br.cadi.psychosocial.api.dto.response.PatientSummaryResponse;
import br.cadi.psychosocial.api.dto.response.StudentSearchResponse;
import br.cadi.psychosocial.domain.PatientAssignment;
import br.cadi.psychosocial.domain.PatientRecord;
import br.cadi.psychosocial.infra.PatientAssignmentRepository;
import br.cadi.psychosocial.infra.PatientRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Regras do módulo psicossocial: vínculo psicólogo↔paciente, leitura de dados
 * clínicos e anotações terapêuticas. Toda operação valida que o psicólogo
 * autenticado está vinculado ao paciente; anotações são privadas do autor.
 */
@Service
@RequiredArgsConstructor
public class PsychosocialService {

    private final PatientAssignmentRepository assignmentRepository;
    private final PatientRecordRepository recordRepository;
    private final StudentRepository studentRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final UserRepository userRepository;

    // ── Pacientes / vínculo ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PatientSummaryResponse> listMyPatients(Long userId) {
        return assignmentRepository.findActiveByPsychologistUserId(userId).stream()
                .map(a -> PatientSummaryResponse.from(a.getPatient()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResponse> searchStudents(Long userId, String query) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        Set<Long> linkedIds = assignmentRepository.findActiveByPsychologistUserId(userId).stream()
                .map(a -> a.getPatient().getId())
                .collect(Collectors.toSet());
        return studentRepository.searchByName(q).stream()
                .map(s -> StudentSearchResponse.from(s, linkedIds.contains(s.getId())))
                .toList();
    }

    @Transactional
    public PatientSummaryResponse linkPatient(Long userId, Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        if (assignmentRepository.existsByPsychologist_User_IdAndPatient_IdAndIsActiveTrue(userId, studentId)) {
            return PatientSummaryResponse.from(student); // idempotente
        }
        PatientAssignment assignment = PatientAssignment.builder()
                .psychologist(resolvePsychologist(userId))
                .patient(student)
                .createdBy(userReference(userId))
                .isActive(true)
                .build();
        assignmentRepository.save(assignment);
        return PatientSummaryResponse.from(student);
    }

    @Transactional
    public void unlinkPatient(Long userId, Long studentId) {
        PatientAssignment assignment = assignmentRepository
                .findByPsychologist_User_IdAndPatient_IdAndIsActiveTrue(userId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientAssignment", studentId));
        softDelete(assignment, userId);
        assignmentRepository.save(assignment);
    }

    // ── Dados clínicos (somente leitura) ────────────────────────────────────

    @Transactional(readOnly = true)
    public PatientClinicalResponse getPatientClinical(Long userId, Long patientId) {
        assertLinked(userId, patientId);
        Student student = studentRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", patientId));
        return PatientClinicalResponse.from(student);
    }

    // ── Anotações terapêuticas (privadas do autor) ──────────────────────────

    @Transactional(readOnly = true)
    public List<PatientRecordResponse> listRecords(Long userId, Long patientId) {
        assertLinked(userId, patientId);
        return recordRepository
                .findByPatient_IdAndPsychologist_User_IdAndIsActiveTrueOrderByCreatedAtDesc(patientId, userId)
                .stream()
                .map(PatientRecordResponse::from)
                .toList();
    }

    @Transactional
    public PatientRecordResponse addRecord(Long userId, Long patientId, PatientRecordRequest request) {
        assertLinked(userId, patientId);
        Student patient = studentRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", patientId));
        PatientRecord record = PatientRecord.builder()
                .notes(request.notes())
                .confidentialCode(generateConfidentialCode())
                .patient(patient)
                .psychologist(resolvePsychologist(userId))
                .createdBy(userReference(userId))
                .isActive(true)
                .build();
        recordRepository.save(record);
        return PatientRecordResponse.from(record);
    }

    @Transactional
    public PatientRecordResponse updateRecord(Long userId, Long recordId, PatientRecordRequest request) {
        PatientRecord record = recordRepository.findByIdAndIsActiveTrue(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientRecord", recordId));
        assertAuthor(userId, record);
        record.setNotes(request.notes());
        recordRepository.save(record);
        return PatientRecordResponse.from(record);
    }

    @Transactional
    public void deleteRecord(Long userId, Long recordId) {
        PatientRecord record = recordRepository.findByIdAndIsActiveTrue(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("PatientRecord", recordId));
        assertAuthor(userId, record);
        softDelete(record, userId);
        recordRepository.save(record);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void assertLinked(Long userId, Long patientId) {
        if (!assignmentRepository.existsByPsychologist_User_IdAndPatient_IdAndIsActiveTrue(userId, patientId)) {
            throw new ForbiddenException("Paciente não vinculado ao psicólogo autenticado");
        }
    }

    private void assertAuthor(Long userId, PatientRecord record) {
        Long authorUserId = record.getPsychologist() != null && record.getPsychologist().getUser() != null
                ? record.getPsychologist().getUser().getId()
                : null;
        if (!userId.equals(authorUserId)) {
            throw new ForbiddenException("Anotação pertence a outro psicólogo");
        }
    }

    private Collaborator resolvePsychologist(Long userId) {
        Collaborator collaborator = collaboratorRepository.findByUserId(userId)
                .orElseThrow(() -> new ForbiddenException("Usuário não é um colaborador"));
        if (collaborator.getType() != CollaboratorType.PSYCHOLOGIST) {
            throw new ForbiddenException("Usuário não é um psicólogo");
        }
        return collaborator;
    }

    private User userReference(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private String generateConfidentialCode() {
        return "PR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void softDelete(AuditableSoftDeleteEntity entity, Long userId) {
        entity.setActive(false);
        entity.setDeactivatedAt(LocalDateTime.now());
        entity.setDeactivatedBy(userReference(userId));
    }
}
