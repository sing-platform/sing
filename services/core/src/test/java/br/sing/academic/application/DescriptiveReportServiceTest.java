package br.sing.academic.application;

import br.sing.academic.api.dto.request.DescriptiveReportUpsertRequest;
import br.sing.academic.api.dto.response.DescriptiveReportResponse;
import br.sing.academic.domain.entities.*;
import br.sing.academic.domain.enums.DescriptiveReportStatus;
import br.sing.academic.infra.repositories.*;
import br.sing.exception.BusinessException;
import br.sing.people.domain.entities.Collaborator;
import br.sing.people.domain.entities.Student;
import br.sing.people.domain.enums.CollaboratorType;
import br.sing.people.infra.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescriptiveReportServiceTest {

    @Mock
    private DescriptiveReportRepository descriptiveReportRepository;
    @Mock
    private DescriptiveReportHistoryRepository descriptiveReportHistoryRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchoolRoomRepository schoolRoomRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AcademicAuthorizationService academicAuthorizationService;
    @Mock
    private Authentication authentication;

    private DescriptiveReportService service;
    private Student student;
    private SchoolRoom schoolRoom;
    private Collaborator professor;

    @BeforeEach
    void setUp() {
        service = new DescriptiveReportService(
                descriptiveReportRepository,
                descriptiveReportHistoryRepository,
                studentRepository,
                schoolRoomRepository,
                enrollmentRepository,
                academicAuthorizationService
        );

        professor = Collaborator.builder()
                .id(10L)
                .name("Professor A")
                .cpf("12345678900")
                .email("prof@sing.local")
                .phone("11911111111")
                .birthDate(LocalDate.of(1990, 1, 1))
                .educationLevel("SUPERIOR")
                .type(CollaboratorType.TEACHER)
                .build();

        student = Student.builder()
                .id(20L)
                .name("Aluno B")
                .cpf("12345678901")
                .email("aluno@sing.local")
                .phone("11922222222")
                .birthDate(LocalDate.of(2010, 1, 1))
                .grade("6A")
                .currentSchool("Escola X")
                .periodStudies("VESPERTINO")
                .build();

        Workshop workshop = Workshop.builder().id(30L).name("Robótica").isActive(true).build();
        schoolRoom = SchoolRoom.builder()
                .id(40L)
                .name("Turma 6A")
                .isActive(true)
                .classroom("Sala 1")
                .workshop(workshop)
                .teacher(professor)
                .scheduleSlots(List.of())
                .build();
    }

    @Test
    void deveCriarParecerQuandoProfessorAutorizado() {
        DescriptiveReportUpsertRequest request = new DescriptiveReportUpsertRequest(
                20L, 40L, "Bom desempenho no período."
        );

        when(studentRepository.findById(20L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(40L)).thenReturn(Optional.of(schoolRoom));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(20L, 40L)).thenReturn(true);
        when(descriptiveReportRepository.existsByStudent_IdAndSchoolRoom_Id(20L, 40L))
                .thenReturn(false);
        when(academicAuthorizationService.isManagement(authentication)).thenReturn(false);
        when(academicAuthorizationService.getCollaboratorOrThrow(authentication)).thenReturn(professor);
        when(descriptiveReportRepository.save(any(DescriptiveReport.class)))
                .thenAnswer(invocation -> {
                    DescriptiveReport saved = invocation.getArgument(0);
                    saved.setId(100L);
                    return saved;
                });

        DescriptiveReportResponse response = service.create(request, authentication);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.studentId()).isEqualTo(20L);
        assertThat(response.schoolRoomId()).isEqualTo(40L);
        assertThat(response.status()).isEqualTo(DescriptiveReportStatus.DRAFT);
        verify(descriptiveReportHistoryRepository).save(any(DescriptiveReportHistory.class));
    }

    @Test
    void deveBloquearEdicaoQuandoPublicado() {
        DescriptiveReport published = DescriptiveReport.builder()
                .id(101L)
                .student(student)
                .schoolRoom(schoolRoom)
                .author(professor)
                .content("Texto antigo")
                .status(DescriptiveReportStatus.PUBLISHED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .publishedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(descriptiveReportRepository.findByIdWithDetails(101L)).thenReturn(Optional.of(published));
        when(academicAuthorizationService.isManagement(authentication)).thenReturn(false);
        when(academicAuthorizationService.getCollaboratorOrThrow(authentication)).thenReturn(professor);

        DescriptiveReportUpsertRequest request = new DescriptiveReportUpsertRequest(
                20L, 40L, "Novo texto"
        );

        assertThatThrownBy(() -> service.update(101L, request, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("publicado ou fechado");

        verify(descriptiveReportRepository, never()).save(any(DescriptiveReport.class));
    }

    @Test
    void deveFiltrarPareceresPorTurmaNoEscopoProfessor() {
        DescriptiveReport report = DescriptiveReport.builder()
                .id(102L)
                .student(student)
                .schoolRoom(schoolRoom)
                .author(professor)
                .content("Texto")
                .status(DescriptiveReportStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(academicAuthorizationService.isProfessor(authentication)).thenReturn(true);
        when(academicAuthorizationService.isManagement(authentication)).thenReturn(false);
        when(academicAuthorizationService.getCollaboratorOrThrow(authentication)).thenReturn(professor);
        when(descriptiveReportRepository.findAllWithFiltersForTeacher(
                eq(10L), eq(40L), isNull(), isNull(), isNull()
        )).thenReturn(List.of(report));

        List<DescriptiveReportResponse> result = service.list(
                40L, null, null, null, authentication
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(102L);

        verify(descriptiveReportRepository).findAllWithFiltersForTeacher(
                10L, 40L, null, null, null
        );
        verify(descriptiveReportRepository, never()).findAllWithFilters(any(), any(), any(), any());
    }
}
