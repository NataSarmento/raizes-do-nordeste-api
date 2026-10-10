package br.edu.raizes.domain.exception;

/** Requisição semanticamente inválida (HTTP 422). */
public class RegraInvalidaException extends RuntimeException {
    public RegraInvalidaException(String msg) { super(msg); }
}
