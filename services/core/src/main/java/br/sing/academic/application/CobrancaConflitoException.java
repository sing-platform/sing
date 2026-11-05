package br.sing.academic.application;

/** O finance respondeu 409: o corpo diverge da cobrança já gravada. Não tem retry. */
public class CobrancaConflitoException extends RuntimeException {

    public CobrancaConflitoException() {
        super("Cobrança em conflito para esta matrícula");
    }
}
