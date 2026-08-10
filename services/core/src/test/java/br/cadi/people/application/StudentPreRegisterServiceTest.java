package br.cadi.people.application;

import br.cadi.academic.domain.entities.Workshop;
import br.cadi.academic.infra.repositories.WorkshopRepository;
import br.cadi.communication.application.NotificationService;
import br.cadi.communication.application.ResilientMessagingService;
import br.cadi.iam.infra.UserRepository;
import br.cadi.people.api.dto.request.StudentPreRegisterCreateRequest;
import br.cadi.people.domain.entities.StudentPreRegister;
import br.cadi.people.domain.enums.Shift;
import br.cadi.people.infra.repositories.StudentPreRegisterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentPreRegisterServiceTest {

    @Mock
    private StudentPreRegisterRepository studentPreRegisterRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private WaitingListVacancyQueryService vacancyQueryService;
    @Mock
    private ResilientMessagingService messagingService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private StudentPreRegisterService service;

    private Workshop workshop;

    @BeforeEach
    void setUp() {
        service = new StudentPreRegisterService(
                studentPreRegisterRepository,
                workshopRepository,
                vacancyQueryService,
                messagingService,
                userRepository,
                notificationService
        );
        workshop = Workshop.builder().id(1L).name("Oficina Teste").isActive(true).build();
    }

    @Test
    void create_deveNotificarQuandoJaExistirVagaCompativel() {
        StudentPreRegisterCreateRequest request = new StudentPreRegisterCreateRequest(
                "Maria Silva",
                "maria@email.com",
                "41999998888",
                1L,
                Shift.MORNING
        );
        StudentPreRegister saved = StudentPreRegister.builder()
                .id(10L)
                .name("Maria Silva")
                .email("maria@email.com")
                .phone("41999998888")
                .workshop(workshop)
                .shift(Shift.MORNING)
                .build();

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(studentPreRegisterRepository.save(any(StudentPreRegister.class))).thenReturn(saved);
        when(vacancyQueryService.countAvailableVacanciesForWorkshopShift(1L, Shift.MORNING)).thenReturn(2L);
        when(studentPreRegisterRepository.countByWorkshop_IdAndShift(1L, Shift.MORNING)).thenReturn(1L);
        when(vacancyQueryService.hasVacancyForWorkshopShift(1L, Shift.MORNING)).thenReturn(true);

        service.create(request);

        verify(notificationService).createWaitingListVacancyIfAbsent(
                eq(1L),
                eq("Oficina Teste"),
                eq(Shift.MORNING),
                eq(2L),
                eq(1L)
        );
    }

    @Test
    void create_naoDeveNotificarQuandoNaoExistirVagaCompativel() {
        StudentPreRegisterCreateRequest request = new StudentPreRegisterCreateRequest(
                "João Souza",
                "joao@email.com",
                "41988887777",
                1L,
                Shift.AFTERNOON
        );
        StudentPreRegister saved = StudentPreRegister.builder()
                .id(11L)
                .name("João Souza")
                .email("joao@email.com")
                .phone("41988887777")
                .workshop(workshop)
                .shift(Shift.AFTERNOON)
                .build();

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(studentPreRegisterRepository.save(any(StudentPreRegister.class))).thenReturn(saved);
        when(vacancyQueryService.countAvailableVacanciesForWorkshopShift(1L, Shift.AFTERNOON)).thenReturn(0L);
        when(vacancyQueryService.hasVacancyForWorkshopShift(1L, Shift.AFTERNOON)).thenReturn(false);

        service.create(request);

        verify(notificationService, never()).createWaitingListVacancyIfAbsent(
                any(), any(), any(), any(Long.class), any(Long.class)
        );
    }

    @Test
    void create_devePersistirPreCadastroAntesDeNotificar() {
        StudentPreRegisterCreateRequest request = new StudentPreRegisterCreateRequest(
                "Ana Lima",
                "ana@email.com",
                "41977776666",
                1L,
                Shift.EVENING
        );

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(studentPreRegisterRepository.save(any(StudentPreRegister.class))).thenAnswer(inv -> {
            StudentPreRegister entity = inv.getArgument(0);
            entity.setId(12L);
            return entity;
        });
        when(vacancyQueryService.countAvailableVacanciesForWorkshopShift(1L, Shift.EVENING)).thenReturn(1L);
        when(studentPreRegisterRepository.countByWorkshop_IdAndShift(1L, Shift.EVENING)).thenReturn(1L);
        when(vacancyQueryService.hasVacancyForWorkshopShift(1L, Shift.EVENING)).thenReturn(true);

        var response = service.create(request);

        ArgumentCaptor<StudentPreRegister> captor = ArgumentCaptor.forClass(StudentPreRegister.class);
        verify(studentPreRegisterRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ana Lima");
        assertThat(response.name()).isEqualTo("Ana Lima");
        verify(notificationService).createWaitingListVacancyIfAbsent(
                eq(1L),
                eq("Oficina Teste"),
                eq(Shift.EVENING),
                eq(1L),
                eq(1L)
        );
    }
}
