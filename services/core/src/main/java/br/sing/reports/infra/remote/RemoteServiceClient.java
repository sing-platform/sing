package br.sing.reports.infra.remote;

import br.sing.exception.BusinessException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

abstract class RemoteServiceClient {

    static final int CONNECT_TIMEOUT_MS = 3_000;
    static final int READ_TIMEOUT_MS = 10_000;

    private final RestClient restClient;
    private final String serviceLabel;

    protected RemoteServiceClient(RestClient restClient, String serviceLabel) {
        this.restClient = restClient;
        this.serviceLabel = serviceLabel;
    }

    protected static RestClient buildRestClient(RestClient.Builder builder, String baseUrl, String defaultUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return builder.requestFactory(factory).baseUrl(withoutTrailingSlash(baseUrl, defaultUrl)).build();
    }

    protected <T> T get(Function<UriBuilder, URI> uri, ParameterizedTypeReference<T> type) {
        String bearer = RequestBearerToken.current();
        try {
            return restClient.get()
                    .uri(uri)
                    .headers(headers -> {
                        if (!bearer.isBlank()) {
                            headers.set(HttpHeaders.AUTHORIZATION, bearer);
                        }
                    })
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        throw remoteError(res.getStatusCode().value());
                    })
                    .body(type);
        } catch (BusinessException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new BusinessException(serviceLabel + " indisponível", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (RestClientException e) {
            throw new BusinessException(serviceLabel + " respondeu em formato inesperado", HttpStatus.BAD_GATEWAY);
        }
    }

    private BusinessException remoteError(int status) {
        return switch (status) {
            case 401 -> new BusinessException(serviceLabel + " rejeitou a autenticação", HttpStatus.UNAUTHORIZED);
            case 403 -> new BusinessException("Sem permissão no " + serviceLabel, HttpStatus.FORBIDDEN);
            case 503 -> new BusinessException(serviceLabel + " indisponível", HttpStatus.SERVICE_UNAVAILABLE);
            default -> new BusinessException(
                    serviceLabel + " respondeu com erro (HTTP " + status + ")", HttpStatus.BAD_GATEWAY);
        };
    }

    private static String withoutTrailingSlash(String url, String defaultUrl) {
        String value = url == null || url.isBlank() ? defaultUrl : url;
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
