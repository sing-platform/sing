package br.cadi.academic.application;

import br.cadi.academic.api.dto.request.ScheduleSlotRequest;
import br.cadi.academic.api.dto.request.SchoolRoomCreateRequest;
import br.cadi.academic.api.dto.request.SchoolRoomUpdateRequest;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.domain.entities.Workshop;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.academic.infra.repositories.WorkshopRepository;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.people.api.dto.request.AssignTeacherRequest;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import br.cadi.people.infra.repositories.PersonRepository;
import br.cadi.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolRoomServiceCrudTest {

    @Mock
    private SchoolRoomRepository schoolRoomRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private CollaboratorRepository collaboratorRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private SchoolRoomVacancyService schoolRoomVacancyService;

    private SchoolRoomService schoolRoomService;
    private Workshop workshop;
    private Collaborator teacher;
    private SchoolRoom schoolRoom;

    @BeforeEach
    void setUp() {
        var auth = new AcademicAuthorizationService(personRepository);
        schoolRoomService = new SchoolRoomService(
                schoolRoomRepository, workshopRepository, collaboratorRepository, enrollmentRepository, auth,
                schoolRoomVacancyService
        );
        workshop = TestDataFactory.workshop(1L, "Oficina A", true);
        teacher = TestDataFactory.teacher(10L, "prof@cadi.com");
        schoolRoom = SchoolRoom.builder()
                .id(100L).name("Turma A").classroom("Sala 1").capacity(30).isActive(true)
                .workshop(workshop).teacher(teacher).build();
    }

    @Test
    void createDeveCriarTurmaQuandoDadosValidos() {
        var request = createRequest("Turma Nova", 1L, 10L);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.existsByNameIgnoreCaseAndWorkshopIdAndIsActiveTrue("Turma Nova", 1L)).thenReturn(false);
        when(collaboratorRepository.findById(10L)).thenReturn(Optional.of(teacher));
        when(schoolRoomRepository.save(any(SchoolRoom.class))).thenAnswer(invocation -> {
            SchoolRoom saved = invocation.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        var response = schoolRoomService.create(request);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.name()).isEqualTo("Turma Nova");
        assertThat(response.workshopId()).isEqualTo(1L);
        assertThat(response.teacherId()).isEqualTo(10L);
        assertThat(response.activeEnrollmentCount()).isZero();
        assertThat(response.vacancies()).isEqualTo(25);
    }

    @Test
    void listAllDevePreencherOcupacaoDasTurmas() {
        when(schoolRoomRepository.findByIsActiveTrue()).thenReturn(List.of(schoolRoom));
        when(enrollmentRepository.countActiveEnrollmentsGroupedBySchoolRoomId(any()))
                .thenReturn(Collections.singletonList(new Object[]{100L, 3L}));

        var list = schoolRoomService.listAll(true);

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().activeEnrollmentCount()).isEqualTo(3);
        assertThat(list.getFirst().vacancies()).isEqualTo(27);
    }

    @Test
    void createDeveLancar409QuandoOficinaInativa() {
        var inactiveWorkshop = TestDataFactory.workshop(1L, "Oficina A", false);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(inactiveWorkshop));

        assertThatThrownBy(() -> schoolRoomService.create(createRequest("Turma", 1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("oficina desativada")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateDeveLancar409QuandoNomeDuplicado() {
        var request = updateRequest("Turma A", 1L, null, true);
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(100L)).thenReturn(Optional.of(schoolRoom));
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.existsByNameIgnoreCaseAndWorkshopIdAndIdNotAndIsActiveTrue("Turma A", 1L, 100L))
                .thenReturn(true);

        assertThatThrownBy(() -> schoolRoomService.update(100L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deactivateDeveLancar409QuandoHaMatriculasAtivas() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(100L)).thenReturn(Optional.of(schoolRoom));
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(100L)).thenReturn(true);

        assertThatThrownBy(() -> schoolRoomService.deactivate(100L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(schoolRoomRepository, never()).save(any());
    }

    @Test
    void deactivateDeveDesativarQuandoNaoHaMatriculasAtivas() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(100L)).thenReturn(Optional.of(schoolRoom));
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(100L)).thenReturn(false);

        schoolRoomService.deactivate(100L);

        assertThat(schoolRoom.isActive()).isFalse();
        verify(schoolRoomRepository).save(schoolRoom);
    }

    @Test
    void assignTeacherDeveLancar409QuandoProfessorJaVinculado() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(100L)).thenReturn(Optional.of(schoolRoom));
        when(collaboratorRepository.findById(10L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> schoolRoomService.assignTeacher(100L, new AssignTeacherRequest(10L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void removeTeacherDeveLancar409QuandoTurmaSemProfessor() {
        schoolRoom.setTeacher(null);
        when(schoolRoomRepository.findById(100L)).thenReturn(Optional.of(schoolRoom));

        assertThatThrownBy(() -> schoolRoomService.removeTeacher(100L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void assignTeacherDeveLancar404QuandoProfessorNaoExiste() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(100L)).thenReturn(Optional.of(schoolRoom));
        when(collaboratorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolRoomService.assignTeacher(100L, new AssignTeacherRequest(99L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void updateDeveLancar404QuandoTurmaNaoExiste() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schoolRoomService.update(999L, updateRequest("X", 1L, null, true)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Turma não encontrada");
    }

    @Test
    void createDeveLancar400QuandoHorarioInicioMaiorOuIgualFim() {
        var invalidSlots = List.of(new ScheduleSlotRequest(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 0)));
        var request = new SchoolRoomCreateRequest("Turma", invalidSlots, "Sala", 1L, 20, null);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.existsByNameIgnoreCaseAndWorkshopIdAndIsActiveTrue("Turma", 1L)).thenReturn(false);

        assertThatThrownBy(() -> schoolRoomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("início deve ser anterior")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createDeveLancar400QuandoHaSobreposicaoDeHorariosNoMesmoDia() {
        var overlapSlots = List.of(
                new ScheduleSlotRequest(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new ScheduleSlotRequest(DayOfWeek.MONDAY, LocalTime.of(9, 30), LocalTime.of(11, 0))
        );
        var request = new SchoolRoomCreateRequest("Turma", overlapSlots, "Sala", 1L, 20, null);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.existsByNameIgnoreCaseAndWorkshopIdAndIsActiveTrue("Turma", 1L)).thenReturn(false);

        assertThatThrownBy(() -> schoolRoomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Horários sobrepostos")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private SchoolRoomCreateRequest createRequest(String name, Long workshopId, Long teacherId) {
        return new SchoolRoomCreateRequest(name, validSlots(), "Sala 2", workshopId, 25, teacherId);
    }

    private SchoolRoomUpdateRequest updateRequest(String name, Long workshopId, Long teacherId, boolean active) {
        return new SchoolRoomUpdateRequest(name, validSlots(), "Sala 3", workshopId, 35, teacherId, active);
    }

    private List<ScheduleSlotRequest> validSlots() {
        return TestDataFactory.validSlots();
    }
}
