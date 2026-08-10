package br.cadi.academic.application;

import br.cadi.academic.domain.entities.*;
import br.cadi.academic.domain.enums.AttendanceStatus;
import br.cadi.academic.api.dto.request.AttendanceItemRequest;
import br.cadi.academic.api.dto.request.BatchAttendanceRequest;
import br.cadi.academic.infra.repositories.*;
import br.cadi.exception.BusinessRuleException;
import br.cadi.exception.ForbiddenException;
import br.cadi.exception.ResourceNotFoundException;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.domain.enums.CollaboratorType;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService — testes unitários")
class AttendanceServiceTest {

    @Mock private LectureRepository lectureRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private SchoolRoomRepository schoolRoomRepository;
    @Mock private SchoolRoomScheduleSlotRepository scheduleSlotRepository;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    @InjectMocks
    private AttendanceService attendanceService;

    // ── Fixtures ────────────────────────────────────────────────────────────

    private SchoolRoom schoolRoom;
    private Lecture lecture;
    private Student student;
    private Enrollment enrollment;
    private Collaborator teacher;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        lenient().when(scheduleSlotRepository.existsBySchoolroomIdAndDayOfWeek(anyLong(), any(DayOfWeek.class)))
                .thenReturn(true);

        schoolRoom = SchoolRoom.builder()
                .id(1L)
                .name("Turma A")
                .build();

        lecture = Lecture.builder()
                .id(10L)
                .schoolroom(schoolRoom)
                .dateTime(LocalDateTime.now())
                .topic("Aula do dia")
                .closed(false)
                .build();

        student = new Student();
        student.setId(100L);
        student.setName("Lucas Souza");

        enrollment = Enrollment.builder()
                .id(1L)
                .student(student)
                .schoolroom(schoolRoom)
                .enrollmentDate(LocalDate.now())
                .status(true)
                .build();

