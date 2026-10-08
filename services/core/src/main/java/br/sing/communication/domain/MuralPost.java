package br.sing.communication.domain;

import br.sing.common.domain.AuditableSoftDeleteEntity;
import br.sing.iam.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Publicação do mural institucional.
 *
 * <p>Existem dois modos de definição do público-alvo (mutuamente exclusivos por perfil):
 * <ul>
 *   <li><b>Por role</b> ({@code audienceRoles}): Secretaria/Admin selecionam quais
 *       perfis receberão o post (ex.: {@code ROLE_STUDENT}, {@code ROLE_PARENT}).</li>
 *   <li><b>Por turma</b> ({@code audienceSchoolroomIds}): Professor seleciona uma ou
 *       mais de suas turmas; todos os alunos matriculados nestas turmas recebem o post.</li>
 * </ul>
 *
 * <p>O público resolvido fica registrado em {@link MuralPostView} (uma linha por destinatário),
 * que também controla a visualização ({@code viewedAt}).
 *
 * <p>Remoção é lógica (soft-delete via {@code is_active}).
 */
@Entity
@Table(name = "mural_post")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MuralPost extends AuditableSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "mural_post_audience",
            joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "role_name", nullable = false)
    private Set<String> audienceRoles = new HashSet<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "mural_post_audience_schoolroom",
            joinColumns = @JoinColumn(name = "post_id")
    )
    @Column(name = "schoolroom_id", nullable = false)
    private Set<Long> audienceSchoolroomIds = new HashSet<>();
}
