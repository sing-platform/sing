package br.sing.communication.infra.repositories;

import br.sing.communication.domain.MuralPost;
import br.sing.communication.domain.MuralPostView;
import br.sing.iam.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MuralPostViewRepository extends JpaRepository<MuralPostView, Long> {

    /** Todas as views de um usuário (para listar o mural com status de leitura). */
    @Query("""
            SELECT v FROM MuralPostView v
            JOIN FETCH v.post p
            WHERE v.user.id = :userId
              AND p.isActive = true
            ORDER BY p.createdAt DESC
            """)
    List<MuralPostView> findActiveByUserId(@Param("userId") Long userId);

    /** View específica de um post para um usuário. */
    Optional<MuralPostView> findByPostIdAndUserId(Long postId, Long userId);

    /** Conta posts não lidos de um usuário. */
    long countByUserIdAndViewedAtIsNull(Long userId);

    /** Marca todos os posts não lidos de um usuário como lidos. */
    @Modifying
    @Query("""
            UPDATE MuralPostView v
            SET v.viewedAt = :now
            WHERE v.user.id = :userId
              AND v.viewedAt IS NULL
            """)
    void markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Verifica se já existe view para o par post+user (evita duplicatas no seed). */
    boolean existsByPostAndUser(MuralPost post, User user);
}
