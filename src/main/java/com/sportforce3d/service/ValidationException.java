package com.sportforce3d.service;

import java.util.LinkedHashMap;
import java.util.Map;

/** Datos de entrada inválidos. {@link #fieldErrors()} asocia cada campo con su mensaje. */
public class ValidationException extends ServiceException {

    private final Map<String, String> fieldErrors;

    public ValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors.isEmpty() ? "Revisa los datos ingresados." : fieldErrors.values().iterator().next());
        this.fieldErrors = Map.copyOf(new LinkedHashMap<>(fieldErrors));
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException(Map.of(field, message));
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
