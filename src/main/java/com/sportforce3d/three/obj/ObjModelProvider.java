package com.sportforce3d.three.obj;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.BodyType;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelData.Focus;
import com.sportforce3d.three.model.ModelData.Part;
import com.sportforce3d.three.model.ModelProvider;
import com.sportforce3d.three.model.Zone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Carga un modelo OBJ exportado desde Blender (resources/models/{tipo}.obj). Cada objeto ("o" o "g") se asigna
 * a una zona por su nombre ({@link Zone#fromMeshName}); los que empiezan con "mannequin" o "body" forman el
 * maniquí; el resto se ignora con un aviso en el log. Se espera: metros, Y arriba, frente hacia +Z, caras en
 * sentido antihorario. El archivo .mtl se ignora: los colores y telas los pone el diseñador.
 */
public final class ObjModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(ObjModelProvider.class);

    @Override
    public String name() {
        return "OBJ";
    }

    /** Ruta del recurso para un tipo de cuerpo. */
    public static String resourceFor(BodyType type) {
        return "/models/" + type.key() + ".obj";
    }

    /** true si hay un OBJ empaquetado para ese tipo de cuerpo. */
    public static boolean available(BodyType type) {
        return ObjModelProvider.class.getResource(resourceFor(type)) != null;
    }

    @Override
    public ModelData load(BodyType type) {
        try (InputStream in = ObjModelProvider.class.getResourceAsStream(resourceFor(type))) {
            if (in == null) {
                throw new IllegalStateException("No existe " + resourceFor(type));
            }
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Objeto en construcción: caras con índices globales (v, vt, vn) ya resueltos (base 0, -1 = ausente). */
    private static final class ObjObject {
        final String name;
        final List<int[]> triangles = new ArrayList<>();

        ObjObject(String name) {
            this.name = name;
        }
    }

    public static ModelData parse(Reader reader, BodyType type) throws IOException {
        List<double[]> v = new ArrayList<>();
        List<double[]> vt = new ArrayList<>();
        List<double[]> vn = new ArrayList<>();
        Map<String, ObjObject> objects = new LinkedHashMap<>();
        ObjObject current = null;

        BufferedReader br = new BufferedReader(reader);
        String line;
        int lineNo = 0;
        while ((line = br.readLine()) != null) {
            lineNo++;
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] t = line.split("\\s+");
            switch (t[0]) {
                case "v" -> v.add(new double[]{d(t[1]), d(t[2]), d(t[3])});
                case "vt" -> vt.add(new double[]{d(t[1]), t.length > 2 ? d(t[2]) : 0});
                case "vn" -> vn.add(new double[]{d(t[1]), d(t[2]), d(t[3])});
                case "o", "g" -> {
                    String name = t.length > 1 ? line.substring(2).strip() : "sin_nombre";
                    current = objects.computeIfAbsent(name, ObjObject::new);
                }
                case "f" -> {
                    if (current == null) {
                        current = objects.computeIfAbsent("sin_nombre", ObjObject::new);
                    }
                    int n = t.length - 1;
                    int[][] corners = new int[n][];
                    for (int i = 0; i < n; i++) {
                        corners[i] = corner(t[i + 1], v.size(), vt.size(), vn.size(), lineNo);
                    }
                    // Polígonos en abanico.
                    for (int i = 1; i < n - 1; i++) {
                        current.triangles.add(concat(corners[0], corners[i], corners[i + 1]));
                    }
                }
                default -> {
                    // usemtl, mtllib, s, l… no se usan.
                }
            }
        }

        List<Part> mannequin = new ArrayList<>();
        Map<Zone, List<Part>> zones = new EnumMap<>(Zone.class);
        for (ObjObject o : objects.values()) {
            if (o.triangles.isEmpty()) {
                continue;
            }
            MeshData mesh = toMesh(o, v, vt, vn);
            String lower = o.name.toLowerCase(Locale.ROOT);
            Optional<Zone> zone = Zone.fromMeshName(lower);
            if (zone.isPresent()) {
                zones.computeIfAbsent(zone.get(), z -> new ArrayList<>()).add(new Part(o.name, mesh));
            } else if (lower.startsWith("mannequin") || lower.startsWith("body")) {
                mannequin.add(new Part(o.name, mesh));
            } else {
                log.warn("Objeto OBJ ignorado (nombre no reconocido): {}", o.name);
            }
        }
        double height = v.stream().mapToDouble(p -> p[1]).max().orElse(1.8);
        return new ModelData(type, mannequin, zones, focusFrom(zones, height), height);
    }

    private static MeshData toMesh(ObjObject o, List<double[]> v, List<double[]> vt, List<double[]> vn) {
        int count = o.triangles.size() * 3;
        float[] points = new float[count * 3];
        float[] normals = new float[count * 3];
        float[] tex = new float[count * 2];
        int[] faces = new int[o.triangles.size() * MeshData.FACE_STRIDE];
        // Normales suaves por vértice si el OBJ no trae.
        Map<Integer, Vec3> smooth = new LinkedHashMap<>();
        boolean needSmooth = o.triangles.stream().anyMatch(tri -> tri[2] < 0 || tri[5] < 0 || tri[8] < 0);
        if (needSmooth) {
            for (int[] tri : o.triangles) {
                Vec3 a = vec(v.get(tri[0]));
                Vec3 b = vec(v.get(tri[3]));
                Vec3 c = vec(v.get(tri[6]));
                Vec3 fn = b.sub(a).cross(c.sub(a));
                for (int k = 0; k < 3; k++) {
                    smooth.merge(tri[k * 3], fn, Vec3::add);
                }
            }
        }
        int i = 0;
        for (int[] tri : o.triangles) {
            for (int k = 0; k < 3; k++) {
                double[] p = v.get(tri[k * 3]);
                // Y arriba, +Z frente → JavaFX (Y abajo, frente hacia -Z): rotación de 180° en X.
                points[i * 3] = (float) p[0];
                points[i * 3 + 1] = (float) -p[1];
                points[i * 3 + 2] = (float) -p[2];
                Vec3 n = tri[k * 3 + 2] >= 0 ? vec(vn.get(tri[k * 3 + 2])).normalize()
                        : smooth.getOrDefault(tri[k * 3], Vec3.UP).normalize();
                normals[i * 3] = (float) n.x();
                normals[i * 3 + 1] = (float) -n.y();
                normals[i * 3 + 2] = (float) -n.z();
                if (tri[k * 3 + 1] >= 0) {
                    double[] uv = vt.get(tri[k * 3 + 1]);
                    tex[i * 2] = (float) uv[0];
                    tex[i * 2 + 1] = (float) (1 - uv[1]); // OBJ tiene V hacia arriba; JavaFX hacia abajo
                }
                int f = (i / 3) * MeshData.FACE_STRIDE + k * 3;
                faces[f] = i;
                faces[f + 1] = i;
                faces[f + 2] = i;
                i++;
            }
        }
        return new MeshData(points, normals, tex, faces);
    }

    /** Encuadres por prenda a partir de las cajas de sus zonas. */
    private static Map<Garment, Focus> focusFrom(Map<Zone, List<Part>> zones, double height) {
        Map<Garment, Focus> focus = new EnumMap<>(Garment.class);
        Map<Garment, double[]> boxes = new EnumMap<>(Garment.class);
        zones.forEach((zone, parts) -> {
            double[] box = boxes.computeIfAbsent(zone.garment(),
                    g -> new double[]{Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE});
            for (Part p : parts) {
                float[] pts = p.mesh().points();
                for (int k = 0; k < pts.length; k += 3) {
                    double y = -pts[k + 1];
                    double z = -pts[k + 2];
                    box[0] = Math.min(box[0], y);
                    box[1] = Math.min(box[1], z);
                    box[2] = Math.max(box[2], y);
                    box[3] = Math.max(box[3], z);
                }
            }
        });
        boxes.forEach((g, box) -> {
            double size = Math.max(0.12, box[2] - box[0]);
            focus.put(g, new Focus(new Vec3(0, (box[0] + box[2]) / 2, (box[1] + box[3]) / 2),
                    Math.max(0.9, size * 3.2), g == Garment.SHOES ? 18 : 4));
        });
        Focus full = new Focus(new Vec3(0, height / 2, 0), 4.0 * height / 1.812, 5);
        for (Garment g : Garment.values()) {
            focus.putIfAbsent(g, full);
        }
        focus.put(Garment.KIT, full);
        return focus;
    }

    private static int[] corner(String token, int nv, int nvt, int nvn, int lineNo) {
        String[] p = token.split("/", -1);
        int vi = index(p[0], nv, lineNo);
        int ti = p.length > 1 && !p[1].isEmpty() ? index(p[1], nvt, lineNo) : -1;
        int ni = p.length > 2 && !p[2].isEmpty() ? index(p[2], nvn, lineNo) : -1;
        return new int[]{vi, ti, ni};
    }

    /** Índices OBJ: base 1; negativos cuentan desde el final. */
    private static int index(String s, int size, int lineNo) {
        int i = Integer.parseInt(s);
        int idx = i < 0 ? size + i : i - 1;
        if (idx < 0 || idx >= size) {
            throw new IllegalArgumentException("Índice fuera de rango en la línea " + lineNo + ": " + s);
        }
        return idx;
    }

    private static int[] concat(int[] a, int[] b, int[] c) {
        return new int[]{a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]};
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }

    private static Vec3 vec(double[] p) {
        return new Vec3(p[0], p[1], p[2]);
    }
}
