package br.sing.academic.application;

/** Rede, 5xx ou outra resposta que ainda pode ser tentada de novo. */
public class CobrancaIndisponivelException extends RuntimeException {

    public CobrancaIndisponivelException(String message) {
        super(message);
    }
}
