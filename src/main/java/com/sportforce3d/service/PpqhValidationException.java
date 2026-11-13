package com.sportforce3d.service;

import java.util.LinkedHashMap;
import java.util.Map;

public class PpqhValidationException extends PpqhServiceException {

    private final Map<String, String> ppqhFieldErrors;

    public PpqhValidationException(Map<String, String> ppqhFieldErrors) {
        super(ppqhFieldErrors.isEmpty() ? "Revisa los datos ingresados." : ppqhFieldErrors.values().iterator().next());
        this.ppqhFieldErrors = Map.copyOf(new LinkedHashMap<>(ppqhFieldErrors));
    }

    public static PpqhValidationException ppqhOf(String ppqhField, String ppqhMessage) {
        return new PpqhValidationException(Map.of(ppqhField, ppqhMessage));
    }

    public Map<String, String> ppqhFieldErrors() {
        return ppqhFieldErrors;
    }
}
