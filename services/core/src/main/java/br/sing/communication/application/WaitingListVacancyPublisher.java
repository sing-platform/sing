package br.sing.communication.application;

import br.sing.academic.application.MatriculaEnvelope;
import br.sing.academic.infra.messaging.EventsRabbitConfig;
import br.sing.iam.domain.User;
import br.sing.iam.infra.UserRepository;
import br.sing.people.domain.enums.Shift;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WaitingListVacancyPublisher {

    public static final String TYPE_WAITING_LIST_VACANCY = "WAITING_LIST_VACANCY";
    public static final String WAITING_LIST_ACTION_URL = "/dashboard/secretaria/espera";

    private static final List<String> RECIPIENT_ROLES =
            List.of("ROLE_SECRETARY", "ROLE_ADMIN", "ROLE_MANAGER");

    private final ResilientMessagingService messagingService;
    private final UserRepository userRepository;
    private final String exchange;

    public WaitingListVacancyPublisher(
            ResilientMessagingService messagingService,
            UserRepository userRepository,
            @Value("${sing.events.exchange:sing.events}") String exchange) {
        this.messagingService = messagingService;
        this.userRepository = userRepository;
        this.exchange = exchange;
    }

    public static String waitingListReferenceKey(Long workshopId, Shift shift) {
        return "WAITING_LIST:" + workshopId + ":" + shift.name();
    }

    public void publishIfApplicable(
            Long workshopId,
            String workshopName,
            Shift shift,
            long availableVacancies,
            long waitingCount
    ) {
        if (workshopId == null || availableVacancies <= 0 || waitingCount <= 0) {
            return;
        }
        List<Long> recipientUserIds = resolveRecipientUserIds();
        if (recipientUserIds.isEmpty()) {
            return;
        }
        String message = String.format(
                "Há %d vaga(s) na oficina \"%s\" (turno %s) e %d candidato(s) aguardando. "
                        + "Acesse a lista de espera para convocar os responsáveis.",
                availableVacancies,
                workshopName != null ? workshopName : "",
                shiftLabelPt(shift),
                waitingCount
        );
        NotificacaoSolicitadaEnvelope envelope = new NotificacaoSolicitadaEnvelope(
                UUID.randomUUID().toString(),
                EventsRabbitConfig.NOTIFICACAO_SOLICITADA_RK,
                1,
                MatriculaEnvelope.instante(Instant.now()),
                new NotificacaoSolicitadaEnvelope.Payload(
                        TYPE_WAITING_LIST_VACANCY,
                        "Vaga disponível na lista de espera",
                        message,
                        WAITING_LIST_ACTION_URL,
                        waitingListReferenceKey(workshopId, shift),
                        recipientUserIds
                )
        );
        messagingService.send(exchange, EventsRabbitConfig.NOTIFICACAO_SOLICITADA_RK, envelope);
    }

    private List<Long> resolveRecipientUserIds() {
        Map<Long, User> byId = new LinkedHashMap<>();
        for (String roleName : RECIPIENT_ROLES) {
            for (User user : userRepository.findByRole_Name(roleName)) {
                if (user.isActive()) {
                    byId.putIfAbsent(user.getId(), user);
                }
            }
        }
        return List.copyOf(byId.keySet());
    }

    private static String shiftLabelPt(Shift shift) {
        return switch (shift) {
            case MORNING -> "manhã";
            case AFTERNOON -> "tarde";
            case EVENING -> "noite";
        };
    }
}
