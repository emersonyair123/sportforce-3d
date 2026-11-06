package com.sportforce3d.viewmodel;

import com.sportforce3d.db.DataAccessException;
import com.sportforce3d.service.ServiceException;

/** Traduce excepciones a mensajes amables, sin trazas ni SQL. */
public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static String of(Throwable error) {
        Throwable e = error;
        while (e != null) {
            if (e instanceof ServiceException service) {
                return service.getMessage();
            }
            if (e instanceof DataAccessException) {
                return "No pudimos acceder a los datos de la tienda. Inténtalo de nuevo.";
            }
            if (e instanceof java.io.UncheckedIOException || e instanceof java.nio.file.AccessDeniedException) {
                return "No se pudo leer o guardar el archivo. Revisa que la carpeta exista y que el archivo no esté "
                        + "abierto en otro programa (p. ej. Excel).";
            }
            e = e.getCause();
        }
        return "Algo salió mal. Inténtalo de nuevo; si se repite, reinicia la aplicación.";
    }
}