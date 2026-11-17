package br.com.blockready.exception;

/** Regra de negócio violada (HTTP 422). */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
