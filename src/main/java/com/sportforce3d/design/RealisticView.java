package com.sportforce3d.design;

import java.util.List;
import java.util.Map;

/**
 * Lo que muestra la Vista realista de un diseño, calculado siempre del mismo {@link DesignDocument} que la Vista 3D
 * ({@link ViewSync#view}): la foto más parecida, sus colores por zona, el diseño de la camiseta, los textos con los
 * colores y la tipografía del 3D, el escudo del pecho y los avisos de lo que la foto no puede mostrar.
 *
 * @param spec    forma guardada (modelo, colores, diseño, nombre y número)
 * @param name    nombre de la espalda o null
 * @param back    número de la espalda o null
 * @param chest   número del pecho o null
 * @param crest   escudo o logo del pecho (el primero) o null
 * @param notices avisos breves, por ejemplo "Medias tobilleras: ver en Vista 3D" (vacío si la foto muestra todo)
 */
public record RealisticView(RealisticSpec spec, Text name, Text back, Text chest, DesignElement crest,
                            List<String> notices) {

    /** Texto con su color, contorno (null = sin contorno) y tipografía de {@link DesignFonts}. */
    public record Text(String value, String fill, String outline, String font) {
    }

    public RealisticView {
        notices = List.copyOf(notices);
    }

    public String modelKey() {
        return spec.modelKey();
    }

    public Map<String, String> zoneColors() {
        return spec.zoneColors();
    }
}
