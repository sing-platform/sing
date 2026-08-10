package br.cadi.communication.infra.repositories;

import br.cadi.communication.domain.MuralPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MuralPostRepository extends JpaRepository<MuralPost, Long> {

    /** Todos os posts ativos ordenados por data de criação (admin/secretaria). */
    List<MuralPost> findByIsActiveTrueOrderByCreatedAtDesc();

    /** Posts ativos publicados por um autor específico. */
    List<MuralPost> findByAuthor_IdAndIsActiveTrueOrderByCreatedAtDesc(Long authorId);
}
