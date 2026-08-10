package br.cadi.academic.application;

import br.cadi.academic.api.dto.request.ScheduleSlotRequest;
import br.cadi.academic.api.dto.response.ScheduleSlotResponse;
import br.cadi.academic.api.dto.request.SchoolRoomCreateRequest;
import br.cadi.academic.api.dto.response.AdminScheduleBoardResponse;
import br.cadi.academic.api.dto.response.SchoolRoomResponse;
import br.cadi.academic.api.dto.response.SchoolRoomStudentResponse;
import br.cadi.academic.api.dto.request.SchoolRoomUpdateRequest;
import br.cadi.academic.domain.entities.Enrollment;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.domain.entities.SchoolRoomScheduleSlot;
import br.cadi.academic.domain.entities.Workshop;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.academic.infra.repositories.WorkshopRepository;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.people.api.dto.request.AssignTeacherRequest;
import br.cadi.iam.domain.User;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.enums.CollaboratorType;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
@Service
@RequiredArgsConstructor
public class SchoolRoomService {

    private static final String ROLE_PROFESSOR = "ROLE_PROFESSOR";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final SchoolRoomRepository schoolRoomRepository;
    private final WorkshopRepository workshopRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicAuthorizationService academicAuthorizationService;
    private final SchoolRoomVacancyService schoolRoomVacancyService;

