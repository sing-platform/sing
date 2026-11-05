package br.sing.academic.application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CobrancaClient {

    private final RestClient restClient;

    @Autowired
    public CobrancaClient(RestClient.Builder builder, N2Properties props) {
        this(builder.baseUrl(semBarra(props.getFinanceBaseUrl())).build());
    }

    CobrancaClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Long criar(long matriculaId, int valorCentavos, LocalDate vencimento) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("matriculaId", matriculaId);
        corpo.put("valorCentavos", valorCentavos);
        corpo.put("vencimento", vencimento.toString());
        try {
            CobrancaResposta resposta = restClient.post()
                    .uri("/cobrancas")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        throw new CobrancaConflitoException();
                    })
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new CobrancaIndisponivelException("finance respondeu " + res.getStatusCode().value());
                    })
                    .body(CobrancaResposta.class);
            if (resposta == null || resposta.id() == null) {
                throw new CobrancaIndisponivelException("finance respondeu sem id");
            }
            return resposta.id();
        } catch (CobrancaConflitoException | CobrancaIndisponivelException e) {
            throw e;
        } catch (RuntimeException e) {
            if (causaConflito(e)) {
                throw new CobrancaConflitoException();
            }
            throw new CobrancaIndisponivelException(e.getMessage());
        }
    }

    private static boolean causaConflito(Throwable erro) {
        Throwable atual = erro;
        while (atual != null) {
            if (atual instanceof CobrancaConflitoException) {
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }

    private static String semBarra(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8082";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public record CobrancaResposta(Long id, String status) {}
}
