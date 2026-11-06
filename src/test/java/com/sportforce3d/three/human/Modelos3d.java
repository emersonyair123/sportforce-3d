package com.sportforce3d.three.human;

import com.sportforce3d.model.Garment;

import java.util.Arrays;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Las pruebas de personas y prendas 3D necesitan {@code src/main/resources/models/humanos} y
 * {@code models/prendas} (MakeHuman y Blender, Etapas 8 y 9). Tras la reconstrucción del 2026-11-06 esos modelos
 * todavía no existen: sin ellos la app usa el maniquí de respaldo y estas pruebas se <b>omiten</b> (aparecen como
 * "skipped" en {@code ./mvnw verify}, no como aprobadas). En cuanto vuelvan los modelos se ejecutan solas.
 */
public final class Modelos3d {

    private Modelos3d() {
    }

    public static boolean personas() {
        return Arrays.stream(Persona.values()).allMatch(HumanModels::available);
    }

    public static boolean prendas() {
        return Arrays.stream(Persona.values()).allMatch(p -> SimulatedGarments.available(p, Garment.JERSEY)
                && SimulatedGarments.available(p, Garment.SHORTS) && SimulatedGarments.available(p, Garment.SOCKS));
    }

    public static void requierePersonas() {
        assumeTrue(personas(), "Faltan los modelos de las personas 3D (models/humanos): prueba omitida");
    }

    public static void requierePersonasYPrendas() {
        requierePersonas();
        assumeTrue(prendas(), "Faltan las prendas simuladas en Blender (models/prendas): prueba omitida");
    }
}