        teacher = Collaborator.builder()
                .id(50L)
                .name("Prof Ana")
                .email("ana@cadi.com")
                .cpf("12345678900")
                .phone("41999999999")
                .birthDate(LocalDate.of(1985, 3, 10))
                .educationLevel("Superior")
                .type(CollaboratorType.TEACHER)
                .build();
    }

    // ── Helpers de mock de auth ─────────────────────────────────────────────


    private void mockAsManagement() {
        var auth = new UsernamePasswordAuthenticationToken(
                "user",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SECRETARY"))
        );
        when(securityContext.getAuthentication()).thenReturn(auth);
    }

    private void mockAsAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(securityContext.getAuthentication()).thenReturn(auth);
    }

    private void mockAsTeacher() {
        var auth = new UsernamePasswordAuthenticationToken(
                "ana@cadi.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))
        );

        when(securityContext.getAuthentication()).thenReturn(auth);

    }

    private void mockAsUnknownRole() {
        var auth = new UsernamePasswordAuthenticationToken(
                "student",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        when(securityContext.getAuthentication()).thenReturn(auth);
    }

    // ── registerBatch ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerBatch")
    class RegisterBatch {

        @Test
        @DisplayName("deve persistir presenças com sucesso quando dados válidos e chamada aberta")
        void devePersistirPresencasComSucesso() {
            mockAsManagement();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.of(lecture));
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of(enrollment));
            when(attendanceRepository.findByLectureIdAndStudentId(10L, 100L))
                    .thenReturn(Optional.empty());

            var request = new BatchAttendanceRequest(
                    1L,
                    LocalDate.now(),
                    "Tópico teste",
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            assertThatNoException().isThrownBy(() -> attendanceService.registerBatch(request));
            verify(attendanceRepository, times(1)).save(any(Attendance.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando chamada já está fechada")
        void deveLancarExcecaoQuandoChamadaFechada() {
            mockAsManagement();
            lecture.setClosed(true);
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.of(lecture));

            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now(), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            assertThatThrownBy(() -> attendanceService.registerBatch(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("fechada");
        }

        @Test
        @DisplayName("deve lançar exceção quando data é futura")
        void deveLancarExcecaoQuandoDataFutura() {
            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now().plusDays(1), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            assertThatThrownBy(() -> attendanceService.registerBatch(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("futura");
        }

        @Test
        @DisplayName("deve lançar exceção quando data é anterior ao limite de 35 dias")
        void deveLancarExcecaoQuandoDataMuitoAntiga() {
            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now().minusDays(36), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            assertThatThrownBy(() -> attendanceService.registerBatch(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("35 dias");
        }

        @Test
        @DisplayName("deve lançar exceção quando aluno não está matriculado na turma")
        void deveLancarExcecaoQuandoAlunoNaoMatriculado() {
            mockAsManagement();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.of(lecture));
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of()); // nenhum aluno matriculado

            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now(), null,
                    List.of(new AttendanceItemRequest(999L, AttendanceStatus.PRESENT, null))
            );

            assertThatThrownBy(() -> attendanceService.registerBatch(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("não está matriculado");
        }

        @Test
        @DisplayName("deve atualizar attendance existente em vez de criar novo")
        void deveAtualizarAttendanceExistente() {
            mockAsManagement();
            var existingAttendance = Attendance.builder()
                    .id(200L)
                    .lecture(lecture)
                    .student(student)
                    .status(AttendanceStatus.ABSENT)
                    .present(false)
                    .build();

            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.of(lecture));
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of(enrollment));
            when(attendanceRepository.findByLectureIdAndStudentId(10L, 100L))
                    .thenReturn(Optional.of(existingAttendance));

            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now(), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, "Obs atualizada"))
            );

            assertThatNoException().isThrownBy(() -> attendanceService.registerBatch(request));

            assertThat(existingAttendance.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
            assertThat(existingAttendance.getPresent()).isTrue();
            assertThat(existingAttendance.getObservation()).isEqualTo("Obs atualizada");
            verify(attendanceRepository, times(1)).save(existingAttendance);
        }

        @Test
        @DisplayName("deve lançar ForbiddenException quando professor tenta acessar turma de outro professor")
        void deveLancarForbiddenQuandoProfessorSemPermissao() {
            mockAsTeacher();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.of(lecture));
            when(collaboratorRepository.findByEmail("ana@cadi.com"))
                    .thenReturn(Optional.of(teacher));
            when(lectureRepository.isTeacherOfLecture(10L, 50L)).thenReturn(false);

            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now(), null,
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            assertThatThrownBy(() -> attendanceService.registerBatch(request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("deve criar lecture automaticamente quando não existe para o dia")
        void deveCriarLectureQuandoNaoExiste() {
            mockAsManagement();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.empty()); // não existe lecture
            when(lectureRepository.save(any(Lecture.class))).thenReturn(lecture);
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of(enrollment));
            when(attendanceRepository.findByLectureIdAndStudentId(any(), eq(100L)))
                    .thenReturn(Optional.empty());

            var request = new BatchAttendanceRequest(
                    1L, LocalDate.now(), "Novo tópico",
                    List.of(new AttendanceItemRequest(100L, AttendanceStatus.PRESENT, null))
            );

            assertThatNoException().isThrownBy(() -> attendanceService.registerBatch(request));
            verify(lectureRepository, times(1)).save(any(Lecture.class));
        }
    }

    // ── closeCall ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("closeCall")
    class CloseCall {

        @Test
        @DisplayName("deve fechar chamada com sucesso quando usuário tem permissão")
        void deveFecharChamadaComSucesso() {
            mockAsManagement();
            when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));

            assertThatNoException().isThrownBy(() -> attendanceService.closeCall(10L));
            assertThat(lecture.getClosed()).isTrue();
            verify(lectureRepository).save(lecture);
        }

        @Test
        @DisplayName("deve lançar exceção quando chamada já está fechada")
        void deveLancarExcecaoQuandoJaFechada() {
            mockAsManagement();
            lecture.setClosed(true);
            when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));

            assertThatThrownBy(() -> attendanceService.closeCall(10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("fechada");
        }

        @Test
        @DisplayName("deve lançar ForbiddenException quando role sem permissão tenta fechar")
        void deveLancarForbiddenQuandoSemPermissao() {
            mockAsUnknownRole();
            when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));

            assertThatThrownBy(() -> attendanceService.closeCall(10L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando lecture não existe")
        void deveLancarNotFoundQuandoLectureInexistente() {
            when(lectureRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> attendanceService.closeCall(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── reopenCall ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reopenCall")
    class ReopenCall {

        @Test
        @DisplayName("deve reabrir chamada com sucesso quando usuário é admin ou secretário")
        void deveReabrirChamadaComSucesso() {
            mockAsAdmin();
            lecture.setClosed(true);
            when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));

            assertThatNoException().isThrownBy(() -> attendanceService.reopenCall(10L));
            assertThat(lecture.getClosed()).isFalse();
            verify(lectureRepository).save(lecture);
        }

        @Test
        @DisplayName("deve lançar exceção quando chamada já está aberta")
        void deveLancarExcecaoQuandoJaAberta() {
            mockAsAdmin();
            when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture)); // closed = false

            assertThatThrownBy(() -> attendanceService.reopenCall(10L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("aberta");
        }

        @Test
        @DisplayName("deve lançar ForbiddenException quando professor tenta reabrir")
        void deveLancarForbiddenQuandoProfessorTentaReabrir() {
            mockAsTeacher();
            when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));

            assertThatThrownBy(() -> attendanceService.reopenCall(10L))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── startCall ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("startCall")
    class StartCall {

        @Test
        @DisplayName("deve retornar lista de alunos com status null quando não há chamada no dia")
        void deveRetornarAlunosSemStatusQuandoSemChamada() {
            mockAsManagement();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.empty());
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of(enrollment));

            var result = attendanceService.startCall(1L, LocalDate.now());

            assertThat(result.attendances()).hasSize(1);
            assertThat(result.attendances().get(0).status()).isNull();
            assertThat(result.closed()).isFalse();
        }

        @Test
        @DisplayName("deve retornar status dos alunos quando chamada já existe")
        void deveRetornarStatusQuandoChamadaExiste() {
            mockAsManagement();
            var attendance = Attendance.builder()
                    .id(200L).lecture(lecture).student(student)
                    .status(AttendanceStatus.PRESENT).present(true).build();

            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.of(lecture));
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of(enrollment));
            when(attendanceRepository.findByLectureIdAndStudentId(10L, 100L))
                    .thenReturn(Optional.of(attendance));

            var result = attendanceService.startCall(1L, LocalDate.now());

            assertThat(result.attendances().get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
        }

        @Test
        @DisplayName("deve lançar exceção quando turma não tem alunos matriculados")
        void deveLancarExcecaoQuandoSemAlunos() {
            mockAsManagement();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(lectureRepository.findBySchoolroomAndDate(eq(1L), any(), any()))
                    .thenReturn(Optional.empty());
            when(enrollmentRepository.findBySchoolroomIdAndStatusTrue(1L))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> attendanceService.startCall(1L, LocalDate.now()))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("deve lançar ForbiddenException quando professor tenta ver turma de outro professor")
        void deveLancarForbiddenQuandoProfessorSemPermissaoNoStartCall() {
            mockAsTeacher();
            when(schoolRoomRepository.findById(1L)).thenReturn(Optional.of(schoolRoom));
            when(collaboratorRepository.findByEmail("ana@cadi.com"))
                    .thenReturn(Optional.of(teacher));
            // schoolRoom.teacher é null → professor não é dono
            assertThatThrownBy(() -> attendanceService.startCall(1L, LocalDate.now()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("deve lançar exceção quando data é futura no startCall")
        void deveLancarExcecaoParaDataFuturaNoStartCall() {
            assertThatThrownBy(() -> attendanceService.startCall(1L, LocalDate.now().plusDays(1)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("futura");
        }
    }

    // ── applyStatus ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Attendance.applyStatus — sincronização do campo present")
    class ApplyStatus {

        @Test
        @DisplayName("PRESENT deve setar present = true")
        void presentDeveSetarPresentTrue() {
            var a = Attendance.builder().build();
            a.applyStatus(AttendanceStatus.PRESENT);
            assertThat(a.getPresent()).isTrue();
            assertThat(a.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        }

        @Test
        @DisplayName("ABSENT deve setar present = false")
        void absentDeveSetarPresentFalse() {
            var a = Attendance.builder().build();
            a.applyStatus(AttendanceStatus.ABSENT);
            assertThat(a.getPresent()).isFalse();
        }

        @Test
        @DisplayName("JUSTIFIED deve setar present = false")
        void justifiedDeveSetarPresentFalse() {
            var a = Attendance.builder().build();
            a.applyStatus(AttendanceStatus.JUSTIFIED);
            assertThat(a.getPresent()).isFalse();
        }
    }
}
