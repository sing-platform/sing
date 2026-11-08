package br.sing.audience.application;

import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.audience.api.AudienceRequest;
import br.sing.audience.api.AudienceResponse;
import br.sing.exception.BusinessException;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.UserRepository;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.domain.entities.Student;
import br.sing.people.infra.repositories.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MuralAudienceServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private SchoolRoomRepository schoolRoomRepository;

    private MuralAudienceService service;

    @BeforeEach
    void setUp() {
        service = new MuralAudienceService(userRepository, personRepository, enrollmentRepository, schoolRoomRepository);
    }

    private static User user(long id, String email, String role, boolean active) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .email(email)
                .password("x")
                .role(Role.builder().name(role).build())
                .isActive(active)
                .build();
    }

    private static Enrollment enrollmentOf(User studentUser) {
        Student student = Student.builder().id(studentUser.getId() + 100).user(studentUser).build();
        return Enrollment.builder().student(student).build();
    }

    @Test
    void gestorSemPublicoRetorna400() {
        User admin = user(1, "admin@sing.local", "ROLE_ADMIN", true);
        when(userRepository.findByEmail("admin@sing.local")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.resolve("admin@sing.local", new AudienceRequest(Set.of(), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Informe ao menos um campo de público-alvo: audienceRoles ou audienceSchoolroomIds.")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void professorComPapelRetorna403() {
        User teacher = user(2, "prof@sing.local", "ROLE_PROFESSOR", true);
        when(userRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.resolve("prof@sing.local", new AudienceRequest(Set.of("ROLE_STUDENT"), Set.of(1L))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Professor não pode usar público-alvo por role. Use audienceSchoolroomIds.")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void professorSemTurmaRetorna400() {
        User teacher = user(2, "prof@sing.local", "ROLE_PROFESSOR", true);
        when(userRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.resolve("prof@sing.local", new AudienceRequest(null, Set.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Professor deve informar ao menos uma turma em audienceSchoolroomIds.")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void professorSoPublicaParaTurmaPropria() {
        User teacher = user(2, "prof@sing.local", "ROLE_PROFESSOR", true);
        Collaborator self = Collaborator.builder().id(20L).build();
        Collaborator other = Collaborator.builder().id(21L).build();
        SchoolRoom foreign = SchoolRoom.builder().id(9L).teacher(other).build();
        when(userRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        when(personRepository.findByUserId(2L)).thenReturn(Optional.of(self));
        when(schoolRoomRepository.findById(9L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.resolve("prof@sing.local", new AudienceRequest(null, Set.of(9L))))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Turma 9 não pertence ao professor autenticado.")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void professorRecebeAlunosHabilitadosDaTurmaPropriaSemOAutor() {
        User teacher = user(2, "prof@sing.local", "ROLE_PROFESSOR", true);
        Collaborator self = Collaborator.builder().id(20L).name("Profa. Ana").build();
        SchoolRoom own = SchoolRoom.builder().id(3L).teacher(self).build();
        User alunoAtivo = user(7, "aluno@sing.local", "ROLE_STUDENT", true);
        User alunoInativo = user(8, "inativo@sing.local", "ROLE_STUDENT", false);
        when(userRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        when(personRepository.findByUserId(2L)).thenReturn(Optional.of(self));
        when(schoolRoomRepository.findById(3L)).thenReturn(Optional.of(own));
        when(enrollmentRepository.findActiveBySchoolroomIdWithStudent(3L))
                .thenReturn(List.of(enrollmentOf(alunoInativo), enrollmentOf(alunoAtivo), enrollmentOf(teacher)));

        AudienceResponse response = service.resolve("prof@sing.local", new AudienceRequest(null, Set.of(3L)));

        assertThat(response.author()).isEqualTo(new AudienceResponse.Author(2L, "Profa. Ana", "prof@sing.local", "ROLE_PROFESSOR"));
        assertThat(response.recipients()).containsExactly(new AudienceResponse.Recipient(7L, "aluno@sing.local"));
    }

    @Test
    void gestorPorPapelExcluiAutorEMantemTodosDoPapel() {
        User admin = user(1, "admin@sing.local", "ROLE_ADMIN", true);
        User aluno = user(7, "aluno@sing.local", "ROLE_STUDENT", true);
        User desabilitado = user(8, "off@sing.local", "ROLE_STUDENT", false);
        when(userRepository.findByEmail("admin@sing.local")).thenReturn(Optional.of(admin));
        when(userRepository.findByRole_Name("ROLE_STUDENT")).thenReturn(List.of(desabilitado, aluno));
        when(userRepository.findByRole_Name("ROLE_ADMIN")).thenReturn(List.of(admin));
        when(personRepository.findByUserId(1L)).thenReturn(Optional.empty());

        AudienceResponse response = service.resolve("admin@sing.local",
                new AudienceRequest(Set.of("ROLE_STUDENT", "ROLE_ADMIN"), null));

        assertThat(response.author().name()).isEqualTo("user1");
        assertThat(response.recipients()).containsExactly(
                new AudienceResponse.Recipient(7L, "aluno@sing.local"),
                new AudienceResponse.Recipient(8L, "off@sing.local"));
    }

    @Test
    void gestorPorTurmaNaoValidaPropriedadeDaTurma() {
        User secretary = user(4, "sec@sing.local", "ROLE_SECRETARY", true);
        User aluno = user(7, "aluno@sing.local", "ROLE_STUDENT", true);
        when(userRepository.findByEmail("sec@sing.local")).thenReturn(Optional.of(secretary));
        when(personRepository.findByUserId(4L)).thenReturn(Optional.empty());
        when(enrollmentRepository.findActiveBySchoolroomIdWithStudent(5L)).thenReturn(List.of(enrollmentOf(aluno)));

        AudienceResponse response = service.resolve("sec@sing.local", new AudienceRequest(null, Set.of(5L)));

        assertThat(response.author().role()).isEqualTo("ROLE_SECRETARY");
        assertThat(response.recipients()).extracting(AudienceResponse.Recipient::userId).containsExactly(7L);
    }
}
