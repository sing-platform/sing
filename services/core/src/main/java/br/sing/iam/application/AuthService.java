package br.sing.iam.application;

import br.sing.exception.*;
import br.sing.iam.api.UserDTO;
import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.RoleRepository;
import br.sing.iam.infra.UserRepository;
import br.sing.iam.api.AuthRequest;
import br.sing.iam.api.AuthResponse;
import br.sing.iam.api.ChangePasswordRequest;
import br.sing.iam.api.RegisterRequest;
import br.sing.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PersonRepository personRepository;

    /**
     * Claims que tornam o token autossuficiente para os demais servicos.
     *
     * <p>{@code pid} (id da pessoa) evita que cada servico precise perguntar ao
     * core quem e o portador do token a cada requisicao. Nem todo usuario tem
     * pessoa associada, entao a claim e opcional.</p>
     */
    private java.util.Map<String, Object> identityClaims(User user) {
        return personRepository.findByUserId(user.getId())
                .<java.util.Map<String, Object>>map(p -> java.util.Map.of("pid", p.getId()))
                .orElseGet(java.util.Map::of);
    }

    public AuthResponse register(RegisterRequest request) {

        try{
            Role defaultRole = roleRepository.findByName("ROLE_STUDENT").orElse(null);

            var user = User.builder()
                    .username(request.username())
                    .email(request.email())
                    .password(passwordEncoder.encode(request.password()))
                    .role(defaultRole)
                    .build();

            repository.findByEmail(request.email()).ifPresent((e) -> {
                throw new HttpServerErrorException(HttpStatus.CONFLICT);
            });
            repository.save(user);

            var jwtToken = jwtService.generateToken(identityClaims(user), user);
            return AuthResponse.builder()
                    .token(jwtToken)
                    .build();
        }catch (HttpServerErrorException e){
            throw new UserCreationConflict("Email already existente");
        }
        catch (Exception e){
            throw new ServerError("Error saving new user  ");
        }

    }

    public AuthResponse authenticate(AuthRequest request) {
        try{
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            var user = repository.findByEmail(request.getEmail())
                    .orElseThrow();
            var jwtToken = jwtService.generateToken(identityClaims(user), user);
            return AuthResponse.builder()
                    .token(jwtToken)
                    .build();
        }catch (BadCredentialsException e){
            throw new BadCredentialsException("Invalid email or password");
        }
        catch (Exception ex){
            throw new UnauthorizedException(ex.getMessage());
        }

    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public UserDTO setRole(String name, String role){
        User user = repository.findByUsername(name).orElseThrow(()->new EntityNotFoundException("User with username: "+name+" not found"));

        user.setRole(roleRepository.findByName(role).orElseThrow(()->new EntityNotFoundException("role with name" + role +"  not found")));
        try{
            repository.save(user);

        }catch (Exception ex){throw new ServerError(ex.getMessage());}
        return new UserDTO(user.getName(), user.getRole().getName());
    }

    public List<UserDTO> getAllUsers() {
        List<UserDTO> usersDTO = new ArrayList<>();
        for(var user: repository.findAll()){
            usersDTO.add(new UserDTO(user.getName(), user.getRole().getName()));
        }
        return usersDTO;
    }

    public User getPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new UnauthorizedException("Não autenticado.");
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Sessão inválida.");
        }
        return user;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User principal = getPrincipal();
        User user = repository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Sessão inválida."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException("Senha atual inválida.", HttpStatus.BAD_REQUEST);
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("A confirmação da nova senha não confere.", HttpStatus.BAD_REQUEST);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual.", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        repository.save(user);
    }

}
