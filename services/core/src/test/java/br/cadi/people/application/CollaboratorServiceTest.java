package br.cadi.people.application;

import br.cadi.exception.EntityNotFoundException;
import br.cadi.exception.UserCreationConflict;
import br.cadi.iam.domain.Role;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.RoleRepository;
import br.cadi.iam.infra.UserRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.people.api.dto.request.AddressRequest;
import br.cadi.people.api.dto.request.CollaboratorCreateRequest;
import br.cadi.people.api.dto.response.CollaboratorDetailResponse;
import br.cadi.people.domain.entities.Address;
import br.cadi.people.domain.entities.Collaborator;
import br.cadi.people.domain.enums.CollaboratorType;
import br.cadi.people.infra.repositories.AddressRepository;
import br.cadi.people.infra.repositories.CollaboratorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorServiceTest {

    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SchoolRoomRepository schoolRoomRepository;

    private CollaboratorService service;

    @BeforeEach
    void setUp() {
        service = new CollaboratorService(
                collaboratorRepository,
                addressRepository,
                userRepository,
                roleRepository,
                passwordEncoder,
                schoolRoomRepository
        );
    }

    private AddressRequest addressDto() {
        return new AddressRequest("12345-678", "Rua A", 100, "Centro", "São Paulo", "SP", "");
    }

    private CollaboratorCreateRequest validTeacherRequest() {
        return new CollaboratorCreateRequest(
                CollaboratorType.TEACHER,
                "Prof. João",
                "11122233344",
                "joao@cadi.com",
                "11999999999",
                LocalDate.of(1980, 1, 1),
                addressDto(),
                "Superior",
                "Licenciatura",
                "USP",
                2005,
                "Matemática"
        );
    }

    private Collaborator buildCollaborator(Long id, String email, CollaboratorType type, boolean active) {
        Address address = Address.builder()
                .id(1L).zipCode("12345-678").street("R")
                .number(1).neighborhood("C").city("SP").state("SP").build();
        Role role = Role.builder().id(2L).name("ROLE_PROFESSOR").build();
        User user = User.builder().id(99L).username(email).email(email)
                .password("x").role(role).isActive(active).build();

        Collaborator c = Collaborator.builder()
                .educationLevel("Superior")
                .type(type)
                .build();
        c.setId(id);
        c.setName("Colab " + id);
        c.setCpf("000.000.000-0" + (id % 10));
        c.setEmail(email);
        c.setPhone("119");
        c.setBirthDate(LocalDate.of(1980, 1, 1));
        c.setAddress(address);
        c.setUser(user);
        return c;
    }

    @Test
    void listAllEligibleTeachersDeveOrdenarPorNomeIgnorandoCase() {
        Collaborator t1 = buildCollaborator(1L, "b@cadi.com", CollaboratorType.TEACHER, true);
        t1.setName("Beatriz");
        Collaborator t2 = buildCollaborator(2L, "a@cadi.com", CollaboratorType.TEACHER, true);
        t2.setName("ana");

        when(collaboratorRepository.findAllEligibleSchoolRoomTeachersActive(CollaboratorType.TEACHER))
                .thenReturn(List.of(t1, t2));

        List<CollaboratorDetailResponse> response = service.listAll(true, CollaboratorType.TEACHER, true);

        assertThat(response).extracting(CollaboratorDetailResponse::name).containsExactly("ana", "Beatriz");
    }

    @Test
    void listAllIncluindoInativosDeveUsarRepositorioCorretoParaTeachersElegiveis() {
        when(collaboratorRepository
                .findAllEligibleSchoolRoomTeachersIncludingInactiveUsers(CollaboratorType.TEACHER))
                .thenReturn(List.of());

        service.listAll(false, CollaboratorType.TEACHER, true);

        verify(collaboratorRepository)
                .findAllEligibleSchoolRoomTeachersIncludingInactiveUsers(CollaboratorType.TEACHER);
        verify(collaboratorRepository, never())
                .findAllEligibleSchoolRoomTeachersActive(any());
    }

    @Test
    void listAllPorTipoDeveUsarFindAllByTypeFiltered() {
        when(collaboratorRepository.findAllByTypeFiltered(CollaboratorType.SECRETARY, true))
                .thenReturn(List.of());

        service.listAll(true, CollaboratorType.SECRETARY, false);

        verify(collaboratorRepository).findAllByTypeFiltered(CollaboratorType.SECRETARY, true);
    }

    @Test
    void getByIdDeveRetornarCollaboratorDetailResponse() {
        Collaborator c = buildCollaborator(7L, "p@cadi.com", CollaboratorType.TEACHER, true);
        when(collaboratorRepository.findByIdWithUserAndAddress(7L)).thenReturn(Optional.of(c));

        CollaboratorDetailResponse response = service.getById(7L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.email()).isEqualTo("p@cadi.com");
    }

    @Test
    void getByIdDeveLancar404QuandoNaoExiste() {
        when(collaboratorRepository.findByIdWithUserAndAddress(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Collaborator not found");
    }

    @Test
    void createDeveLancar409QuandoCpfDuplicado() {
        when(collaboratorRepository.existsByCpf("11122233344")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validTeacherRequest()))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("CPF already registered");

        verify(userRepository, never()).save(any());
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void createDeveLancar409QuandoEmailDuplicado() {
        when(collaboratorRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("joao@cadi.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validTeacherRequest()))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("E-mail already registered");
    }

    @Test
    void createDeveLancar404QuandoRoleNaoExiste() {
        when(collaboratorRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(collaboratorRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_PROFESSOR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(validTeacherRequest()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ROLE_PROFESSOR");
    }

    @Test
    void createDevePersistirUserAddressEColaborador() {
        when(collaboratorRepository.existsByCpf(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(collaboratorRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("ROLE_PROFESSOR"))
                .thenReturn(Optional.of(Role.builder().id(2L).name("ROLE_PROFESSOR").build()));
        when(passwordEncoder.encode("CADI#111")).thenReturn("$encoded$");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(50L);
            return u;
        });
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });
        when(collaboratorRepository.save(any(Collaborator.class))).thenAnswer(inv -> {
            Collaborator c = inv.getArgument(0);
            c.setId(77L);
            return c;
        });

        CollaboratorDetailResponse response = service.create(validTeacherRequest());

        assertThat(response.id()).isEqualTo(77L);
        assertThat(response.type()).isEqualTo(CollaboratorType.TEACHER);
        assertThat(response.email()).isEqualTo("joao@cadi.com");
        verify(collaboratorRepository).save(any(Collaborator.class));
    }

    @Test
    void deactivateProfessorDeveDesativarUser() {
        Collaborator c = buildCollaborator(1L, "p@cadi.com", CollaboratorType.TEACHER, true);
        when(collaboratorRepository.findByIdWithUserAndAddress(1L)).thenReturn(Optional.of(c));
        when(schoolRoomRepository.findByTeacherId(1L)).thenReturn(List.of());

        service.deactivate(1L);

        assertThat(c.getUser().isActive()).isFalse();
        verify(userRepository).save(c.getUser());
    }

    @Test
    void reactivateDeveAlterarUserActiveParaTrue() {
        Collaborator c = buildCollaborator(1L, "p@cadi.com", CollaboratorType.STAFF, false);
        when(collaboratorRepository.findByIdWithUserAndAddress(1L)).thenReturn(Optional.of(c));

        service.reactivate(1L);

        assertThat(c.getUser().isActive()).isTrue();
        verify(userRepository).save(c.getUser());
    }

    @Test
    void deactivateDeveLancar404QuandoNaoExiste() {
        when(collaboratorRepository.findByIdWithUserAndAddress(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
