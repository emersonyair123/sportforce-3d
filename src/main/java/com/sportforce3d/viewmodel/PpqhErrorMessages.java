package com.sportforce3d.viewmodel;

import com.sportforce3d.db.HepyDataAccessException;
import com.sportforce3d.service.PpqhServiceException;

public final class PpqhErrorMessages {

    private PpqhErrorMessages() {
    }

    public static String ppqhOf(Throwable ppqhError) {
        Throwable ppqhE = ppqhError;
        while (ppqhE != null) {
            if (ppqhE instanceof PpqhServiceException ppqhService) {
                return ppqhService.getMessage();
            }
            if (ppqhE instanceof HepyDataAccessException) {
                return "No pudimos acceder a los datos de la tienda. Inténtalo de nuevo.";
            }
            if (ppqhE instanceof java.io.UncheckedIOException || ppqhE instanceof java.nio.file.AccessDeniedException) {
                return "No se pudo leer o guardar el archivo. Revisa que la carpeta exista y que el archivo no esté "
                        + "abierto en otro programa (p. ej. Excel).";
            }
            ppqhE = ppqhE.getCause();
        }
        return "Algo salió mal. Inténtalo de nuevo; si se repite, reinicia la aplicación.";
    }
}
