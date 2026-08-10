package br.cadi.communication.application;

import br.cadi.academic.infra.repositories.EnrollmentRepository;
import br.cadi.academic.infra.repositories.SchoolRoomRepository;
import br.cadi.communication.api.dto.request.MuralPostCreateRequest;
import br.cadi.communication.api.dto.request.MuralPostUpdateRequest;
import br.cadi.communication.api.dto.response.MuralPostResponse;
import br.cadi.communication.domain.EmailMessageEvent;
import br.cadi.communication.domain.MuralPost;
import br.cadi.communication.domain.MuralPostView;
import br.cadi.communication.infra.repositories.MuralPostRepository;
import br.cadi.communication.infra.repositories.MuralPostViewRepository;
import br.cadi.config.RabbitMQConfig;
import br.cadi.exception.BusinessException;
import br.cadi.exception.EntityNotFoundException;
import br.cadi.iam.domain.User;
import br.cadi.iam.infra.UserRepository;
import br.cadi.people.infra.repositories.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MuralPostService {

    private final MuralPostRepository muralPostRepository;
    private final MuralPostViewRepository viewRepository;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolRoomRepository schoolRoomRepository;
    private final ResilientMessagingService messagingService;

    // -----------------------------------------------------------------------
    // Leitura
    // -----------------------------------------------------------------------

    /**
     * Lista as publicações visíveis para o usuário autenticado.
     *
     * <ul>
     *   <li>ADMIN / SECRETARY → todos os posts ativos (visão de gestão, sem rastreamento de views).</li>
     *   <li>Demais → posts via tabela {@code mural_post_view}, com status de leitura.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<MuralPostResponse> listForCurrentUser(String userEmail) {
        User user = getUserOrThrow(userEmail);
        String role = user.getRole().getName();

        if (isManager(role)) {
            return muralPostRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                    .stream()
                    .map(p -> toResponse(p, true))
                    .toList();
        }

        return viewRepository.findActiveByUserId(user.getId())
                .stream()
                .map(v -> toResponse(v.getPost(), v.getViewedAt() != null))
                .toList();
    }

    /**
     * Retorna uma publicação por ID respeitando as regras de visibilidade.
     */
    @Transactional(readOnly = true)
    public MuralPostResponse getById(Long id, String userEmail) {
        User user = getUserOrThrow(userEmail);
        MuralPost post = getActivePostOrThrow(id);

        if (isManager(user.getRole().getName())) {
            return toResponse(post, true);
        }

        MuralPostView view = viewRepository.findByPostIdAndUserId(id, user.getId())
                .orElseThrow(() -> new BusinessException("Acesso não autorizado a esta publicação", HttpStatus.FORBIDDEN));

        return toResponse(post, view.getViewedAt() != null);
    }

    // -----------------------------------------------------------------------
    // Escrita
    // -----------------------------------------------------------------------

    /**
     * Cria uma nova publicação no mural.
     *
     * <p>Após a criação:
     * <ul>
     *   <li>Resolve os destinatários e cria registros em {@code mural_post_view}.</li>
     *   <li>Envia e-mail de notificação para cada destinatário via RabbitMQ.</li>
     * </ul>
     */
    @Transactional
    public MuralPostResponse create(MuralPostCreateRequest request, String userEmail) {
        User author = getUserOrThrow(userEmail);
        String role = author.getRole().getName();

        validateAudienceRequest(role, request.audienceRoles(), request.audienceSchoolroomIds(), null);

        MuralPost post = MuralPost.builder()
                .title(request.title().trim())
                .content(request.content())
                .author(author)
                .audienceRoles(nullSafeSet(request.audienceRoles()))
                .audienceSchoolroomIds(nullSafeSet(request.audienceSchoolroomIds()))
                .isActive(true)
                .build();

        post = muralPostRepository.save(post);

        // Resolve destinatários e cria views + notificações
        Set<User> recipients = resolveRecipients(role, request.audienceRoles(), request.audienceSchoolroomIds(), author);
        createViewsAndNotify(post, recipients, author);

        return toResponse(post, false);
    }

    /**
     * Atualiza título, conteúdo e público-alvo de uma publicação.
     *
     * <p>Ao atualizar, os registros de visualização são recriados para que
     * os novos destinatários recebam o post como não lido.
     */
    @Transactional
    public MuralPostResponse update(Long id, MuralPostUpdateRequest request, String userEmail) {
        User user = getUserOrThrow(userEmail);
        MuralPost post = getActivePostOrThrow(id);
        String role = user.getRole().getName();

        checkWriteAccess(user, post);
        validateAudienceRequest(role, request.audienceRoles(), request.audienceSchoolroomIds(), id);

        post.setTitle(request.title().trim());
        post.setContent(request.content());
        post.setAudienceRoles(nullSafeSet(request.audienceRoles()));
        post.setAudienceSchoolroomIds(nullSafeSet(request.audienceSchoolroomIds()));
        post.setUpdatedAt(LocalDateTime.now());

        post = muralPostRepository.save(post);

        // Recria views para novos destinatários; mantém os já lidos
        Set<User> newRecipients = resolveRecipients(role, request.audienceRoles(), request.audienceSchoolroomIds(), user);
        addViewsForNewRecipients(post, newRecipients);

        return toResponse(post, false);
    }

    /**
     * Soft-delete: marca o post como inativo.
     */
    @Transactional
    public void delete(Long id, String userEmail) {
        User user = getUserOrThrow(userEmail);
        MuralPost post = getActivePostOrThrow(id);
        checkWriteAccess(user, post);

        post.setActive(false);
        muralPostRepository.save(post);
    }

    // -----------------------------------------------------------------------
    // View tracking
    // -----------------------------------------------------------------------

    /**
     * Marca todos os posts não lidos do usuário como lidos.
     * Chamado quando o usuário acessa a página do mural.
     */
    @Transactional
    public void markAllRead(String userEmail) {
        User user = getUserOrThrow(userEmail);
        if (isManager(user.getRole().getName())) return; // gestores não têm view tracking
        viewRepository.markAllAsRead(user.getId(), LocalDateTime.now());
    }

    /**
     * Conta o número de posts não lidos para o badge da navbar.
     *
     * @return 0 para ADMIN/SECRETARY (eles não têm rastreamento de views)
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String userEmail) {
        User user = getUserOrThrow(userEmail);
        if (isManager(user.getRole().getName())) return 0;
        return viewRepository.countByUserIdAndViewedAtIsNull(user.getId());
    }

    // -----------------------------------------------------------------------
    // Helpers de audiência e validação
    // -----------------------------------------------------------------------

    /**
     * Valida as regras de público-alvo conforme o perfil do autor.
     *
     * <p>Professor:
     * <ul>
     *   <li>Deve informar {@code audienceSchoolroomIds} (suas próprias turmas).</li>
     *   <li>Não pode usar {@code audienceRoles}.</li>
     *   <li>Todas as turmas informadas devem pertencer ao professor.</li>
     * </ul>
     *
     * <p>Secretaria/Admin: deve informar ao menos um dos dois campos.
     *
     * @param postId null na criação; usado para mensagens de erro no update
     */
    private void validateAudienceRequest(String roleName,
                                         Set<String> audienceRoles,
                                         Set<Long> audienceSchoolroomIds,
                                         Long postId) {
        boolean hasRoles    = audienceRoles != null && !audienceRoles.isEmpty();
        boolean hasRooms    = audienceSchoolroomIds != null && !audienceSchoolroomIds.isEmpty();

        if ("ROLE_PROFESSOR".equals(roleName)) {
            if (hasRoles) {
                throw new BusinessException(
                        "Professor não pode usar público-alvo por role. Use audienceSchoolroomIds.",
                        HttpStatus.FORBIDDEN);
            }
            if (!hasRooms) {
                throw new BusinessException(
                        "Professor deve informar ao menos uma turma em audienceSchoolroomIds.",
                        HttpStatus.BAD_REQUEST);
            }
        } else {
            // SECRETARY / ADMIN
            if (!hasRoles && !hasRooms) {
                throw new BusinessException(
                        "Informe ao menos um campo de público-alvo: audienceRoles ou audienceSchoolroomIds.",
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    /**
     * Resolve o conjunto de usuários destinatários a partir dos critérios de audiência.
     */
    private Set<User> resolveRecipients(String authorRole,
                                        Set<String> audienceRoles,
                                        Set<Long> audienceSchoolroomIds,
                                        User author) {
        Set<User> recipients = new HashSet<>();

        // Resolução por role (Secretaria/Admin)
        if (audienceRoles != null) {
            for (String role : audienceRoles) {
                recipients.addAll(userRepository.findByRole_Name(role));
            }
        }

        // Resolução por turma
        if (audienceSchoolroomIds != null) {
            for (Long roomId : audienceSchoolroomIds) {
                // Valida que professor é dono da turma
                if ("ROLE_PROFESSOR".equals(authorRole)) {
                    Long teacherPersonId = personRepository.findByUserId(author.getId())
                            .map(p -> p.getId())
                            .orElseThrow(() -> new EntityNotFoundException("Perfil do professor não encontrado"));
                    schoolRoomRepository.findById(roomId).ifPresent(room -> {
                        if (room.getTeacher() == null || !room.getTeacher().getId().equals(teacherPersonId)) {
                            throw new BusinessException(
                                    "Turma " + roomId + " não pertence ao professor autenticado.",
                                    HttpStatus.FORBIDDEN);
                        }
                    });
                }
                enrollmentRepository.findActiveBySchoolroomIdWithStudent(roomId)
                        .forEach(e -> {
                            User studentUser = e.getStudent().getUser();
                            if (studentUser != null && studentUser.isEnabled()) {
                                recipients.add(studentUser);
                            }
                        });
            }
        }

        // Não incluir o próprio autor
        recipients.remove(author);

        return recipients;
    }

    /** Cria views para todos os destinatários e envia notificações por e-mail. */
    private void createViewsAndNotify(MuralPost post, Set<User> recipients, User author) {
        for (User recipient : recipients) {
            if (!viewRepository.existsByPostAndUser(post, recipient)) {
                viewRepository.save(MuralPostView.builder()
                        .post(post)
                        .user(recipient)
                        .viewedAt(null)
                        .build());
            }
            sendEmailNotification(post, recipient);
        }
    }

    /** Adiciona views apenas para destinatários ainda não registrados (usado no update). */
    private void addViewsForNewRecipients(MuralPost post, Set<User> recipients) {
        for (User recipient : recipients) {
            if (!viewRepository.existsByPostAndUser(post, recipient)) {
                viewRepository.save(MuralPostView.builder()
                        .post(post)
                        .user(recipient)
                        .viewedAt(null)
                        .build());
                sendEmailNotification(post, recipient);
            }
        }
    }

    /** Envia e-mail de notificação via RabbitMQ (com fallback no outbox). */
    private void sendEmailNotification(MuralPost post, User recipient) {
        String body = buildEmailBody(post, recipient);
        EmailMessageEvent event = EmailMessageEvent.builder()
                .to(recipient.getEmail())
                .subject("Nova publicação no mural: " + post.getTitle())
                .body(body)
                .html(true)
                .build();
        messagingService.send(RabbitMQConfig.EXCHANGE, RabbitMQConfig.EMAIL_RK, event);
    }

    private String buildEmailBody(MuralPost post, User recipient) {
        String authorName = resolveAuthorName(post.getAuthor());
        return String.format("""
                <html><body>
                <h2>Nova publicação no Mural CADI</h2>
                <h3>%s</h3>
                <p>%s</p>
                <hr/>
                <p><small>Publicado por <strong>%s</strong> em %s.<br/>
                Acesse o sistema para visualizar.</small></p>
                </body></html>
                """,
                post.getTitle(),
                post.getContent().replace("\n", "<br/>"),
                authorName,
                post.getCreatedAt().toLocalDate()
        );
    }

    // -----------------------------------------------------------------------
    // Controle de acesso
    // -----------------------------------------------------------------------

    private void checkWriteAccess(User user, MuralPost post) {
        String role = user.getRole().getName();
        if (isManager(role)) return;
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new BusinessException("Apenas o autor pode modificar esta publicação", HttpStatus.FORBIDDEN);
        }
    }

    private boolean isManager(String roleName) {
        return "ROLE_ADMIN".equals(roleName) || "ROLE_SECRETARY".equals(roleName);
    }

    // -----------------------------------------------------------------------
    // Helpers utilitários
    // -----------------------------------------------------------------------

    private User getUserOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    private MuralPost getActivePostOrThrow(Long id) {
        MuralPost post = muralPostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Publicação não encontrada"));
        if (!post.isActive()) {
            throw new EntityNotFoundException("Publicação não encontrada");
        }
        return post;
    }

    /** Resolve o nome de exibição: Person.name quando disponível, username como fallback. */
    private String resolveAuthorName(User author) {
        return personRepository.findByUserId(author.getId())
                .map(p -> p.getName())
                .orElse(author.getName());
    }

    private <T> Set<T> nullSafeSet(Set<T> set) {
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    private MuralPostResponse toResponse(MuralPost post, boolean viewed) {
        return MuralPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .authorName(resolveAuthorName(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .audienceRoles(post.getAudienceRoles())
                .audienceSchoolroomIds(post.getAudienceSchoolroomIds())
                .viewed(viewed)
                .build();
    }
}
