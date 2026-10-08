package br.sing.people.infra.repositories;

import br.sing.people.domain.entities.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    Optional<Guardian> findByCpf(String cpf);
    Optional<Guardian> findByEmail(String email);
}