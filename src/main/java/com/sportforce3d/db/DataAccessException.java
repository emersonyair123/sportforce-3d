package com.sportforce3d.db;

/** Error de acceso a datos sin detalles de SQL para la interfaz. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
