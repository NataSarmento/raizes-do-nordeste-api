package br.edu.raizes.domain.exception;

/** Violação de regra de negócio por estado do sistema (HTTP 409). */
public class ConflitoException extends RuntimeException {
    public ConflitoException(String msg) { super(msg); }
}
