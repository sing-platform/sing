package br.sing.iam.application;

import br.sing.exception.UserCreationConflict;
import br.sing.iam.api.AuthResponse;
import br.sing.iam.api.RegisterRequestAdmin;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import br.sing.exception.EntityNotFoundException;
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