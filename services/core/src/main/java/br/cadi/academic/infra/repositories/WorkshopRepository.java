package br.cadi.academic.infra.repositories;

import br.cadi.academic.domain.entities.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkshopRepository extends JpaRepository<Workshop, Long> {
    List<Workshop> findByIsActiveTrue();
    Optional<Workshop> findFirstByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    Optional<Workshop> findByName(String name);
}
