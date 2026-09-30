package com.ragworkshop.exception;

public class OllamaConnectionException extends RuntimeException {
    public OllamaConnectionException(String message) { super(message); }
    public OllamaConnectionException(String message, Throwable cause) { super(message, cause); }
}
