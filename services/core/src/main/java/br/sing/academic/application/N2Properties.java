package br.sing.academic.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "sing.n2")
public class N2Properties {

    /** Valor fictício da cobrança, em centavos. */
    private int cobrancaValorCentavos = 15000;

    /** Dias somados à data da matrícula para achar o vencimento. */
    private int cobrancaPrazoDias = 10;

    private Duration retryIntervalo = Duration.ofSeconds(30);

    private int retryLimite = 10;

    private int claimSegundos = 60;

    private String financeBaseUrl = "http://localhost:8082";

    private long varreduraMs = 5000;
}
