package br.sing.communication.application;

import br.sing.iam.domain.Role;
import br.sing.iam.domain.User;
import br.sing.iam.infra.UserRepository;
import br.sing.people.domain.enums.Shift;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingListVacancyPublisherTest {

    @Mock
    private ResilientMessagingService messagingService;
    @Mock
    private UserRepository userRepository;

    private WaitingListVacancyPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WaitingListVacancyPublisher(messagingService, userRepository, "sing.events");
    }

    private static User user(long id, String role, boolean active) {
        return User.builder()
                .id(id)
                .username("u" + id)
                .email("u" + id + "@sing.local")
                .password("x")
                .role(Role.builder().name(role).build())
                .isActive(active)
                .build();
    }

    @Test
    void publicaEnvelopeDoContratoParaUsuariosAtivosDosTresPapeis() {
        when(userRepository.findByRole_Name("ROLE_SECRETARY")).thenReturn(List.of(user(2, "ROLE_SECRETARY", true)));
        when(userRepository.findByRole_Name("ROLE_ADMIN"))
                .thenReturn(List.of(user(1, "ROLE_ADMIN", true), user(9, "ROLE_ADMIN", false)));
        when(userRepository.findByRole_Name("ROLE_MANAGER")).thenReturn(List.of(user(5, "ROLE_MANAGER", true)));

        publisher.publishIfApplicable(4L, "Violão", Shift.MORNING, 1, 2);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingService).send(org.mockito.ArgumentMatchers.eq("sing.events"),
                org.mockito.ArgumentMatchers.eq("notificacao.solicitada"), captor.capture());
        NotificacaoSolicitadaEnvelope envelope = (NotificacaoSolicitadaEnvelope) captor.getValue();

        assertThat(envelope.eventId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(envelope.eventType()).isEqualTo("notificacao.solicitada");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.occurredAt()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
        assertThat(envelope.payload().type()).isEqualTo("WAITING_LIST_VACANCY");
        assertThat(envelope.payload().title()).isEqualTo("Vaga disponível na lista de espera");
        assertThat(envelope.payload().message()).isEqualTo(
                "Há 1 vaga(s) na oficina \"Violão\" (turno manhã) e 2 candidato(s) aguardando. "
                        + "Acesse a lista de espera para convocar os responsáveis.");
        assertThat(envelope.payload().actionUrl()).isEqualTo("/dashboard/secretaria/espera");
        assertThat(envelope.payload().referenceKey()).isEqualTo("WAITING_LIST:4:MORNING");
        assertThat(envelope.payload().recipientUserIds()).containsExactly(2L, 1L, 5L);
    }

    @Test
    void eventIdMudaACadaPublicacao() {
        when(userRepository.findByRole_Name("ROLE_SECRETARY")).thenReturn(List.of(user(2, "ROLE_SECRETARY", true)));
        when(userRepository.findByRole_Name("ROLE_ADMIN")).thenReturn(List.of());
        when(userRepository.findByRole_Name("ROLE_MANAGER")).thenReturn(List.of());

        publisher.publishIfApplicable(4L, "Violão", Shift.AFTERNOON, 1, 1);
        publisher.publishIfApplicable(4L, "Violão", Shift.AFTERNOON, 1, 1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingService, org.mockito.Mockito.times(2)).send(any(), any(), captor.capture());
        List<String> ids = captor.getAllValues().stream()
                .map(e -> ((NotificacaoSolicitadaEnvelope) e).eventId())
                .toList();
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    }

    @Test
    void usuarioEmMaisDeUmPapelApareceUmaVez() {
        User both = user(3, "ROLE_ADMIN", true);
        when(userRepository.findByRole_Name("ROLE_SECRETARY")).thenReturn(List.of(both));
        when(userRepository.findByRole_Name("ROLE_ADMIN")).thenReturn(List.of(both));
        when(userRepository.findByRole_Name("ROLE_MANAGER")).thenReturn(List.of());

        publisher.publishIfApplicable(1L, null, Shift.EVENING, 3, 1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingService).send(any(), any(), captor.capture());
        NotificacaoSolicitadaEnvelope envelope = (NotificacaoSolicitadaEnvelope) captor.getValue();
        assertThat(envelope.payload().recipientUserIds()).containsExactly(3L);
        assertThat(envelope.payload().message()).contains("(turno noite)").contains("oficina \"\"");
    }

    @Test
    void naoPublicaSemOficinaSemVagaOuSemEspera() {
        publisher.publishIfApplicable(null, "X", Shift.MORNING, 1, 1);
        publisher.publishIfApplicable(1L, "X", Shift.MORNING, 0, 1);
        publisher.publishIfApplicable(1L, "X", Shift.MORNING, 1, 0);

        verifyNoInteractions(messagingService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void naoPublicaQuandoNaoHaDestinatarios() {
        when(userRepository.findByRole_Name(any())).thenReturn(List.of());

        publisher.publishIfApplicable(1L, "X", Shift.MORNING, 1, 1);

        verify(messagingService, never()).send(any(), any(), any());
    }
}
