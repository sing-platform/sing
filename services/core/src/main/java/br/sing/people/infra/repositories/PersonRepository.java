package br.sing.people.infra.repositories;
import br.sing.people.domain.entities.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByEmail(String email);
    Optional<Person> findByCpf(String cpf);
    Optional<Person> findByUserId(Long userId);

}