package br.sing.academic.application;

import br.sing.academic.api.dto.response.IntegracaoMatriculaResponse;
import br.sing.academic.domain.entities.Enrollment;
import br.sing.academic.domain.entities.MatriculaIntegracao;
import br.sing.academic.infra.repositories.MatriculaIntegracaoRepository;
import br.sing.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatriculaIntegracaoService {

    private final MatriculaIntegracaoRepository repository;
    private final N2Properties props;

    /**
     * Chamado dentro da transação que grava a matrícula. O eventId nasce aqui e não é trocado.
     */
    public MatriculaIntegracao registrar(Enrollment enrollment) {
        String eventId = UUID.randomUUID().toString();
        Instant agora = Instant.now();
        MatriculaIntegracao linha = MatriculaIntegracao.builder()
                .matriculaId(enrollment.getId())
                .eventId(eventId)
                .envelope(MatriculaEnvelope.json(enrollment, eventId, agora))
                .valorCentavos(props.getCobrancaValorCentavos())
                .vencimento(enrollment.getEnrollmentDate().plusDays(props.getCobrancaPrazoDias()))
                .publicacao("pendente")
                .cobranca("pendente")
                .tentativasPublicacao(0)
                .tentativasCobranca(0)
                .build();
        return repository.save(linha);
    }

    public IntegracaoMatriculaResponse consultar(Long matriculaId) {
        MatriculaIntegracao linha = repository.findByMatriculaId(matriculaId)
                .orElseThrow(() -> new EntityNotFoundException("Integração da matrícula não encontrada"));
        return new IntegracaoMatriculaResponse(
                linha.getMatriculaId(),
                linha.getEventId(),
                linha.getPublicacao(),
                linha.getCobranca(),
                linha.getTentativasPublicacao(),
                linha.getTentativasCobranca()
        );
    }
}
