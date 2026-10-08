package br.sing.communication.application;

import br.sing.communication.api.dto.response.NotificationResponse;
import br.sing.communication.domain.Notification;
import br.sing.communication.infra.repositories.NotificationRepository;
import br.sing.exception.EntityNotFoundException;
import br.sing.iam.domain.User;
import br.sing.iam.infra.UserRepository;
import br.sing.people.domain.enums.Shift;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    public static final String TYPE_WAITING_LIST_VACANCY = "WAITING_LIST_VACANCY";

    public static final String WAITING_LIST_ACTION_URL = "/dashboard/secretaria/espera";

    private static final List<String> RECIPIENT_ROLES =
            List.of("ROLE_SECRETARY", "ROLE_ADMIN", "ROLE_MANAGER");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String userEmail) {
        User user = getUserOrThrow(userEmail);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void markAsRead(Long notificationId, String userEmail) {
        User user = getUserOrThrow(userEmail);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notificação não encontrada"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new EntityNotFoundException("Notificação não encontrada");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(String userEmail) {
        User user = getUserOrThrow(userEmail);
        List<Notification> unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(n -> n.getReadAt() == null)
                .toList();
        LocalDateTime now = LocalDateTime.now();
        for (Notification notification : unread) {
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    public static String waitingListReferenceKey(Long workshopId, Shift shift) {
        return "WAITING_LIST:" + workshopId + ":" + shift.name();
    }

    // REQUIRES_NEW: chamado após commit de eventos ou de outra transação ativa.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createWaitingListVacancyIfAbsent(
            Long workshopId,
            String workshopName,
            Shift shift,
            long availableVacancies,
            long waitingCount
    ) {
        if (workshopId == null || availableVacancies <= 0 || waitingCount <= 0) {
            return;
        }
        String referenceKey = waitingListReferenceKey(workshopId, shift);
        String title = "Vaga disponível na lista de espera";
        String message = String.format(
                "Há %d vaga(s) na oficina \"%s\" (turno %s) e %d candidato(s) aguardando. "
                        + "Acesse a lista de espera para convocar os responsáveis.",
                availableVacancies,
                workshopName != null ? workshopName : "",
                shiftLabelPt(shift),
                waitingCount
        );
        createForSecretariesIfAbsent(referenceKey, title, message, WAITING_LIST_ACTION_URL);
    }

    private void createForSecretariesIfAbsent(String referenceKey, String title, String message, String actionUrl) {
        List<User> all = new java.util.ArrayList<>();
        for (String roleName : RECIPIENT_ROLES) {
            for (User user : userRepository.findByRole_Name(roleName)) {
                if (all.stream().noneMatch(u -> u.getId().equals(user.getId()))) {
                    all.add(user);
                }
            }
        }

        for (User user : all) {
            if (!Boolean.TRUE.equals(user.isActive())) {
                continue;
            }
            if (notificationRepository.existsByUserIdAndReferenceKeyAndReadAtIsNull(user.getId(), referenceKey)) {
                continue;
            }
            Notification notification = Notification.builder()
                    .title(title)
                    .message(message)
                    .type(TYPE_WAITING_LIST_VACANCY)
                    .user(user)
                    .actionUrl(actionUrl)
                    .referenceKey(referenceKey)
                    .build();
            notificationRepository.save(notification);
        }
    }

    private User getUserOrThrow(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getActionUrl(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private static String shiftLabelPt(Shift shift) {
        return switch (shift) {
            case MORNING -> "manhã";
            case AFTERNOON -> "tarde";
            case EVENING -> "noite";
        };
    }
}
