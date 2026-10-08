package br.sing.academic.application;

import br.sing.academic.domain.entities.SchoolRoom;
import br.sing.academic.domain.entities.Workshop;
import br.sing.academic.infra.repositories.EnrollmentRepository;
import br.sing.academic.infra.repositories.SchoolRoomRepository;
import br.sing.academic.infra.repositories.WorkshopRepository;
import br.sing.exception.BusinessException;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.infra.repositories.CollaboratorRepository;
import br.sing.people.infra.repositories.PersonRepository;
import br.sing.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolRoomServicePageTeacherTest {

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
    private Collaborator teacher;

    @BeforeEach
    void setUp() {
        var auth = new AcademicAuthorizationService(personRepository);
        schoolRoomService = new SchoolRoomService(
                schoolRoomRepository, workshopRepository, collaboratorRepository, enrollmentRepository, auth,
                schoolRoomVacancyService
        );
        teacher = TestDataFactory.teacher(10L, "prof@sing.local");
    }

    @Test
    void professorComOutroTeacherIdDeveReceber403() {
        when(personRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        var auth = new TestingAuthenticationToken("prof@sing.local", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        assertThatThrownBy(() -> schoolRoomService.pageSchoolRoomsForTeacher(99L, true, PageRequest.of(0, 20), auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void professorDoProprioTeacherIdConsultaRepositorioComFiltroAtivo() {
        Workshop workshop = TestDataFactory.workshop(1L, "Oficina", true);
        SchoolRoom room = SchoolRoom.builder().id(1L).name("Turma A").isActive(true).workshop(workshop).build();
        Pageable incoming = PageRequest.of(0, 20, Sort.by("unknownField"));
        Pageable normalized = PageRequest.of(0, 20, Sort.by("name").ascending());
        Page<SchoolRoom> page = new PageImpl<>(List.of(room), normalized, 1);
        when(personRepository.findByEmail("prof@sing.local")).thenReturn(Optional.of(teacher));
        when(schoolRoomRepository.pageByTeacherIdAndActiveOptional(eq(10L), eq(Boolean.TRUE), any(Pageable.class)))
                .thenReturn(page);
        when(enrollmentRepository.countActiveEnrollmentsGroupedBySchoolRoomId(anyList())).thenReturn(List.of());
        var auth = new TestingAuthenticationToken("prof@sing.local", null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR")));

        var result = schoolRoomService.pageSchoolRoomsForTeacher(10L, true, incoming, auth);

        assertThat(result.getContent()).hasSize(1);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(schoolRoomRepository).pageByTeacherIdAndActiveOptional(eq(10L), eq(Boolean.TRUE), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("name")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("unknownField")).isNull();
    }

    @Test
    void secretariaPodeConsultarSemFiltroDeAtivoQuandoActiveOnlyFalse() {
        Workshop workshop = TestDataFactory.workshop(1L, "Oficina", true);
        SchoolRoom room = SchoolRoom.builder().id(1L).name("Turma A").isActive(false).workshop(workshop).build();
        Page<SchoolRoom> page = new PageImpl<>(List.of(room), PageRequest.of(0, 20), 1);
        var auth = new TestingAuthenticationToken("sec@sing.local", null,
                List.of(new SimpleGrantedAuthority("ROLE_SECRETARY")));
        when(schoolRoomRepository.pageByTeacherIdAndActiveOptional(eq(10L), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(enrollmentRepository.countActiveEnrollmentsGroupedBySchoolRoomId(anyList())).thenReturn(List.of());

        var result = schoolRoomService.pageSchoolRoomsForTeacher(10L, false, PageRequest.of(0, 20), auth);

        assertThat(result.getContent()).hasSize(1);
        verify(schoolRoomRepository).pageByTeacherIdAndActiveOptional(eq(10L), isNull(), any(Pageable.class));
    }
}
