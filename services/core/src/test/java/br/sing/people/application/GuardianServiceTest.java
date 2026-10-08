package br.sing.people.application;

import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import br.sing.people.api.dto.request.AddressRequest;
import br.sing.people.api.dto.request.GuardianRequest;
import br.sing.people.api.dto.response.GuardianResponse;
import br.sing.people.domain.entities.Address;
import br.sing.people.domain.entities.Guardian;
import br.sing.people.infra.repositories.AddressRepository;
import br.sing.people.infra.repositories.GuardianRepository;
import br.sing.people.infra.repositories.StudentGuardianRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link GuardianService}.
 *
 * <p>Cobre criação com criação automática de {@link User} e {@link Address},
 * validação de CPF duplicado, atualização e exclusão em cascata.
 */
@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @Mock private GuardianRepository guardianRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private GuardianService guardianService;

    @BeforeEach
    void setUp() {
        guardianService = new GuardianService(
                guardianRepository, userRepository, roleRepository,
                addressRepository, studentGuardianRepository, passwordEncoder);
    }

    private AddressRequest addressDto() {
        return new AddressRequest("12345-678", "Rua A", 100, "Centro",
                "São Paulo", "SP", "");
    }

    private GuardianRequest validRequest() {
        return new GuardianRequest(
                "Maria Silva",
                "maria@sing.local",
                "senha123",
                "11122233344",
                "11999999999",
                LocalDate.of(1985, 5, 10),
                "MOTHER",
                true,
                addressDto()
        );
    }

    private Role parentRole() {
        return Role.builder().id(4L).name("ROLE_PARENT").build();
    }

    @Test
    void createDeveCriarUserAddressEGuardianERetornarResponse() {
        GuardianRequest request = validRequest();

        when(guardianRepository.findByCpf("11122233344")).thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class)))
                .thenAnswer(inv -> {
                    Address a = inv.getArgument(0);
                    a.setId(1L);
                    return a;
                });
        when(roleRepository.findByName("ROLE_PARENT")).thenReturn(Optional.of(parentRole()));
        when(passwordEncoder.encode("senha123")).thenReturn("$encoded$");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> {
                    User u = inv.getArgument(0);
                    u.setId(99L);
                    return u;
                });
        when(guardianRepository.save(any(Guardian.class)))
                .thenAnswer(inv -> {
                    Guardian g = inv.getArgument(0);
                    g.setId(50L);
                    return g;
                });

        GuardianResponse response = guardianService.create(request);

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.name()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("maria@sing.local");
        assertThat(response.isPrimary()).isTrue();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole().getName()).isEqualTo("ROLE_PARENT");
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("$encoded$");
    }

    @Test
    void createDeveLancarExcecaoQuandoCpfJaCadastrado() {
        when(guardianRepository.findByCpf("11122233344"))
                .thenReturn(Optional.of(new Guardian()));

        assertThatThrownBy(() -> guardianService.create(validRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CPF já cadastrado");

        verify(guardianRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void createDeveLancarExcecaoQuandoRoleParentNaoExiste() {
        when(guardianRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleRepository.findByName("ROLE_PARENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guardianService.create(validRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ROLE_PARENT");
    }

    @Test
    void findByIdDeveRetornarGuardianResponse() {
        Guardian g = Guardian.builder()
                .isPrimary(true)
                .build();
        g.setId(1L);
        g.setName("Maria");
        g.setEmail("m@sing.local");
        g.setPhone("119");

        when(guardianRepository.findById(1L)).thenReturn(Optional.of(g));

        GuardianResponse response = guardianService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Maria");
    }

    @Test
    void findByIdDeveLancarRuntimeQuandoNaoExiste() {
        when(guardianRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guardianService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void findAllDeveMapearParaListaDeResponse() {
        Guardian g1 = Guardian.builder().isPrimary(true).build();
        g1.setId(1L); g1.setName("A"); g1.setEmail("a@a.com"); g1.setPhone("1");
        Guardian g2 = Guardian.builder().isPrimary(false).build();
        g2.setId(2L); g2.setName("B"); g2.setEmail("b@b.com"); g2.setPhone("2");

        when(guardianRepository.findAll()).thenReturn(List.of(g1, g2));

        List<GuardianResponse> responses = guardianService.findAll();

        assertThat(responses).extracting(GuardianResponse::name).containsExactly("A", "B");
        assertThat(responses).extracting(GuardianResponse::isPrimary)
                .containsExactly(true, false);
    }

    @Test
    void deleteDeveLimparVinculosERemoverUsuarioEGuardian() {
        User user = User.builder().id(99L).username("m@sing.local").email("m@sing.local")
                .password("x").role(parentRole()).build();
        Guardian g = Guardian.builder().isPrimary(true).build();
        g.setId(1L); g.setUser(user); g.setName("M");

        when(guardianRepository.findById(1L)).thenReturn(Optional.of(g));

        guardianService.delete(1L);

        verify(studentGuardianRepository).deleteByGuardianId(1L);
        verify(userRepository).deleteById(99L);
        verify(guardianRepository).delete(g);
    }

    @Test
    void deleteDeveLancarRuntimeQuandoGuardianNaoExiste() {
        when(guardianRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guardianService.delete(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("não encontrado");

        verify(studentGuardianRepository, never()).deleteByGuardianId(any());
        verify(guardianRepository, never()).delete(any());
    }
}
