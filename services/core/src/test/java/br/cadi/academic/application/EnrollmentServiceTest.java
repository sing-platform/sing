package br.cadi.academic.application;

import br.cadi.academic.api.dto.request.EnrollmentRequest;
import br.cadi.academic.api.dto.response.EnrollmentPreviewResponse;
import br.cadi.academic.api.dto.response.EnrollmentSuggestedAction;
import br.cadi.academic.domain.entities.Enrollment;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.domain.entities.SchoolRoomScheduleSlot;
import br.cadi.academic.domain.entities.Workshop;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.exception.BusinessException;
import br.cadi.people.domain.entities.Student;
import br.cadi.people.infra.repositories.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private SchoolRoomRepository schoolRoomRepository;
    @Mock
    private SchoolRoomVacancyService schoolRoomVacancyService;

    private EnrollmentService enrollmentService;

    private Student student;
    private Workshop w1;
    private Workshop w2;
    private Workshop w3;
    private SchoolRoom roomA;
    private SchoolRoom roomB;
    private SchoolRoom roomC;
    private SchoolRoom roomD;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository, studentRepository, schoolRoomRepository, schoolRoomVacancyService);
        w1 = Workshop.builder().id(1L).name("Oficina 1").isActive(true).build();
        w2 = Workshop.builder().id(2L).name("Oficina 2").isActive(true).build();
        w3 = Workshop.builder().id(3L).name("Oficina 3").isActive(true).build();
        student = Student.builder()
                .id(50L)
                .name("Aluno")
                .cpf("12345678901")
                .email("a@a.com")
                .phone("11999999999")
                .birthDate(java.time.LocalDate.of(2010, 1, 1))
                .grade("1-ef")
                .currentSchool("Escola X")
                .periodStudies("NOTURNO")
                .build();
        roomA = room(10L, "Turma A", w1, slot(DayOfWeek.MONDAY, 14, 16));
        roomB = room(11L, "Turma B", w1, slot(DayOfWeek.MONDAY, 16, 18));
        roomC = room(20L, "Turma C", w2, slot(DayOfWeek.TUESDAY, 14, 16));
        roomD = room(21L, "Turma D", w3, slot(DayOfWeek.WEDNESDAY, 14, 16));
    }

    private static SchoolRoomScheduleSlot slot(DayOfWeek d, int h1, int h2) {
        return SchoolRoomScheduleSlot.builder()
                .dayOfWeek(d)
                .startTime(LocalTime.of(h1, 0))
                .endTime(LocalTime.of(h2, 0))
                .build();
    }

    private SchoolRoom room(long id, String name, Workshop w, SchoolRoomScheduleSlot... slots) {
        List<SchoolRoomScheduleSlot> list = new ArrayList<>(List.of(slots));
        SchoolRoom sr = SchoolRoom.builder()
                .id(id)
                .name(name)
                .isActive(true)
                .classroom("S1")
                .capacity(30)
                .workshop(w)
                .scheduleSlots(list)
                .build();
        for (SchoolRoomScheduleSlot s : list) {
            s.setSchoolroom(sr);
        }
        return sr;
    }

    @Test
    void deveMatricularQuandoSemConflitos() {
        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(20L)).thenReturn(Optional.of(roomC));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 20L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of());
        when(enrollmentRepository.countBySchoolroomIdAndStatusTrue(20L)).thenReturn(5L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(20L)));

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void deveRejeitarTerceiraTurma() {
        Enrollment e1 = Enrollment.builder().id(1L).status(true).student(student).schoolroom(roomA).enrollmentDate(java.time.LocalDate.now()).build();
        Enrollment e2 = Enrollment.builder().id(2L).status(true).student(student).schoolroom(roomC).enrollmentDate(java.time.LocalDate.now()).build();

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(21L)).thenReturn(Optional.of(roomD));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 21L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of(e1, e2));

        assertThatThrownBy(() -> enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(21L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duas turmas");
    }

    @Test
    void remanejamentoMesmaOficinaEncerraMatriculaAnterior() {
        Enrollment eA = Enrollment.builder().id(1L).status(true).student(student).schoolroom(roomA).enrollmentDate(java.time.LocalDate.now()).build();

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(11L)).thenReturn(Optional.of(roomB));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 11L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of(eA));
        when(enrollmentRepository.countBySchoolroomIdAndStatusTrue(11L)).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(11L)));

        assertThat(eA.getStatus()).isFalse();
        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().stream().anyMatch(e -> Boolean.TRUE.equals(e.getStatus()) && e.getSchoolroom().equals(roomB)))
                .isTrue();
    }

    @Test
    void deveRejeitarConflitoDeHorario() {
        SchoolRoom roomOverlap = room(30L, "Turma X", w2, slot(DayOfWeek.MONDAY, 15, 17));
        Enrollment eA = Enrollment.builder().id(1L).status(true).student(student).schoolroom(roomA).enrollmentDate(java.time.LocalDate.now()).build();

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(30L)).thenReturn(Optional.of(roomOverlap));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 30L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of(eA));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(10L)).thenReturn(Optional.of(roomA));

        assertThatThrownBy(() -> enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(30L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("conflita");
    }

    @Test
    void deveRejeitarMatutinoComSlotsManha() {
        student.setPeriodStudies("MATUTINO");
        SchoolRoom manha = room(40L, "Turma Manha", w2, slot(DayOfWeek.WEDNESDAY, 8, 10));

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(40L)).thenReturn(Optional.of(manha));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 40L)).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(40L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("escola principal");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void loteDuasTurmasSemConflitoPersisteDuasVezes() {
        Enrollment aposPrimeira = Enrollment.builder()
                .id(99L)
                .status(true)
                .student(student)
                .schoolroom(roomC)
                .enrollmentDate(java.time.LocalDate.now())
                .build();

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(20L)).thenReturn(Optional.of(roomC));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(21L)).thenReturn(Optional.of(roomD));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 20L)).thenReturn(false);
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 21L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L))
                .thenReturn(List.of())
                .thenReturn(List.of(aposPrimeira));
        when(enrollmentRepository.countBySchoolroomIdAndStatusTrue(20L)).thenReturn(0L);
        when(enrollmentRepository.countBySchoolroomIdAndStatusTrue(21L)).thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> i.getArgument(0));

        enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(20L, 21L)));

        verify(enrollmentRepository, times(2)).save(any(Enrollment.class));
    }

    @Test
    void loteRejeitaDuplicatasNaLista() {
        assertThatThrownBy(() -> enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(20L, 20L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duplicata");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void loteRejeitaConflitoEntreAsDuasTurmasSemPersistir() {
        SchoolRoom roomManha1 = room(31L, "Turma M1", w2, slot(DayOfWeek.MONDAY, 14, 16));
        SchoolRoom roomManha2 = room(32L, "Turma M2", w3, slot(DayOfWeek.MONDAY, 15, 17));

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(31L)).thenReturn(Optional.of(roomManha1));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(32L)).thenReturn(Optional.of(roomManha2));
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of());

        assertThatThrownBy(() -> enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(31L, 32L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Matrícula em lote inviável")
                .hasMessageContaining("conflito de horário");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void loteRejeitaQuandoAlunoJaTemDuasTurmasSemPersistir() {
        Enrollment e1 = Enrollment.builder().id(1L).status(true).student(student).schoolroom(roomA).enrollmentDate(java.time.LocalDate.now()).build();
        Enrollment e2 = Enrollment.builder().id(2L).status(true).student(student).schoolroom(roomC).enrollmentDate(java.time.LocalDate.now()).build();

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(21L)).thenReturn(Optional.of(roomD));
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of(e1, e2));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 21L)).thenReturn(false);

        assertThatThrownBy(() -> enrollmentService.enrollStudent(new EnrollmentRequest(50L, List.of(21L))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("duas turmas");

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void previewListaDuplicadaNaoConsultaAluno() {
        EnrollmentPreviewResponse r = enrollmentService.previewEnrollment(new EnrollmentRequest(50L, List.of(20L, 20L)));

        assertThat(r.ok()).isFalse();
        assertThat(r.issues().getFirst().code()).isEqualTo("DUPLICATE_ROOM_IDS");
        verify(studentRepository, never()).findById(any());
    }

    @Test
    void previewUmaTurmaOkSugereEnroll() {
        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(20L)).thenReturn(Optional.of(roomC));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 20L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of());
        when(enrollmentRepository.countBySchoolroomIdAndStatusTrue(20L)).thenReturn(0L);

        EnrollmentPreviewResponse r = enrollmentService.previewEnrollment(new EnrollmentRequest(50L, List.of(20L)));

        assertThat(r.ok()).isTrue();
        assertThat(r.suggestedAction()).isEqualTo(EnrollmentSuggestedAction.ENROLL);
    }

    @Test
    void previewRemanejamentoSugereTransfer() {
        Enrollment eA = Enrollment.builder().id(1L).status(true).student(student).schoolroom(roomA).enrollmentDate(java.time.LocalDate.now()).build();

        when(studentRepository.findById(50L)).thenReturn(Optional.of(student));
        when(schoolRoomRepository.findByIdWithScheduleSlotsAndTeacher(11L)).thenReturn(Optional.of(roomB));
        when(enrollmentRepository.existsByStudentIdAndSchoolroomIdAndStatusTrue(50L, 11L)).thenReturn(false);
        when(enrollmentRepository.findActiveByStudentWithRoomAndWorkshop(50L)).thenReturn(List.of(eA));
        when(enrollmentRepository.countBySchoolroomIdAndStatusTrue(11L)).thenReturn(0L);

        EnrollmentPreviewResponse r = enrollmentService.previewEnrollment(new EnrollmentRequest(50L, List.of(11L)));

        assertThat(r.ok()).isTrue();
        assertThat(r.suggestedAction()).isEqualTo(EnrollmentSuggestedAction.TRANSFER);
    }
}