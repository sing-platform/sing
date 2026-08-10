package br.cadi.iam.application;

import br.cadi.exception.UserCreationConflict;
import br.cadi.iam.api.AuthResponse;
import br.cadi.iam.api.RegisterRequestAdmin;
import br.cadi.iam.domain.Role;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.RoleRepository;
import br.cadi.iam.infra.UserRepository;
import br.cadi.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse cadastrarComRole(RegisterRequestAdmin request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserCreationConflict("Email já cadastrado");
        }

        String roleName = request.role() != null ? request.role() : "ROLE_ALUNO";

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + roleName));

        var user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .token(jwtService.generateToken(user))
                .build();
    }
}