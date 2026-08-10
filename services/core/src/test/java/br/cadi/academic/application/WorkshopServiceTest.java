package br.cadi.academic.application;

import br.cadi.academic.api.dto.request.WorkshopCreateRequest;
import br.cadi.academic.api.dto.request.WorkshopUpdateRequest;
import br.cadi.academic.domain.entities.SchoolRoom;
import br.cadi.academic.domain.entities.Workshop;
import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.academic.infra.repositories.WorkshopRepository;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkshopServiceTest {

    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private SchoolRoomRepository schoolRoomRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private WorkshopService workshopService;

    @BeforeEach
    void setUp() {
        workshopService = new WorkshopService(workshopRepository, schoolRoomRepository, enrollmentRepository);
    }

    @Test
    void createReativaOficinaInativaComMesmoNome() {
        Workshop existing = TestDataFactory.workshop(1L, "Robotica", false);
        when(workshopRepository.findFirstByNameIgnoreCase("Robotica")).thenReturn(Optional.of(existing));
        when(workshopRepository.save(existing)).thenReturn(existing);

        var response = workshopService.create(new WorkshopCreateRequest(" Robotica "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.active()).isTrue();
        assertThat(existing.getName()).isEqualTo("Robotica");
        verify(workshopRepository).save(existing);
    }

    @Test
    void createDeveLancar409QuandoNomeJaExisteAtivo() {
        Workshop existing = TestDataFactory.workshop(1L, "Robotica", true);
        when(workshopRepository.findFirstByNameIgnoreCase("Robotica")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> workshopService.create(new WorkshopCreateRequest("Robotica")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(workshopRepository, never()).save(any());
    }

    @Test
    void updateDeveLancar404QuandoOficinaNaoExiste() {
        when(workshopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workshopService.update(99L, new WorkshopUpdateRequest("Nova")))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Oficina não encontrada");
    }

    @Test
    void updateDeveLancar409QuandoNomeDuplicado() {
        Workshop workshop = TestDataFactory.workshop(1L, "Arte", true);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(workshopRepository.existsByNameIgnoreCaseAndIdNot("Robotica", 1L)).thenReturn(true);

        assertThatThrownBy(() -> workshopService.update(1L, new WorkshopUpdateRequest("Robotica")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void previewDeactivationNaoPermiteQuandoTurmaAtivaTemMatriculaAtiva() {
        Workshop workshop = TestDataFactory.workshop(1L, "Oficina", true);
        SchoolRoom roomA = TestDataFactory.schoolRoom(10L, "Turma A", true);
        SchoolRoom roomB = TestDataFactory.schoolRoom(11L, "Turma B", false);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.findByWorkshop_Id(1L)).thenReturn(List.of(roomB, roomA));
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(10L)).thenReturn(true);
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(11L)).thenReturn(false);

        var response = workshopService.previewDeactivation(1L);

        assertThat(response.canProceed()).isFalse();
        assertThat(response.schoolRooms()).hasSize(2);
        assertThat(response.schoolRooms().getFirst().name()).isEqualTo("Turma A");
        assertThat(response.schoolRooms().getFirst().hasActiveEnrollments()).isTrue();
    }

    @Test
    void previewDeactivationPodeProsseguirQuandoNaoHaMatriculasAtivasEmTurmasAtivas() {
        Workshop workshop = TestDataFactory.workshop(1L, "Oficina", true);
        SchoolRoom roomA = TestDataFactory.schoolRoom(10L, "A Turma", true);
        SchoolRoom roomB = TestDataFactory.schoolRoom(11L, "B Turma", false);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.findByWorkshop_Id(1L)).thenReturn(List.of(roomB, roomA));
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(10L)).thenReturn(false);
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(11L)).thenReturn(true);

        var response = workshopService.previewDeactivation(1L);

        assertThat(response.canProceed()).isTrue();
        assertThat(response.schoolRooms()).extracting(r -> r.name()).containsExactly("A Turma", "B Turma");
    }

    @Test
    void deactivateWithCascadeDeveLancar409QuandoHaMatriculasAtivas() {
        Workshop workshop = TestDataFactory.workshop(1L, "Oficina", true);
        SchoolRoom roomA = TestDataFactory.schoolRoom(10L, "Turma A", true);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.findByWorkshop_Id(1L)).thenReturn(List.of(roomA));
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(10L)).thenReturn(true);

        assertThatThrownBy(() -> workshopService.deactivateWithCascade(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Turma A")
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deactivateWithCascadeDesativaOficinaETurmasAtivas() {
        Workshop workshop = TestDataFactory.workshop(1L, "Oficina", true);
        SchoolRoom activeRoom = TestDataFactory.schoolRoom(10L, "Ativa", true);
        SchoolRoom inactiveRoom = TestDataFactory.schoolRoom(11L, "Inativa", false);
        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(schoolRoomRepository.findByWorkshop_Id(1L)).thenReturn(List.of(activeRoom, inactiveRoom));
        when(enrollmentRepository.existsBySchoolroomIdAndStatusTrue(10L)).thenReturn(false);

        workshopService.deactivateWithCascade(1L);

        assertThat(workshop.isActive()).isFalse();
        assertThat(activeRoom.isActive()).isFalse();
        assertThat(inactiveRoom.isActive()).isFalse();
        verify(workshopRepository).save(workshop);
        verify(schoolRoomRepository).save(activeRoom);
        verify(schoolRoomRepository, never()).save(inactiveRoom);
    }
}
