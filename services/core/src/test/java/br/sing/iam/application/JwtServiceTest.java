package br.sing.iam.application;

import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para {@link JwtService}.
 *
 * <p>Sem container Spring — instanciamos o service e injetamos os campos
 * {@code secretKey} e {@code jwtExpiration} via {@link ReflectionTestUtils}.
 */
class JwtServiceTest {

    private static final String SECRET_HEX =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final long EXPIRATION_MS = 60_000L; // 1 min

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_HEX);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_MS);

        user = User.builder()
                .id(1L)
                .username("joao")
                .email("joao@sing.local")
                .password("senha")
                .role(Role.builder().id(1L).name("ROLE_PROFESSOR").build())
                .isActive(true)
                .build();
    }

    @Test
    void generateTokenDeveProduzirTokenJwtNaoVazio() {
        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        // JWT tem três segmentos separados por ponto
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsernameDeveRetornarEmailUsadoComoSubject() {
        String token = jwtService.generateToken(user);

        // O User.getUsername() expõe o email — é o subject do token.
        assertThat(jwtService.extractUsername(token)).isEqualTo("joao@sing.local");
    }

    @Test
    void extractRoleDeveRetornarRoleDoClaim() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_PROFESSOR");
    }

    @Test
    void isTokenValidDeveRetornarTrueParaTokenAssinadoPeloUsuarioAtual() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValidDeveRetornarFalseParaUsuarioDiferente() {
        String token = jwtService.generateToken(user);

        User outro = User.builder()
                .username("maria")
                .email("maria@sing.local")
                .password("x")
                .role(user.getRole())
                .isActive(true)
                .build();

        assertThat(jwtService.isTokenValid(token, outro)).isFalse();
    }

    @Test
    void isTokenValidDeveRetornarFalseParaTokenExpirado() {
        // expira em 1ms para simular token vencido
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);
        String token = jwtService.generateToken(user);

        // espera passar do limite — isTokenExpired interno usa Date.before(now)
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ao verificar token vencido, parsing lança ExpiredJwtException
        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void extractUsernameDeveLancarExcecaoQuandoTokenAssinadoComOutraChave() {
        // gera token com a chave atual
        String token = jwtService.generateToken(user);

        // troca a chave de assinatura — qualquer parse deve falhar com SignatureException
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }

    @Test
    void generateTokenDeveAceitarUserSemRoleEPersistirRoleNulaNoClaim() {
        User semRole = User.builder()
                .username("pedro")
                .email("pedro@sing.local")
                .password("x")
                .role(null)
                .isActive(true)
                .build();

        String token = jwtService.generateToken(semRole);

        // role do claim é null quando User não tem Role
        assertThat(jwtService.extractRole(token)).isNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo("pedro@sing.local");
    }
}
