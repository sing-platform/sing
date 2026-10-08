package br.sing.iam.application;

import br.sing.exception.EntityNotFoundException;
import br.sing.exception.UserCreationConflict;
import br.sing.iam.api.AuthResponse;
import br.sing.iam.api.RegisterRequestAdmin;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link AdminUserService}.
 *
 * <p>Endpoint exposto apenas para ROLE_ADMIN — permite cadastrar
 * usuário com role específica.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                userRepository, roleRepository, passwordEncoder, jwtService);
    }

    private RegisterRequestAdmin buildRequest(String email, String role) {
        return new RegisterRequestAdmin("novo_user", email, "senha123", role);
    }

    @Test
    void cadastrarComRoleDeveCriarUsuarioERetornarToken() {
        RegisterRequestAdmin request = buildRequest("novo@sing.local", "ROLE_PROFESSOR");
        Role role = Role.builder().id(2L).name("ROLE_PROFESSOR").build();

        when(userRepository.findByEmail("novo@sing.local")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_PROFESSOR")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("senha123")).thenReturn("$encoded$");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-admin-token");

        AuthResponse response = adminUserService.cadastrarComRole(request);

        assertThat(response.getToken()).isEqualTo("jwt-admin-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void cadastrarComRoleDeveLancar409QuandoEmailJaExiste() {
        RegisterRequestAdmin request = buildRequest("existente@sing.local", "ROLE_PROFESSOR");
        when(userRepository.findByEmail("existente@sing.local"))
                .thenReturn(Optional.of(User.builder().build()));

        assertThatThrownBy(() -> adminUserService.cadastrarComRole(request))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("Email já cadastrado");

        verify(userRepository, never()).save(any());
    }

    @Test
    void cadastrarComRoleDeveLancar404QuandoRoleNaoExiste() {
        RegisterRequestAdmin request = buildRequest("novo@sing.local", "ROLE_INEXISTENTE");

        when(userRepository.findByEmail("novo@sing.local")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_INEXISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.cadastrarComRole(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("ROLE_INEXISTENTE");

        verify(userRepository, never()).save(any());
    }

    @Test
    void cadastrarComRoleDeveUsarRoleAlunoComoFallbackQuandoRoleNula() {
        // request com role nula → service deve buscar "ROLE_ALUNO"
        RegisterRequestAdmin request = buildRequest("novo@sing.local", null);
        Role roleAluno = Role.builder().id(1L).name("ROLE_ALUNO").build();

        when(userRepository.findByEmail("novo@sing.local")).thenReturn(Optional.empty());
        when(roleRepository.findByName("ROLE_ALUNO")).thenReturn(Optional.of(roleAluno));
        when(passwordEncoder.encode(anyString())).thenReturn("$x$");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        AuthResponse response = adminUserService.cadastrarComRole(request);

        assertThat(response.getToken()).isEqualTo("jwt");
        verify(roleRepository).findByName("ROLE_ALUNO");
    }
}
