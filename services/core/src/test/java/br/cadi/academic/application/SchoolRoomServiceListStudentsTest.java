package br.cadi.academic.application;

import br.cadi.academic.api.dto.response.SchoolRoomStudentResponse;
import br.cadi.academic.domain.entities.Enrollment;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.domain.entities.Workshop;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.academic.infra.repositories.WorkshopRepository;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.enums.CollaboratorType;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import br.cadi.people.infra.repositories.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolRoomServiceListStudentsTest {

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

    private AcademicAuthorizationService academicAuthorizationService;

    private SchoolRoomService schoolRoomService;

    private SchoolRoom schoolRoom;
    private Collaborator teacher;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        academicAuthorizationService = new AcademicAuthorizationService(personRepository);
        schoolRoomService = new SchoolRoomService(
                schoolRoomRepository,
                workshopRepository,
                collaboratorRepository,
                enrollmentRepository,
                academicAuthorizationService,
                schoolRoomVacancyService
        );
        Workshop w = Workshop.builder().id(1L).name("W").isActive(true).build();
        teacher = Collaborator.builder()
                .id(10L)
                .name("Prof")
                .email("prof@cadi.com")
                .cpf("111")
                .phone("1")
                .birthDate(LocalDate.of(1980, 1, 1))
                .educationLevel("sup")
                .type(CollaboratorType.TEACHER)
                .build();
        schoolRoom = SchoolRoom.builder()
                .id(1L)
                .name("Turma A")
                .classroom("101")
                .isActive(true)
                .workshop(w)
                .teacher(teacher)
                .build();
    }

    @Test
    void deveLancar404QuandoTurmaNaoExiste() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(99L)).thenReturn(Optional.empty());
        var auth = new TestingAuthenticationToken("admin@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThatThrownBy(() -> schoolRoomService.listStudents(99L, auth))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Turma não encontrada");
    }

    @Test
    void adminDeveListarAlunos() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));
        Student student = Student.builder()
                .id(5L)
                .name("Aluno")
                .email("aluno@cadi.com")
                .cpf("222")
                .phone("2")
                .birthDate(LocalDate.of(2010, 1, 1))
                .grade("6")
                .currentSchool("Escola X")
                .periodStudies("MATUTINO")
                .build();
        Enrollment en = Enrollment.builder()
                .id(1L)
                .student(student)
                .schoolroom(schoolRoom)
                .enrollmentDate(LocalDate.of(2024, 3, 1))
                .status(true)
                .build();
        when(enrollmentRepository.findActiveBySchoolroomIdWithStudent(1L)).thenReturn(List.of(en));

        var auth = new TestingAuthenticationToken("admin@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<SchoolRoomStudentResponse> result = schoolRoomService.listStudents(1L, auth);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().studentId()).isEqualTo(5L);
        assertThat(result.getFirst().name()).isEqualTo("Aluno");
        assertThat(result.getFirst().enrollmentActive()).isTrue();
    }

    @Test
    void professorVinculadoDeveListarAlunos() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));
        when(enrollmentRepository.findActiveBySchoolroomIdWithStudent(1L)).thenReturn(List.of());
        when(personRepository.findByEmail("prof@cadi.com")).thenReturn(Optional.of(teacher));

        var auth = new TestingAuthenticationToken("prof@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        List<SchoolRoomStudentResponse> result = schoolRoomService.listStudents(1L, auth);
        assertThat(result).isEmpty();
    }

    @Test
    void professorNaoVinculadoDeveReceber403() {
        Collaborator outro = Collaborator.builder()
                .id(11L)
                .name("Outro")
                .email("outro@cadi.com")
                .cpf("333")
                .phone("3")
                .birthDate(LocalDate.of(1980, 1, 1))
                .educationLevel("sup")
                .type(CollaboratorType.TEACHER)
                .build();
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));
        when(personRepository.findByEmail("outro@cadi.com")).thenReturn(Optional.of(outro));

        var auth = new TestingAuthenticationToken("outro@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        assertThatThrownBy(() -> schoolRoomService.listStudents(1L, auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void professorComTurmaSemProfessorDesignadoDeveReceber403() {
        schoolRoom.setTeacher(null);
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));

        var auth = new TestingAuthenticationToken("prof@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        assertThatThrownBy(() -> schoolRoomService.listStudents(1L, auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void perfilAlunoDeveReceber403() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));

        var auth = new TestingAuthenticationToken("aluno@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        assertThatThrownBy(() -> schoolRoomService.listStudents(1L, auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void secretariaDeveListarQualquerTurma() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));
        when(enrollmentRepository.findActiveBySchoolroomIdWithStudent(anyLong())).thenReturn(List.of());

        var auth = new TestingAuthenticationToken("sec@cadi.com", null,
                List.of(new SimpleGrantedAuthority("ROLE_SECRETARY")));

        assertThat(schoolRoomService.listStudents(1L, auth)).isEmpty();
    }
}
