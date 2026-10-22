package br.sing.iam.application;

import br.sing.exception.EntityNotFoundException;
import br.sing.exception.ServerError;
import br.sing.exception.UnauthorizedException;
import br.sing.exception.UserCreationConflict;
import br.sing.iam.api.AuthRequest;
import br.sing.iam.api.AuthResponse;
import br.sing.iam.api.ChangePasswordRequest;
import br.sing.iam.api.RegisterRequest;
import br.sing.iam.api.UserDTO;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários para {@link AuthService}.
 *
 * <p>Cobre os fluxos de:
 * <ul>
 *   <li>Registro público — só {@code ROLE_STUDENT}</li>
 *   <li>Autenticação — credenciais válidas / inválidas</li>
 *   <li>Atribuição de role e listagem de usuários</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    /** Usado para resolver a claim {@code pid} do token. */
    @Mock
    private br.sing.people.infra.repositories.PersonRepository personRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                personRepository
        );
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Role studentRole() {
        return Role.builder().id(1L).name("ROLE_STUDENT").build();
    }

    private Role professorRole() {
        return Role.builder().id(2L).name("ROLE_PROFESSOR").build();
    }

    // ------------------------------------------------------------------
    // register
    // ------------------------------------------------------------------

    @Test
    void registerDeveCriarUsuarioComRoleStudentERetornarToken() {
        RegisterRequest request = new RegisterRequest("joao", "joao@sing.local", "senha123");

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole()));
        when(passwordEncoder.encode("senha123")).thenReturn("$encoded$");
        when(userRepository.findByEmail("joao@sing.local")).thenReturn(Optional.empty());
        when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerDeveLancarUserCreationConflictQuandoEmailJaExiste() {
        RegisterRequest request = new RegisterRequest("joao", "joao@sing.local", "senha123");

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole()));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
        when(userRepository.findByEmail("joao@sing.local")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserCreationConflict.class)
                .hasMessageContaining("Email already existente");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerDeveLancarServerErrorQuandoRepositorioFalha() {
        RegisterRequest request = new RegisterRequest("joao", "joao@sing.local", "senha123");

        when(roleRepository.findByName("ROLE_STUDENT")).thenReturn(Optional.of(studentRole()));
        when(passwordEncoder.encode(anyString())).thenReturn("$encoded$");
        when(userRepository.findByEmail("joao@sing.local")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ServerError.class);
    }

    // ------------------------------------------------------------------
    // authenticate
    // ------------------------------------------------------------------

    @Test
    void authenticateDeveRetornarTokenQuandoCredenciaisValidas() {
        AuthRequest request = new AuthRequest("joao@sing.local", "senha123");
        User user = User.builder()
                .id(1L)
                .username("joao")
                .email("joao@sing.local")
                .password("$encoded$")
                .role(studentRole())
                .isActive(true)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("joao@sing.local")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(anyMap(), eq(user))).thenReturn("jwt-token");

        AuthResponse response = authService.authenticate(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void authenticateDeveLancarUnauthorizedQuandoAuthenticationManagerLanca() {
        AuthRequest request = new AuthRequest("joao@sing.local", "errada");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication
                        .BadCredentialsException("invalid"));

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ------------------------------------------------------------------
    // changePassword
    // ------------------------------------------------------------------

    @Test
    void changePasswordDeveValidarSenhaAtualEAtualizarHash() {
        User principal = User.builder()
                .id(1L).username("joao").email("joao@sing.local")
                .password("$old$").role(studentRole()).isActive(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null)
        );
        ChangePasswordRequest request = new ChangePasswordRequest("senhaAtual", "novaSenha123", "novaSenha123");

        when(userRepository.findByEmail("joao@sing.local")).thenReturn(Optional.of(principal));
        when(passwordEncoder.matches("senhaAtual", "$old$")).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", "$old$")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("$new$");

        authService.changePassword(request);

        assertThat(principal.getPassword()).isEqualTo("$new$");
        verify(userRepository).save(principal);
    }

    @Test
    void changePasswordDeveRejeitarSenhaAtualInvalida() {
        User principal = User.builder()
                .id(1L).username("joao").email("joao@sing.local")
                .password("$old$").role(studentRole()).isActive(true).build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(principal, null)
        );
        ChangePasswordRequest request = new ChangePasswordRequest("errada", "novaSenha123", "novaSenha123");

        when(userRepository.findByEmail("joao@sing.local")).thenReturn(Optional.of(principal));
        when(passwordEncoder.matches("errada", "$old$")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request))
                .hasMessageContaining("Senha atual inválida");

        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // setRole
    // ------------------------------------------------------------------

    @Test
    void setRoleDeveAtualizarRoleEPersistirUsuario() {
        User user = User.builder()
                .id(1L).username("joao").email("joao@sing.local")
                .password("x").role(studentRole()).isActive(true).build();

        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_PROFESSOR")).thenReturn(Optional.of(professorRole()));

        UserDTO dto = authService.setRole("joao", "ROLE_PROFESSOR");

        assertThat(dto.username()).isEqualTo("joao");
        assertThat(dto.role()).isEqualTo("ROLE_PROFESSOR");
        verify(userRepository).save(user);
    }

    @Test
    void setRoleDeveLancar404QuandoUsuarioNaoEncontrado() {
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.setRole("inexistente", "ROLE_PROFESSOR"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("inexistente");
    }

    @Test
    void setRoleDeveLancar404QuandoRoleNaoEncontrada() {
        User user = User.builder()
                .id(1L).username("joao").email("joao@sing.local")
                .password("x").role(studentRole()).isActive(true).build();

        when(userRepository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ROLE_INEXISTENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.setRole("joao", "ROLE_INEXISTENTE"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // getAllUsers / getAllRoles
    // ------------------------------------------------------------------

    @Test
    void getAllUsersDeveMapearParaUserDTOComUsernameERoleName() {
        User u1 = User.builder().username("a").email("a@sing.local")
                .password("x").role(studentRole()).build();
        User u2 = User.builder().username("b").email("b@sing.local")
                .password("x").role(professorRole()).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserDTO> dtos = authService.getAllUsers();

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(UserDTO::username).containsExactly("a", "b");
        assertThat(dtos).extracting(UserDTO::role)
                .containsExactly("ROLE_STUDENT", "ROLE_PROFESSOR");
    }

    @Test
    void getAllRolesDeveDelegarParaRepositorio() {
        when(roleRepository.findAll()).thenReturn(List.of(studentRole(), professorRole()));

        List<Role> roles = authService.getAllRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getName)
                .containsExactly("ROLE_STUDENT", "ROLE_PROFESSOR");
    }
}
