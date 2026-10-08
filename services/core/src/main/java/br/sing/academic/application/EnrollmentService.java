package br.sing.academic.application;

import br.sing.academic.api.dto.request.EnrollmentRequest;
import br.sing.academic.api.dto.response.EnrollmentPreviewIssue;
import br.sing.academic.api.dto.response.EnrollmentPreviewResponse;
import br.sing.academic.api.dto.response.EnrollmentSuggestedAction;
import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.enums.VacancyCause;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.exception.BusinessException;
import br.sing.exception.EntityNotFoundException;
import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final SchoolRoomVacancyService schoolRoomVacancyService;

    @Transactional(readOnly = true)
    public EnrollmentPreviewResponse previewEnrollment(EnrollmentRequest request) {
        List<Long> roomIds = request.schoolRoomIds();
        if (new HashSet<>(roomIds).size() != roomIds.size()) {
            return new EnrollmentPreviewResponse(
                    false,
                    List.of(new EnrollmentPreviewIssue(
                            "DUPLICATE_ROOM_IDS",
                            "A solicitação contém IDs de turma em duplicata.",
                            HttpStatus.BAD_REQUEST.value()
                    )),
                    EnrollmentSuggestedAction.ENROLL
            );
        }
        try {
            if (roomIds.size() >= 2) {
                assertBatchPlanValid(request.studentId(), roomIds);
            } else {
                validateSingleEnrollmentPlan(request.studentId(), roomIds.getFirst());
            }
        } catch (BusinessException e) {
            return new EnrollmentPreviewResponse(
                    false,
                    List.of(toIssue(e)),
                    EnrollmentSuggestedAction.ENROLL
            );
        } catch (EntityNotFoundException e) {
            return new EnrollmentPreviewResponse(
                    false,
                    List.of(new EnrollmentPreviewIssue("NOT_FOUND", e.getMessage(), HttpStatus.NOT_FOUND.value())),
                    EnrollmentSuggestedAction.ENROLL
            );
        }
        return new EnrollmentPreviewResponse(
                true,
                List.of(),
                resolveSuggestedAction(request.studentId(), roomIds)
        );
    }

    private static EnrollmentPreviewIssue toIssue(BusinessException e) {
        String code = switch (e.getStatus()) {
            case CONFLICT -> "CONFLICT";
            case BAD_REQUEST -> "BAD_REQUEST";
            default -> "BUSINESS_ERROR";
        };
        return new EnrollmentPreviewIssue(code, e.getMessage(), e.getStatus().value());
    }

    private EnrollmentSuggestedAction resolveSuggestedAction(Long studentId, List<Long> roomIds) {
        List<Enrollment> active = enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(studentId);
        boolean anyTransfer = false;
        boolean anyPureEnroll = false;
        for (Long roomId : roomIds) {
            SchoolRoom room = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(roomId).orElse(null);
            if (room == null) {
                continue;
            }
            boolean transfer = active.stream()
                    .anyMatch(e -> Boolean.TRUE.equals(e.getStatus())
                            && e.getSchoolroom().getWorkshop().getId().equals(room.getWorkshop().getId())
                            && !e.getSchoolroom().getId().equals(room.getId()));
            if (transfer) {
                anyTransfer = true;
            } else {
                anyPureEnroll = true;
            }
        }
        if (anyTransfer && anyPureEnroll) {
            return EnrollmentSuggestedAction.MIXED;
        }
        if (anyTransfer) {
            return EnrollmentSuggestedAction.TRANSFER;
        }
        return EnrollmentSuggestedAction.ENROLL;
    }

    @Transactional
    public void enrollStudent(EnrollmentRequest request) {
        List<Long> roomIds = request.schoolRoomIds();
        if (new HashSet<>(roomIds).size() != roomIds.size()) {
            throw new BusinessException(
                    "A solicitação contém IDs de turma em duplicata.",
                    HttpStatus.BAD_REQUEST
            );
        }
        if (roomIds.size() >= 2) {
            assertBatchPlanValid(request.studentId(), roomIds);
        }
        for (Long roomId : roomIds) {
            persistSingleEnrollment(request.studentId(), roomId);
        }
    }

    private void persistSingleEnrollment(Long studentId, Long schoolRoomId) {
        validateSingleEnrollmentPlan(studentId, schoolRoomId);

        var student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));

        var room = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));

        List<Enrollment> active = enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(studentId);

        List<Enrollment> sameWorkshopOthers = active.stream()
                .filter(e -> e.getSchoolroom().getWorkshop().getId().equals(room.getWorkshop().getId()))
                .filter(e -> !e.getSchoolroom().getId().equals(room.getId()))
                .toList();

        for (Enrollment e : sameWorkshopOthers) {
            SchoolRoom oldRoom = e.getSchoolroom();
            long countBefore = enrollmentRepository.countBySchoolroomIdAndStatusTrue(oldRoom.getId());
            e.setStatus(false);
            enrollmentRepository.save(e);
            schoolRoomVacancyService.notifyIfVacancyOpened(
                    oldRoom.getId(), countBefore, countBefore - 1, VacancyCause.TRANSFER_OUT);
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .schoolroom(room)
                .enrollmentDate(LocalDate.now())
                .status(true)
                .build();

        enrollmentRepository.save(enrollment);
    }

    /**
     * Valida matrícula em uma turma sem efeitos colaterais (sem gravar).
     * Replica as checagens de {@link #persistSingleEnrollment} antes das mutações.
     */
    private void validateSingleEnrollmentPlan(Long studentId, Long schoolRoomId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));

        SchoolRoom room = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));

        if (!Boolean.TRUE.equals(room.isActive())) {
            throw new BusinessException("Não é possível matricular em uma turma desativada.", HttpStatus.BAD_REQUEST);
        }

        if (enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(studentId, schoolRoomId)) {
            throw new BusinessException("O aluno já possui uma matrícula ativa nesta turma.", HttpStatus.CONFLICT);
        }

        EnrollmentRules.assertPeriodStudiesAllow(student, room);

        List<Enrollment> active = enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(studentId);

        List<Enrollment> virtualStillActive = active.stream()
                .filter(e -> Boolean.TRUE.equals(e.getStatus()))
                .filter(e -> !(e.getSchoolroom().getWorkshop().getId().equals(room.getWorkshop().getId())
                        && !e.getSchoolroom().getId().equals(room.getId())))
                .toList();

        if (virtualStillActive.size() >= EnrollmentRules.maxActiveEnrollments()) {
            throw new BusinessException(
                    "O aluno já está matriculado em duas turmas.",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<SchoolRoom> othersWithSlots = new ArrayList<>();
        for (Enrollment e : virtualStillActive) {
            SchoolRoom sr = schoolRoomRepository
                    .findByIdWithScheduleSlotsAndTeacher(e.getSchoolroom().getId())
                    .orElse(e.getSchoolroom());
            othersWithSlots.add(sr);
        }

        String conflictName = EnrollmentRules.conflictingRoomName(room, othersWithSlots);
        if (conflictName != null) {
            throw new BusinessException(
                    "A turma escolhida conflita com o horário da turma \"" + conflictName + "\" do aluno.",
                    HttpStatus.BAD_REQUEST
            );
        }

        long currentTotal = enrollmentRepository.countBySchoolroomIdAndStatusTrue(schoolRoomId);
        if (room.getCapacity() != null && currentTotal >= room.getCapacity()) {
            throw new BusinessException("A turma já atingiu sua capacidade máxima.", HttpStatus.BAD_REQUEST);
        }
    }

    private void assertBatchPlanValid(Long studentId, List<Long> schoolRoomIds) {
        var student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado"));

        List<Enrollment> initial = enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(studentId);
        Map<Long, Long> virtualRoomToWorkshop = new LinkedHashMap<>();
        for (Enrollment e : initial) {
            if (!Boolean.TRUE.equals(e.getStatus())) {
                continue;
            }
            SchoolRoom sr = e.getSchoolroom();
            virtualRoomToWorkshop.put(sr.getId(), sr.getWorkshop().getId());
        }

        for (Long roomId : schoolRoomIds) {
            var room = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(roomId)
                    .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));

            if (!Boolean.TRUE.equals(room.isActive())) {
                throw new BusinessException(
                        "Matrícula em lote inviável: não é possível matricular em uma turma desativada (\""
                                + room.getName() + "\").",
                        HttpStatus.BAD_REQUEST
                );
            }

            if (enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(studentId, roomId)) {
                throw new BusinessException(
                        "Matrícula em lote inviável: o aluno já possui matrícula ativa na turma \""
                                + room.getName() + "\".",
                        HttpStatus.CONFLICT
                );
            }

            EnrollmentRules.assertPeriodStudiesAllow(student, room);

            Long wId = room.getWorkshop().getId();
            virtualRoomToWorkshop.entrySet().removeIf(
                    entry -> wId.equals(entry.getValue()) && !room.getId().equals(entry.getKey())
            );

            if (virtualRoomToWorkshop.size() >= EnrollmentRules.maxActiveEnrollments()) {
                throw new BusinessException(
                        "Matrícula em lote inviável: o aluno já está matriculado em duas turmas (limite atingido).",
                        HttpStatus.BAD_REQUEST
                );
            }

            List<SchoolRoom> othersWithSlots = new ArrayList<>();
            for (Long vid : virtualRoomToWorkshop.keySet()) {
                SchoolRoom loaded = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(vid)
                        .orElse(null);
                if (loaded != null) {
                    othersWithSlots.add(loaded);
                }
            }

            String conflictName = EnrollmentRules.conflictingRoomName(room, othersWithSlots);
            if (conflictName != null) {
                throw new BusinessException(
                        "Matrícula em lote inviável: conflito de horário entre a turma \""
                                + room.getName() + "\" e a turma \"" + conflictName + "\".",
                        HttpStatus.BAD_REQUEST
                );
            }

            long currentTotal = enrollmentRepository.countBySchoolroomIdAndStatusTrue(roomId);
            if (room.getCapacity() != null && currentTotal >= room.getCapacity()) {
                throw new BusinessException(
                        "Matrícula em lote inviável: a turma \"" + room.getName()
                                + "\" já atingiu a capacidade máxima.",
                        HttpStatus.BAD_REQUEST
                );
            }

            virtualRoomToWorkshop.put(room.getId(), wId);
        }
    }

    @Transactional
    public void unenrollStudent(Long studentId, Long roomId) {
        var enrollment = enrollmentRepository.findByStudentIdAndSchoolroomIdAndStatusTrue(studentId, roomId)
                .orElseThrow(() -> new EntityNotFoundException("Vínculo ativo não encontrado para desvincular."));

        long countBefore = enrollmentRepository.countBySchoolroomIdAndStatusTrue(roomId);
        enrollment.setStatus(false);
        enrollmentRepository.save(enrollment);
        schoolRoomVacancyService.notifyIfVacancyOpened(roomId, countBefore, countBefore - 1, VacancyCause.UNENROLL);
    }
}
