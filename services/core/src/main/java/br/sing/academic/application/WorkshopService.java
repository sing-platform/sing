package br.sing.academic.application;

import br.sing.academic.api.dto.response.SchoolRoomDeactivationImpactRow;
import br.sing.academic.api.dto.request.WorkshopCreateRequest;
import br.sing.academic.api.dto.response.WorkshopDeactivationPreviewResponse;
import br.sing.academic.api.dto.response.WorkshopResponse;
import br.sing.academic.api.dto.request.WorkshopUpdateRequest;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.Workshop;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.academic.infra.repositories.WorkshopRepository;
import br.sing.exception.BusinessException;
import br.sing.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public WorkshopResponse create(WorkshopCreateRequest request) {
        String normalizedName = request.name().trim();
        Workshop existing = workshopRepository.findFirstByNameIgnoreCase(normalizedName).orElse(null);

        if (existing != null && Boolean.TRUE.equals(existing.isActive())) {
            throw new BusinessException("Já existe uma oficina com este nome", HttpStatus.CONFLICT);
        }

        if (existing != null) {
            existing.setName(normalizedName);
            existing.setActive(true);
            Workshop reactivated = workshopRepository.save(existing);
            return toResponse(reactivated);
        }

        Workshop workshop = new Workshop();
        workshop.setName(normalizedName);
        workshop.setActive(true);

        Workshop saved = workshopRepository.save(workshop);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkshopResponse> listAll(Boolean onlyActive) {
        List<Workshop> workshops = Boolean.TRUE.equals(onlyActive)
                ? workshopRepository.findByIsActiveTrue()
                : workshopRepository.findAll();

        return workshops.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkshopResponse getById(Long id) {
        Workshop workshop = getWorkshopOrThrow(id);
        return toResponse(workshop);
    }

    @Transactional
    public WorkshopResponse update(Long id, WorkshopUpdateRequest request) {
        Workshop workshop = getWorkshopOrThrow(id);
        validateDuplicateNameForUpdate(id, request.name());

        workshop.setName(request.name().trim());
        Workshop saved = workshopRepository.save(workshop);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public WorkshopDeactivationPreviewResponse previewDeactivation(Long workshopId) {
        Workshop workshop = getWorkshopOrThrow(workshopId);
        List<SchoolRoom> rooms = schoolRoomRepository.findByWorkshop_Id(workshopId);
        List<SchoolRoomDeactivationImpactRow> rows = rooms.stream()
                .sorted(Comparator.comparing(SchoolRoom::getName))
                .map(r -> new SchoolRoomDeactivationImpactRow(
                        r.getId(),
                        r.getName(),
                        Boolean.TRUE.equals(r.isActive()),
                        enrollmentRepository.existsBySchoolroomIdAndStatusTrue(r.getId())
                ))
                .toList();

        boolean canProceed = rooms.stream()
                .filter(r -> Boolean.TRUE.equals(r.isActive()))
                .noneMatch(r -> enrollmentRepository.existsBySchoolroomIdAndStatusTrue(r.getId()));

        return new WorkshopDeactivationPreviewResponse(
                workshop.getId(),
                workshop.getName(),
                canProceed,
                rows
        );
    }

    /**
     * Desativa a oficina e, em cascata, todas as turmas ativas vinculadas.
     * Bloqueia se alguma turma ativa possuir matrículas ativas.
     */
    @Transactional
    public void deactivateWithCascade(Long id) {
        Workshop workshop = getWorkshopOrThrow(id);
        List<SchoolRoom> rooms = schoolRoomRepository.findByWorkshop_Id(id);
        List<SchoolRoom> active = rooms.stream()
                .filter(r -> Boolean.TRUE.equals(r.isActive()))
                .toList();

        List<String> blocking = new ArrayList<>();
        for (SchoolRoom r : active) {
            if (enrollmentRepository.existsBySchoolroomIdAndStatusTrue(r.getId())) {
                blocking.add(r.getName());
            }
        }
        if (!blocking.isEmpty()) {
            throw new BusinessException(
                    "Não é possível desativar a oficina: há matrículas ativas nas turmas: "
                            + String.join(", ", blocking),
                    HttpStatus.CONFLICT
            );
        }

        workshop.setActive(false);
        workshopRepository.save(workshop);

        for (SchoolRoom r : active) {
            r.setActive(false);
            schoolRoomRepository.save(r);
        }
    }

    private Workshop getWorkshopOrThrow(Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Oficina não encontrada"));
    }

    private void validateDuplicateNameForUpdate(Long id, String name) {
        if (workshopRepository.existsByNameIgnoreCaseAndIdNot(name.trim(), id)) {
            throw new BusinessException("Já existe uma oficina com este nome", HttpStatus.CONFLICT);
        }
    }

    private WorkshopResponse toResponse(Workshop workshop) {
        return new WorkshopResponse(
                workshop.getId(),
                workshop.getName(),
                workshop.isActive()
        );
    }
}
