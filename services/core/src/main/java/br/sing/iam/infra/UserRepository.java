package br.sing.iam.infra;

import br.sing.iam.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String mail);

    /** Todos os usuários com uma determinada role (ex.: {@code ROLE_STUDENT}). */
    List<User> findByRole_Name(String roleName);
}
