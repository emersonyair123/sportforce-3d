package com.sportforce3d.three.human;

import com.sportforce3d.design.GarmentCut;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelData.Part;
import com.sportforce3d.three.model.Zone;
import com.sportforce3d.three.obj.ObjModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prendas con la caída de la tela simulada en Blender ({@code tools/blender/generar_prendas.py}): una carpeta por
 * persona y corte en {@code resources/models/prendas/<persona>/<corte>/} con {@code prenda.obj} (un objeto por zona,
 * con las islas UV de UvLayout), {@code ao.png} (oclusión ambiental horneada con Cycles) y {@code normal.png}
 * (normal map de los pliegues). Las que faltan se ajustan con {@link OutfitFitter} como antes.
 *
 * <p>Etapa 10: cada largo es otra carpeta ({@link #folder(Persona, Garment, GarmentCut)}): manga larga
 * {@code camiseta_futbol_v_larga}, short {@code short_futbol_corto}/{@code short_futbol_largo} y medias
 * {@code medias_futbol_tobillera}/{@code medias_futbol_media}; el corte de siempre no lleva sufijo.
 */
public final class SimulatedGarments {

    private static final Logger log = LoggerFactory.getLogger(SimulatedGarments.class);

    /** Corte de cada prenda simulada (nombre de la carpeta). */
    static final Map<Garment, String> CUTS = Map.of(Garment.JERSEY, "camiseta_futbol_v", Garment.SHORTS, "short_futbol",
            Garment.SOCKS, "medias_futbol");

    /** Bordes de la tela (cuello, puños, bastas): el Solidify los cierra con una cara casi perpendicular. */
    private static final double RIM_COS = Math.cos(Math.toRadians(70));

    /**
     * Una prenda simulada.
     *
     * @param zones    mallas por zona (espacio de JavaFX, como el resto del visor)
     * @param shading  oclusión y normal map horneados
     * @param openings puntos de los bordes de la tela (espacio del modelo), por donde la piel se puede ver
     */
    public record Piece(Map<Zone, List<Part>> zones, ClothShading shading, List<Vec3> openings) {
    }

    private SimulatedGarments() {
    }

    static String folder(Persona persona, Garment garment) {
        return folder(persona, garment, GarmentCut.DEFAULT);
    }

    /** Carpeta de la prenda con ese largo (el corte de siempre no lleva sufijo). */
    static String folder(Persona persona, Garment garment, GarmentCut cut) {
        String suffix = switch (garment) {
            case JERSEY -> cut.sleeve() == GarmentCut.Sleeve.LARGA ? "_larga" : "";
            case SHORTS -> switch (cut.shorts()) {
                case CORTO -> "_corto";
                case MEDIO -> "";
                case LARGO -> "_largo";
            };
            case SOCKS -> switch (cut.socks()) {
                case TOBILLERA -> "_tobillera";
                case MEDIA -> "_media";
                case LARGA -> "";
            };
            default -> "";
        };
        return "/models/prendas/" + persona.key() + "/" + CUTS.get(garment) + suffix + "/";
    }

    public static boolean available(Persona persona, Garment garment) {
        return available(persona, garment, GarmentCut.DEFAULT);
    }

    public static boolean available(Persona persona, Garment garment, GarmentCut cut) {
        return CUTS.containsKey(garment)
                && SimulatedGarments.class.getResource(folder(persona, garment, cut) + "prenda.obj") != null;
    }

    /** Prendas simuladas de la persona que estén empaquetadas (vacío si ninguna). Lento: segundo plano. */
    public static Map<Garment, Piece> load(Persona persona) {
        return load(persona, GarmentCut.DEFAULT);
    }

    /**
     * Prendas simuladas con el largo pedido; si falta una variante se usa la de siempre (con un aviso en el
     * registro) para que el diseñador siga funcionando.
     */
    public static Map<Garment, Piece> load(Persona persona, GarmentCut cut) {
        Map<Garment, Piece> out = new EnumMap<>(Garment.class);
        for (Garment g : CUTS.keySet()) {
            GarmentCut use = cut;
            if (!available(persona, g, cut)) {
                if (!available(persona, g)) {
                    continue;
                }
                log.warn("Falta la prenda simulada {}: se usa {}", folder(persona, g, cut), folder(persona, g));
                use = GarmentCut.DEFAULT;
            }
            try {
                out.put(g, read(persona, g, use));
            } catch (IOException | RuntimeException e) {
                // Sin la prenda simulada se usa el ajuste de OutfitFitter: el diseñador sigue funcionando.
                log.warn("No se pudo cargar la prenda simulada {}: {}", folder(persona, g, use), e.toString());
            }
        }
        return out;
    }

    static Piece read(Persona persona, Garment garment) throws IOException {
        return read(persona, garment, GarmentCut.DEFAULT);
    }

    static Piece read(Persona persona, Garment garment, GarmentCut cut) throws IOException {
        String dir = folder(persona, garment, cut);
        try (InputStream obj = open(dir + "prenda.obj"); InputStream ao = open(dir + "ao.png");
             InputStream normal = open(dir + "normal.png")) {
            return read(new InputStreamReader(obj, StandardCharsets.UTF_8), ImageIO.read(ao), ImageIO.read(normal),
                    persona);
        }
    }

    static Piece read(Reader obj, BufferedImage ao, BufferedImage normal, Persona persona) throws IOException {
        if (ao == null || normal == null) {
            throw new IOException("ao.png o normal.png ilegibles");
        }
        ModelData data = ObjModelProvider.parse(obj, persona.fallback());
        if (data.zones().isEmpty()) {
            throw new IOException("prenda.obj sin zonas reconocidas");
        }
        List<Vec3> openings = new ArrayList<>();
        data.zones().values().forEach(parts -> parts.forEach(p -> openings.addAll(rims(p.mesh()))));
        return new Piece(data.zones(), ClothShading.fromMaps(ao, normal), openings);
    }

    /** Puntos (espacio del modelo) de las aristas donde la tela dobla en ángulo cerrado: el canto de los bordes. */
    static List<Vec3> rims(MeshData mesh) {
        float[] p = mesh.points();
        int[] f = mesh.faces();
        // El OBJ trae cada esquina por separado: se sueldan los puntos en la misma posición (0,01 mm).
        Map<List<Integer>, Integer> ids = new HashMap<>();
        int[] canon = new int[mesh.pointCount()];
        List<Integer> first = new ArrayList<>();
        for (int i = 0; i < canon.length; i++) {
            List<Integer> key = List.of(Math.round(p[i * 3] * 1e5f), Math.round(p[i * 3 + 1] * 1e5f),
                    Math.round(p[i * 3 + 2] * 1e5f));
            Integer id = ids.get(key);
            if (id == null) {
                id = first.size();
                ids.put(key, id);
                first.add(i);
            }
            canon[i] = id;
        }
        int faces = mesh.faceCount();
        double[][] normals = new double[faces][];
        Map<Long, int[]> edges = new HashMap<>();
        for (int t = 0; t < faces; t++) {
            int b = t * MeshData.FACE_STRIDE;
            int a = f[b];
            int c = f[b + 3];
            int d = f[b + 6];
            double ux = p[c * 3] - p[a * 3];
            double uy = p[c * 3 + 1] - p[a * 3 + 1];
            double uz = p[c * 3 + 2] - p[a * 3 + 2];
            double wx = p[d * 3] - p[a * 3];
            double wy = p[d * 3 + 1] - p[a * 3 + 1];
            double wz = p[d * 3 + 2] - p[a * 3 + 2];
            double nx = uy * wz - uz * wy;
            double ny = uz * wx - ux * wz;
            double nz = ux * wy - uy * wx;
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            normals[t] = len < 1e-15 ? new double[3] : new double[]{nx / len, ny / len, nz / len};
            int[] v = {canon[a], canon[c], canon[d]};
            for (int k = 0; k < 3; k++) {
                int v0 = Math.min(v[k], v[(k + 1) % 3]);
                int v1 = Math.max(v[k], v[(k + 1) % 3]);
                long key = ((long) v0 << 32) | v1;
                int[] pair = edges.computeIfAbsent(key, x -> new int[]{-1, -1});
                pair[pair[0] < 0 ? 0 : 1] = t;
            }
        }
        boolean[] rim = new boolean[first.size()];
        edges.forEach((key, pair) -> {
            if (pair[1] >= 0) {
                double[] n0 = normals[pair[0]];
                double[] n1 = normals[pair[1]];
                if (n0[0] * n1[0] + n0[1] * n1[1] + n0[2] * n1[2] < RIM_COS) {
                    rim[(int) (key >>> 32)] = true;
                    rim[(int) (key & 0xFFFFFFFFL)] = true;
                }
            }
        });
        List<Vec3> out = new ArrayList<>();
        for (int c = 0; c < rim.length; c++) {
            if (rim[c]) {
                int i = first.get(c) * 3;
                out.add(new Vec3(p[i], -p[i + 1], -p[i + 2]));
            }
        }
        return out;
    }

    private static InputStream open(String resource) throws IOException {
        InputStream in = SimulatedGarments.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IOException("Falta " + resource);
        }
        return in;
    }
}
