package com.ragworkshop.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DocumentProcessingException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> document(DocumentProcessingException exception) { return Map.of("success", false, "message", exception.getMessage()); }
    @ExceptionHandler({OllamaConnectionException.class, IllegalStateException.class}) @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> service(RuntimeException exception) { return Map.of("success", false, "message", exception.getMessage()); }
    @ExceptionHandler(Exception.class) @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> unexpected(Exception exception) { return Map.of("success", false, "message", "The request could not be completed."); }
}
