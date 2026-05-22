package com.project.soa.common.exception;

import java.time.Instant;
import java.util.List;


public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDto> fieldErrors
) {
    public ErrorResponseDto(int status, String error, String message, String path) {
        this(Instant.now(), status, error, message, path, null);
    }

    public ErrorResponseDto(int status, String error, String message, String path, List<FieldErrorDto> fieldErrors) {
        this(Instant.now(), status, error, message, path, fieldErrors);
    }

    public record FieldErrorDto(String field, String message) {}
}
