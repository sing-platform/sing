package br.sing.finance.infra.repositories;

import br.sing.finance.domain.Sponsor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SponsorRepository extends JpaRepository<Sponsor, Long> {

    @EntityGraph(attributePaths = {"student"})
    List<Sponsor> findAllByIsActiveTrue();

    @EntityGraph(attributePaths = {"student"})
    List<Sponsor> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"student"})
    java.util.Optional<Sponsor> findById(Long id);
}