    @Transactional
    public SchoolRoomResponse create(SchoolRoomCreateRequest request) {
        Workshop workshop = getWorkshopOrThrow(request.workshopId());
        validateWorkshopIsActive(workshop);
        validateDuplicateNameForCreate(request.name(), workshop.getId());
        validateScheduleSlots(request.scheduleSlots());

        SchoolRoom schoolRoom = new SchoolRoom();
        schoolRoom.setName(request.name().trim());
        schoolRoom.setSchedule(null);
        schoolRoom.setClassroom(request.classroom().trim());
        schoolRoom.setCapacity(request.capacity());
        schoolRoom.setActive(true);
        schoolRoom.setWorkshop(workshop);

        Collaborator teacher = getTeacherOrNull(request.teacherId());
        validateTeacherScheduleConflict(teacher, null, request.scheduleSlots());
        schoolRoom.setTeacher(teacher);

        applyScheduleSlots(schoolRoom, request.scheduleSlots());

        SchoolRoom saved = schoolRoomRepository.save(schoolRoom);
        return toResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<SchoolRoomResponse> listAll(Boolean onlyActive) {
        List<SchoolRoom> schoolRooms = Boolean.TRUE.equals(onlyActive)
                ? schoolRoomRepository.findByIsActiveTrue()
                : schoolRoomRepository.findAll();

        Map<Long, Long> counts = loadActiveEnrollmentCounts(schoolRooms.stream().map(SchoolRoom::getId).toList());
        return schoolRooms.stream()
                .map(sr -> toResponse(sr, counts.getOrDefault(sr.getId(), 0L)))
                .toList();
    }

    /**
     * Monta {@link SchoolRoomResponse} para os IDs informados (turmas ativas/inativas conforme persistência),
     * com slots, professor e contagem de matrículas ativas.
     */
    @Transactional(readOnly = true)
    public List<SchoolRoomResponse> listResponsesBySchoolRoomIds(Collection<Long> schoolRoomIds) {
        if (schoolRoomIds == null || schoolRoomIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = schoolRoomIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<SchoolRoom> loaded = schoolRoomRepository.findByIdIn(distinct);
        Map<Long, Long> counts = loadActiveEnrollmentCounts(distinct);
        Map<Long, SchoolRoomResponse> byId = new HashMap<>();
        for (SchoolRoom sr : loaded) {
            byId.put(sr.getId(), toResponse(sr, counts.getOrDefault(sr.getId(), 0L)));
        }
        return distinct.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    /**
     * Uma leitura de turmas ativas + matrículas ativas com alunos (evita N+1 HTTP na grade admin).
     */
    @Transactional(readOnly = true)
    public AdminScheduleBoardResponse getAdminScheduleBoardData() {
        List<SchoolRoom> rooms = schoolRoomRepository.findByIsActiveTrue();
        List<Long> ids = rooms.stream().map(SchoolRoom::getId).toList();
        Map<Long, Long> counts = loadActiveEnrollmentCounts(ids);
        List<SchoolRoomResponse> roomResponses = rooms.stream()
                .map(sr -> toResponse(sr, counts.getOrDefault(sr.getId(), 0L)))
                .toList();
        if (ids.isEmpty()) {
            return new AdminScheduleBoardResponse(roomResponses, Map.of());
        }
        List<Enrollment> enrollments = enrollmentRepository.findActiveBySchoolRoomIdsWithStudent(ids);
        Map<Long, List<SchoolRoomStudentResponse>> byRoom = new LinkedHashMap<>();
        for (Enrollment e : enrollments) {
            Long roomId = e.getSchoolroom().getId();
            byRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(toStudentResponse(e));
        }
        return new AdminScheduleBoardResponse(roomResponses, byRoom);
    }

    @Transactional(readOnly = true)
    public SchoolRoomResponse getById(Long id) {
        SchoolRoom schoolRoom = getSchoolRoomOrThrow(id);
        long active = enrollmentRepository.countBySchoolroomIdAndStatusTrue(id);
        return toResponse(schoolRoom, active);
    }

    @Transactional(readOnly = true)
    public Page<SchoolRoomResponse> pageSchoolRoomsForTeacher(
            Long teacherId,
            boolean activeOnly,
            Pageable pageable,
            Authentication authentication
    ) {
        academicAuthorizationService.assertProfessorOwnsTeacherIdOrManagement(authentication, teacherId);
        Pageable p = AcademicPageables.normalizeSchoolRoomPageable(pageable);
        Boolean activeFilter = activeOnly ? Boolean.TRUE : null;
        Page<SchoolRoom> page = schoolRoomRepository.pageByTeacherIdAndActiveOptional(teacherId, activeFilter, p);
        Map<Long, Long> counts = loadActiveEnrollmentCounts(page.getContent().stream().map(SchoolRoom::getId).toList());
        return page.map(sr -> toResponse(sr, counts.getOrDefault(sr.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public List<SchoolRoomStudentResponse> listStudents(Long schoolRoomId, Authentication authentication) {
        SchoolRoom schoolRoom = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));
        academicAuthorizationService.assertCanAccessSchoolRoomAsProfessor(authentication, schoolRoom);
        List<Enrollment> enrollments = enrollmentRepository.findActiveBySchoolroomIdWithStudent(schoolRoomId);
        return enrollments.stream().map(this::toStudentResponse).toList();
    }

    private SchoolRoomStudentResponse toStudentResponse(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        return new SchoolRoomStudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhone(),
                student.getGrade(),
                student.getPeriodStudies(),
                enrollment.getEnrollmentDate(),
                Boolean.TRUE.equals(enrollment.getStatus())
        );
    }

    @Transactional
    public SchoolRoomResponse update(Long id, SchoolRoomUpdateRequest request) {
        SchoolRoom schoolRoom = getSchoolRoomOrThrow(id);
        Workshop workshop = getWorkshopOrThrow(request.workshopId());
        validateWorkshopIsActive(workshop);
        validateDuplicateNameForUpdate(id, request.name(), workshop.getId());
        validateScheduleSlots(request.scheduleSlots());

        Integer previousCapacity = schoolRoom.getCapacity();
        schoolRoom.setName(request.name().trim());
        schoolRoom.setSchedule(null);
        schoolRoom.setClassroom(request.classroom().trim());
        schoolRoom.setCapacity(request.capacity());
        schoolRoom.setActive(request.active());
        schoolRoom.setWorkshop(workshop);

        Collaborator teacher = getTeacherOrNull(request.teacherId());
        validateTeacherScheduleConflict(teacher, id, request.scheduleSlots());
        schoolRoom.setTeacher(teacher);

        applyScheduleSlots(schoolRoom, request.scheduleSlots());

        SchoolRoom saved = schoolRoomRepository.save(schoolRoom);
        schoolRoomVacancyService.notifyIfCapacityIncreaseOpenedVacancy(saved, previousCapacity, saved.getCapacity());
        long active = enrollmentRepository.countBySchoolroomIdAndStatusTrue(id);
        return toResponse(saved, active);
    }

    @Transactional
    public void deactivate(Long id) {
        SchoolRoom schoolRoom = getSchoolRoomOrThrow(id);
        if (enrollmentRepository.existsBySchoolroomIdAndStatusTrue(id)) {
            throw new BusinessException("Não é possível desativar turma com matrículas ativas", HttpStatus.CONFLICT);
        }
        schoolRoom.setActive(false);
        schoolRoomRepository.save(schoolRoom);
    }

    private SchoolRoom getSchoolRoomOrThrow(Long id) {
        return schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(id)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada"));
    }

    private Workshop getWorkshopOrThrow(Long workshopId) {
        return workshopRepository.findById(workshopId)
                .orElseThrow(() -> new EntityNotFoundException("Oficina não encontrada"));
    }

    private Collaborator getTeacherOrNull(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        Collaborator collaborator = collaboratorRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Professor/colaborador não encontrado"));
        if (!isEligibleCollaboratorAsTitular(collaborator)) {
            throw new BusinessException(
                    "Colaborador informado não pode ser titular (esperado tipo professor ou usuário com papel de professor)",
                    HttpStatus.BAD_REQUEST);
        }
        return collaborator;
    }

    /** Alinhado à listagem de GET /teachers: tipo TEACHER ou role IAM ROLE_PROFESSOR. */
    private boolean isEligibleCollaboratorAsTitular(Collaborator c) {
        if (c.getType() == CollaboratorType.TEACHER) {
            return true;
        }
        User u = c.getUser();
        return u != null && u.getRole() != null && ROLE_PROFESSOR.equals(u.getRole().getName());
    }

    private void validateWorkshopIsActive(Workshop workshop) {
        if (!Boolean.TRUE.equals(workshop.isActive())) {
            throw new BusinessException("Não é possível vincular turma a oficina desativada", HttpStatus.CONFLICT);
        }
    }

    private void validateDuplicateNameForCreate(String name, Long workshopId) {
        if (schoolRoomRepository.existsByNameIgnoreCaseAndWorkshopIdAndIsActiveTrue(name.trim(), workshopId)) {
            throw new BusinessException("Já existe turma ativa com este nome para a oficina informada", HttpStatus.CONFLICT);
        }
    }

    private void validateDuplicateNameForUpdate(Long id, String name, Long workshopId) {
        if (schoolRoomRepository.existsByNameIgnoreCaseAndWorkshopIdAndIdNotAndIsActiveTrue(name.trim(), workshopId, id)) {
            throw new BusinessException("Já existe turma ativa com este nome para a oficina informada", HttpStatus.CONFLICT);
        }
    }

    private void validateScheduleSlots(List<ScheduleSlotRequest> slots) {
        for (ScheduleSlotRequest slot : slots) {
            if (!slot.startTime().isBefore(slot.endTime())) {
                throw new BusinessException("Horário de início deve ser anterior ao horário de fim", HttpStatus.BAD_REQUEST);
            }
        }
        List<ScheduleSlotRequest> sorted = new ArrayList<>(slots);
        sorted.sort(Comparator.comparing(ScheduleSlotRequest::dayOfWeek).thenComparing(ScheduleSlotRequest::startTime));
        for (int i = 0; i < sorted.size() - 1; i++) {
            ScheduleSlotRequest a = sorted.get(i);
            ScheduleSlotRequest b = sorted.get(i + 1);
            if (a.dayOfWeek() != b.dayOfWeek()) {
                continue;
            }
            if (a.startTime().isBefore(b.endTime()) && b.startTime().isBefore(a.endTime())) {
                throw new BusinessException("Horários sobrepostos no mesmo dia da semana", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateTeacherScheduleConflict(Collaborator teacher, Long currentSchoolRoomId, List<ScheduleSlotRequest> newSlots) {
        if (teacher == null || newSlots == null || newSlots.isEmpty()) {
            return;
        }
        List<SchoolRoom> teacherRooms = schoolRoomRepository.findByTeacherIdAndIsActiveTrue(teacher.getId());
        for (SchoolRoom room : teacherRooms) {
            if (currentSchoolRoomId != null && room.getId().equals(currentSchoolRoomId)) {
                continue;
            }
            List<SchoolRoomScheduleSlot> existingSlots = room.getScheduleSlots();
            if (existingSlots == null || existingSlots.isEmpty()) {
                continue;
            }
            for (ScheduleSlotRequest newSlot : newSlots) {
                for (SchoolRoomScheduleSlot existingSlot : existingSlots) {
                    if (newSlot.dayOfWeek() == existingSlot.getDayOfWeek()) {
                        boolean overlap = newSlot.startTime().isBefore(existingSlot.getEndTime()) && existingSlot.getStartTime().isBefore(newSlot.endTime());
                        if (overlap) {
                            String msg = String.format("Professor já possui a turma '%s' no mesmo horário (%s %s-%s)",
                                    room.getName(), dayAbbrevPt(newSlot.dayOfWeek()),
                                    newSlot.startTime().format(TIME_FMT), newSlot.endTime().format(TIME_FMT));
                            throw new BusinessException(msg, HttpStatus.CONFLICT);
                        }
                    }
                }
            }
        }
    }

    private void applyScheduleSlots(SchoolRoom schoolRoom, List<ScheduleSlotRequest> requests) {
        schoolRoom.getScheduleSlots().clear();
        for (ScheduleSlotRequest r : requests) {
            SchoolRoomScheduleSlot slot = new SchoolRoomScheduleSlot();
            slot.setSchoolroom(schoolRoom);
            slot.setDayOfWeek(r.dayOfWeek());
            slot.setStartTime(r.startTime());
            slot.setEndTime(r.endTime());
            schoolRoom.getScheduleSlots().add(slot);
        }
    }

    private Map<Long, Long> loadActiveEnrollmentCounts(Collection<Long> schoolRoomIds) {
        if (schoolRoomIds == null || schoolRoomIds.isEmpty()) {
            return Map.of();
        }
        List<Long> distinct = schoolRoomIds.stream().distinct().toList();
        Map<Long, Long> map = new HashMap<>();
        for (Long id : distinct) {
            map.put(id, 0L);
        }
        for (Object[] row : enrollmentRepository.countActiveEnrollmentsGroupedBySchoolRoomId(distinct)) {
            Long roomId = ((Number) row[0]).longValue();
            Long cnt = ((Number) row[1]).longValue();
            map.put(roomId, cnt);
        }
        return map;
    }

    private SchoolRoomResponse toResponse(SchoolRoom schoolRoom, long activeEnrollmentCount) {
        List<SchoolRoomScheduleSlot> slots = schoolRoom.getScheduleSlots() == null
                ? List.of()
                : schoolRoom.getScheduleSlots().stream()
                        .filter(s -> s.getDayOfWeek() != null && s.getStartTime() != null && s.getEndTime() != null)
                        .toList();
        List<ScheduleSlotResponse> slotResponses = slots.stream()
                .sorted(Comparator.comparing(SchoolRoomScheduleSlot::getDayOfWeek).thenComparing(SchoolRoomScheduleSlot::getStartTime))
                .map(s -> new ScheduleSlotResponse(s.getId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();

        String scheduleText;
        if (!slotResponses.isEmpty()) {
            scheduleText = formatScheduleSummaryFromSlots(slots);
        } else {
            scheduleText = schoolRoom.getSchedule() != null ? schoolRoom.getSchedule() : "";
        }

        Integer capacity = schoolRoom.getCapacity();
        Integer vacancies = capacity == null
                ? null
                : (int) Math.max(0L, capacity.longValue() - activeEnrollmentCount);

        Collaborator teacher = schoolRoom.getTeacher();
        Workshop workshop = schoolRoom.getWorkshop();
        Long workshopId = workshop != null ? workshop.getId() : null;
        String workshopName = workshop != null && workshop.getName() != null ? workshop.getName() : "";
        return new SchoolRoomResponse(
                schoolRoom.getId(),
                schoolRoom.getName(),
                scheduleText,
                slotResponses,
                schoolRoom.getClassroom(),
                capacity,
                schoolRoom.isActive(),
                workshopId,
                workshopName,
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getName() : null,
                (int) Math.min(activeEnrollmentCount, Integer.MAX_VALUE),
                vacancies
        );
    }

    @Transactional
    public SchoolRoomResponse assignTeacher(Long schoolRoomId, AssignTeacherRequest request) {
        SchoolRoom schoolRoom = schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(schoolRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        Collaborator teacher = collaboratorRepository.findById(request.teacherId())
                .filter(this::isEligibleCollaboratorAsTitular)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor não encontrado"));

        if (teacher.equals(schoolRoom.getTeacher())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Professor já vinculado a esta turma");
        }

        List<ScheduleSlotRequest> slotsAsRequest = schoolRoom.getScheduleSlots().stream()
                .map(s -> new ScheduleSlotRequest(s.getDayOfWeek(), s.getStartTime(), s.getEndTime()))
                .toList();
        validateTeacherScheduleConflict(teacher, schoolRoomId, slotsAsRequest);

        schoolRoom.setTeacher(teacher);
        SchoolRoom saved = schoolRoomRepository.save(schoolRoom);
        long active = enrollmentRepository.countBySchoolroomIdAndStatusTrue(schoolRoomId);
        return toResponse(saved, active);
    }

    @Transactional
    public SchoolRoomResponse removeTeacher(Long schoolRoomId) {
        SchoolRoom schoolRoom = schoolRoomRepository.findById(schoolRoomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        if (schoolRoom.getTeacher() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Turma não possui professor vinculado");
        }

        schoolRoom.setTeacher(null);
        SchoolRoom saved = schoolRoomRepository.save(schoolRoom);
        long active = enrollmentRepository.countBySchoolroomIdAndStatusTrue(schoolRoomId);
        return toResponse(saved, active);
    }

    private String formatScheduleSummaryFromSlots(List<SchoolRoomScheduleSlot> slots) {
        return slots.stream()
                .sorted(Comparator.comparing(SchoolRoomScheduleSlot::getDayOfWeek).thenComparing(SchoolRoomScheduleSlot::getStartTime))
                .map(s -> dayAbbrevPt(s.getDayOfWeek()) + " "
                        + s.getStartTime().format(TIME_FMT) + "–" + s.getEndTime().format(TIME_FMT))
                .reduce((a, b) -> a + "; " + b)
                .orElse("");
    }

    private static String dayAbbrevPt(DayOfWeek day) {
        return switch (Objects.requireNonNull(day)) {
            case MONDAY -> "Seg";
            case TUESDAY -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY -> "Qui";
            case FRIDAY -> "Sex";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }
}
