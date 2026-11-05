package br.sing.academic.application;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobrancaClientTest {

    @Test
    void primeiraVez201ERepeticao200() throws Exception {
        AtomicReference<String> corpo = new AtomicReference<>();
        AtomicInteger vezes = new AtomicInteger();
        HttpServer server = servidor((pedido, status) -> {
            corpo.set(pedido);
            int codigo = vezes.incrementAndGet() == 1 ? 201 : 200;
            status.set(codigo);
            return "{\"id\":44001,\"status\":\"pendente\"}";
        });
        try {
            CobrancaClient client = cliente(server);
            assertThat(client.criar(9001L, 15000, LocalDate.of(2026, 11, 15))).isEqualTo(44001L);
            assertThat(client.criar(9001L, 15000, LocalDate.of(2026, 11, 15))).isEqualTo(44001L);
            assertThat(corpo.get()).contains("\"matriculaId\":9001");
            assertThat(corpo.get()).contains("\"valorCentavos\":15000");
            assertThat(corpo.get()).contains("\"vencimento\":\"2026-11-15\"");
            assertThat(vezes.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void conflito409NaoViraRetry() throws Exception {
        HttpServer server = servidor((pedido, status) -> {
            status.set(409);
            return "{\"erro\":\"divergente\"}";
        });
        try {
            CobrancaClient client = cliente(server);
            assertThatThrownBy(() -> client.criar(9001L, 1, LocalDate.of(2026, 11, 15)))
                    .isInstanceOf(CobrancaConflitoException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void erro500EntraNoRetry() throws Exception {
        HttpServer server = servidor((pedido, status) -> {
            status.set(500);
            return "fora";
        });
        try {
            CobrancaClient client = cliente(server);
            assertThatThrownBy(() -> client.criar(9001L, 15000, LocalDate.of(2026, 11, 15)))
                    .isInstanceOf(CobrancaIndisponivelException.class);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void redeForaEntraNoRetry() {
        CobrancaClient client = new CobrancaClient(
                RestClient.builder().baseUrl("http://127.0.0.1:9").build());
        assertThatThrownBy(() -> client.criar(9001L, 15000, LocalDate.of(2026, 11, 15)))
                .isInstanceOf(CobrancaIndisponivelException.class);
    }

    private static CobrancaClient cliente(HttpServer server) {
        int porta = server.getAddress().getPort();
        return new CobrancaClient(RestClient.builder().baseUrl("http://127.0.0.1:" + porta).build());
    }

    private static HttpServer servidor(Resposta resposta) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/cobrancas", exchange -> {
            String pedido = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            AtomicInteger status = new AtomicInteger(201);
            String corpo = resposta.montar(pedido, status);
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return server;
    }

    @FunctionalInterface
    private interface Resposta {
        String montar(String pedido, AtomicInteger status);
    }
}
