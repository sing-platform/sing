package br.sing.academic.application;

import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.Workshop;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.exception.BusinessException;
import br.sing.exception.EntityNotFoundException;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.domain.enums.CollaboratorType;
import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentQueryServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private SchoolRoomRepository schoolRoomRepository;

    @Mock
    private PersonRepository personRepository;

    private EnrollmentQueryService enrollmentQueryService;

    private Workshop workshop;
    private Collaborator teacher;
    private SchoolRoom schoolRoom;
    private Student student;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        AcademicAuthorizationService auth = new AcademicAuthorizationService(personRepository);
        enrollmentQueryService = new EnrollmentQueryService(enrollmentRepository, schoolRoomRepository, auth);
        workshop = Workshop.builder().id(1L).name("Oficina").isActive(true).build();
        teacher = Collaborator.builder()
                .id(10L)
                .name("Prof")
                .email("prof@sing.local")
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
                .workshop(workshop)
                .teacher(teacher)
                .build();
        student = Student.builder()
                .id(5L)
                .name("Aluno")
                .email("aluno@sing.local")
                .cpf("222")
                .phone("2")
                .birthDate(LocalDate.of(2010, 1, 1))
                .grade("6")
                .currentSchool("Escola X")
                .periodStudies("MATUTINO")
                .build();
        enrollment = Enrollment.builder()
                .id(100L)
                .student(student)
                .schoolroom(schoolRoom)
                .enrollmentDate(LocalDate.of(2024, 3, 1))
                .status(true)
                .build();
    }

    @Test
    void pageBySchoolRoomTurmaInexistenteLanca404() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(99L)).thenReturn(Optional.empty());
        var auth = new TestingAuthenticationToken("admin@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertThatThrownBy(() -> enrollmentQueryService.pageBySchoolRoom(99L, null, PageRequest.of(0, 20), auth))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void pageBySchoolRoomAdminDelegaAoRepositorio() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));
        Pageable p = PageRequest.of(0, 20);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), p, 1);
        when(enrollmentRepository.pageBySchoolRoomIdAndStatusOptional(eq(1L), eq(Boolean.TRUE), any(Pageable.class)))
                .thenReturn(page);
        var auth = new TestingAuthenticationToken("admin@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var result = enrollmentQueryService.pageBySchoolRoom(1L, true, p, auth);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().enrollmentId()).isEqualTo(100L);
        verify(enrollmentRepository).pageBySchoolRoomIdAndStatusOptional(eq(1L), eq(Boolean.TRUE), any(Pageable.class));
    }

    @Test
    void pageBySchoolRoomProfessorTitularDelegaAoRepositorio() {
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(1L)).thenReturn(Optional.of(schoolRoom));
        when(personRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        Pageable p = PageRequest.of(0, 20);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), p, 1);
        when(enrollmentRepository.pageBySchoolRoomIdAndStatusOptional(eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(page);
        var auth = new TestingAuthenticationToken("prof@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
        var result = enrollmentQueryService.pageBySchoolRoom(1L, null, p, auth);
        assertThat(result.getContent()).hasSize(1);
        verify(enrollmentRepository).pageBySchoolRoomIdAndStatusOptional(eq(1L), isNull(), any(Pageable.class));
    }

    @Test
    void pageByStudentProfessorUsaFiltroPorTitular() {
        when(personRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        Pageable p = PageRequest.of(0, 20);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), p, 1);
        when(enrollmentRepository.pageByStudentIdAndTeacherIdAndStatusOptional(eq(5L), eq(10L), eq(Boolean.TRUE), any(Pageable.class)))
                .thenReturn(page);
        var auth = new TestingAuthenticationToken("prof@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
        var result = enrollmentQueryService.pageByStudent(5L, true, p, auth);
        assertThat(result.getContent()).hasSize(1);
        verify(enrollmentRepository).pageByStudentIdAndTeacherIdAndStatusOptional(eq(5L), eq(10L), eq(Boolean.TRUE), any(Pageable.class));
    }

    @Test
    void pageByStudentAlunoRecebe403() {
        var auth = new TestingAuthenticationToken("aluno@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        assertThatThrownBy(() -> enrollmentQueryService.pageByStudent(5L, null, PageRequest.of(0, 20), auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void pageByTeacherProfessorOutroIdRecebe403() {
        when(personRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        var auth = new TestingAuthenticationToken("prof@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
        assertThatThrownBy(() -> enrollmentQueryService.pageByTeacher(99L, null, PageRequest.of(0, 20), auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void pageByTeacherProfessorProprioIdConsultaRepositorio() {
        when(personRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        Pageable p = PageRequest.of(0, 20);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), p, 1);
        when(enrollmentRepository.pageByTeacherIdAndStatusOptional(eq(10L), eq(Boolean.TRUE), any(Pageable.class)))
                .thenReturn(page);
        var auth = new TestingAuthenticationToken("prof@sing.local", null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));
        var result = enrollmentQueryService.pageByTeacher(10L, true, p, auth);
        assertThat(result.getContent()).hasSize(1);
        verify(enrollmentRepository).pageByTeacherIdAndStatusOptional(eq(10L), eq(Boolean.TRUE), any(Pageable.class));
    }
}
