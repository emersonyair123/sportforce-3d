package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.MeshData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lector propio de OBJ + MTL (sin librerías) para las personas de MakeHuman. Devuelve una malla por material
 * ({@code usemtl}) en coordenadas de JavaFX, sin objetos de JavaFX: se puede llamar en segundo plano.
 *
 * <ul>
 *   <li>OBJ: {@code v}, {@code vt}, {@code vn}, {@code f} (triángulos, cuadros y polígonos en abanico; formatos
 *       {@code v}, {@code v/vt}, {@code v//vn}, {@code v/vt/vn}; índices negativos), {@code usemtl} y
 *       {@code mtllib}. {@code o}, {@code g}, {@code s} y el resto se ignoran.</li>
 *   <li>Normales: si el OBJ no las trae (MakeHuman no las exporta) se calculan <b>suaves</b>: la suma de las
 *       normales de las caras que comparten el vértice, ponderadas por área.</li>
 *   <li>Coordenadas: metros, Y arriba, frente hacia +Z → JavaFX con la rotación de 180° en X {@code (x, -y, -z)};
 *       V de la textura se invierte (OBJ la tiene hacia arriba).</li>
 *   <li>MTL: {@code newmtl}, {@code Kd}, {@code Ks}, {@code Ns}, {@code map_Kd} y {@code map_D}. {@code d} y
 *       {@code Tr} se ignoran: MakeHuman escribe {@code d 0} (totalmente transparente) en todos sus materiales y
 *       la transparencia real va en el canal alfa de la textura.</li>
 *   <li>Rutas de textura: relativas a la carpeta del MTL; las absolutas o con {@code ..} se descartan.</li>
 * </ul>
 */
public final class ObjMtlReader {

    /** Material sin textura asignada en el OBJ. */
    public static final String DEFAULT_MATERIAL = "default";

    /** Abre un archivo relativo a la carpeta del OBJ; null si no existe. */
    @FunctionalInterface
    public interface Opener {
        Reader open(String relativePath) throws IOException;
    }

    /**
     * @param name       nombre del material ({@code newmtl})
     * @param diffuse    Kd (r, g, b en 0..1)
     * @param specular   Ks (r, g, b en 0..1)
     * @param shininess  Ns (0..1000)
     * @param diffuseMap ruta relativa de map_Kd o null
     * @param alphaMap   ruta relativa de map_D o null
     */
    public record Material(String name, double[] diffuse, double[] specular, double shininess, String diffuseMap,
                           String alphaMap) {

        static Material named(String name) {
            return new Material(name, new double[]{1, 1, 1}, new double[]{0, 0, 0}, 0, null, null);
        }

        Material with(double[] kd, double[] ks, Double ns, String mapKd, String mapD) {
            return new Material(name, kd != null ? kd : diffuse, ks != null ? ks : specular,
                    ns != null ? ns : shininess, mapKd != null ? mapKd : diffuseMap, mapD != null ? mapD : alphaMap);
        }
    }

    /** Malla de un material, en el orden en que aparece en el OBJ. */
    public record Group(String material, MeshData mesh) {
    }

    /**
     * @param groups    una malla por material
     * @param materials materiales leídos de los MTL (puede faltar alguno de los usados)
     * @param min       esquina mínima de la caja (espacio del modelo: Y arriba)
     * @param max       esquina máxima de la caja
     */
    public record Result(List<Group> groups, Map<String, Material> materials, double[] min, double[] max) {

        public double height() {
            return max[1] - min[1];
        }

        public Material material(String name) {
            return materials.getOrDefault(name, Material.named(name));
        }
    }

    private ObjMtlReader() {
    }

    // ---------------------------------------------------------------- OBJ

    /** Caras de un material: (v, vt, vn) por esquina, base 0, -1 = ausente; 9 enteros por triángulo. */
    private static final class Faces {
        int[] data = new int[9 * 256];
        int size;

        void add(int[] a, int[] b, int[] c) {
            if (size + 9 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            System.arraycopy(a, 0, data, size, 3);
            System.arraycopy(b, 0, data, size + 3, 3);
            System.arraycopy(c, 0, data, size + 6, 3);
            size += 9;
        }
    }

    /** Lista creciente de doubles (v, vt, vn). */
    private static final class Doubles {
        double[] data = new double[3 * 1024];
        int size;

        void add(double a, double b, double c) {
            if (size + 3 > data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[size++] = a;
            data[size++] = b;
            data[size++] = c;
        }

        int count() {
            return size / 3;
        }
    }

    public static Result read(Reader obj, Opener opener) throws IOException {
        Doubles v = new Doubles();
        Doubles vt = new Doubles();
        Doubles vn = new Doubles();
        Map<String, Faces> byMaterial = new LinkedHashMap<>();
        Map<String, Material> materials = new LinkedHashMap<>();
        Faces current = null;

        BufferedReader br = new BufferedReader(obj);
        String line;
        int lineNo = 0;
        while ((line = br.readLine()) != null) {
            lineNo++;
            line = line.strip();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] t = line.split("\\s+");
            switch (t[0]) {
                case "v" -> {
                    need(t, 4, lineNo);
                    v.add(d(t[1], lineNo), d(t[2], lineNo), d(t[3], lineNo));
                }
                case "vt" -> {
                    need(t, 2, lineNo);
                    vt.add(d(t[1], lineNo), t.length > 2 ? d(t[2], lineNo) : 0, 0);
                }
                case "vn" -> {
                    need(t, 4, lineNo);
                    vn.add(d(t[1], lineNo), d(t[2], lineNo), d(t[3], lineNo));
                }
                case "usemtl" -> current = byMaterial.computeIfAbsent(rest(line), k -> new Faces());
                case "mtllib" -> {
                    for (int i = 1; i < t.length; i++) {
                        String path = safePath(t[i]);
                        if (path == null) {
                            continue;
                        }
                        try (Reader mtl = opener.open(path)) {
                            if (mtl != null) {
                                materials.putAll(readMtl(mtl, folderOf(path)));
                            }
                        }
                    }
                }
                case "f" -> {
                    need(t, 4, lineNo);
                    if (current == null) {
                        current = byMaterial.computeIfAbsent(DEFAULT_MATERIAL, k -> new Faces());
                    }
                    int n = t.length - 1;
                    int[][] corners = new int[n][];
                    for (int i = 0; i < n; i++) {
                        corners[i] = corner(t[i + 1], v.count(), vt.count(), vn.count(), lineNo);
                    }
                    for (int i = 1; i < n - 1; i++) {
                        current.add(corners[0], corners[i], corners[i + 1]);
                    }
                }
                default -> {
                    // o, g, s, l, p… no se usan.
                }
            }
        }

        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, Faces> e : byMaterial.entrySet()) {
            if (e.getValue().size > 0) {
                groups.add(new Group(e.getKey(), toMesh(e.getValue(), v, vt, vn, min, max)));
            }
        }
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("El OBJ no tiene caras");
        }
        return new Result(Collections.unmodifiableList(groups), Collections.unmodifiableMap(materials), min, max);
    }

    private static MeshData toMesh(Faces faces, Doubles v, Doubles vt, Doubles vn, double[] min, double[] max) {
        int[] f = faces.data;
        int corners = faces.size / 3;

        // Índices locales: solo los vértices y coordenadas de textura que usa este material.
        int[] pointOf = new int[v.count()];
        int[] texOf = new int[vt.count()];
        int[] normalOf = new int[vn.count()];
        Arrays.fill(pointOf, -1);
        Arrays.fill(texOf, -1);
        Arrays.fill(normalOf, -1);
        int points = 0;
        int texCoords = 0;
        int explicitNormals = 0;
        boolean missingTex = false;
        for (int c = 0; c < corners; c++) {
            int vi = f[c * 3];
            int ti = f[c * 3 + 1];
            int ni = f[c * 3 + 2];
            if (pointOf[vi] < 0) {
                pointOf[vi] = points++;
            }
            if (ti < 0) {
                missingTex = true;
            } else if (texOf[ti] < 0) {
                texOf[ti] = texCoords++;
            }
            if (ni >= 0 && normalOf[ni] < 0) {
                normalOf[ni] = explicitNormals++;
            }
        }
        int noTex = missingTex ? texCoords++ : -1;

        float[] pts = new float[points * 3];
        for (int vi = 0; vi < pointOf.length; vi++) {
            int p = pointOf[vi];
            if (p >= 0) {
                double x = v.data[vi * 3];
                double y = v.data[vi * 3 + 1];
                double z = v.data[vi * 3 + 2];
                pts[p * 3] = (float) x;
                pts[p * 3 + 1] = (float) -y;
                pts[p * 3 + 2] = (float) -z;
                min[0] = Math.min(min[0], x);
                min[1] = Math.min(min[1], y);
                min[2] = Math.min(min[2], z);
                max[0] = Math.max(max[0], x);
                max[1] = Math.max(max[1], y);
                max[2] = Math.max(max[2], z);
            }
        }
        float[] tex = new float[texCoords * 2];
        for (int ti = 0; ti < texOf.length; ti++) {
            int t = texOf[ti];
            if (t >= 0) {
                tex[t * 2] = (float) vt.data[ti * 3];
                tex[t * 2 + 1] = (float) (1 - vt.data[ti * 3 + 1]);
            }
        }

        // Normales suaves por vértice (ya en coordenadas de JavaFX) y, a continuación, las explícitas del OBJ.
        double[] smooth = new double[points * 3];
        for (int c = 0; c < corners; c += 3) {
            int a = pointOf[f[c * 3]];
            int b = pointOf[f[(c + 1) * 3]];
            int d = pointOf[f[(c + 2) * 3]];
            double ux = pts[b * 3] - pts[a * 3];
            double uy = pts[b * 3 + 1] - pts[a * 3 + 1];
            double uz = pts[b * 3 + 2] - pts[a * 3 + 2];
            double wx = pts[d * 3] - pts[a * 3];
            double wy = pts[d * 3 + 1] - pts[a * 3 + 1];
            double wz = pts[d * 3 + 2] - pts[a * 3 + 2];
            // El producto cruz mide el doble del área: las caras grandes pesan más.
            double nx = uy * wz - uz * wy;
            double ny = uz * wx - ux * wz;
            double nz = ux * wy - uy * wx;
            for (int k : new int[]{a, b, d}) {
                smooth[k * 3] += nx;
                smooth[k * 3 + 1] += ny;
                smooth[k * 3 + 2] += nz;
            }
        }
        float[] normals = new float[(points + explicitNormals) * 3];
        for (int p = 0; p < points; p++) {
            putNormal(normals, p, smooth[p * 3], smooth[p * 3 + 1], smooth[p * 3 + 2]);
        }
        for (int ni = 0; ni < normalOf.length; ni++) {
            int n = normalOf[ni];
            if (n >= 0) {
                putNormal(normals, points + n, vn.data[ni * 3], -vn.data[ni * 3 + 1], -vn.data[ni * 3 + 2]);
            }
        }

        int[] out = new int[corners * 3];
        for (int c = 0; c < corners; c++) {
            int p = pointOf[f[c * 3]];
            int ti = f[c * 3 + 1];
            int ni = f[c * 3 + 2];
            out[c * 3] = p;
            out[c * 3 + 1] = ni >= 0 ? points + normalOf[ni] : p;
            out[c * 3 + 2] = ti >= 0 ? texOf[ti] : noTex;
        }
        return new MeshData(pts, normals, tex, out);
    }

    private static void putNormal(float[] normals, int i, double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len < 1e-12) {
            x = 0;
            y = -1; // "arriba" en JavaFX
            z = 0;
            len = 1;
        }
        normals[i * 3] = (float) (x / len);
        normals[i * 3 + 1] = (float) (y / len);
        normals[i * 3 + 2] = (float) (z / len);
    }

    private static int[] corner(String token, int nv, int nvt, int nvn, int lineNo) {
        String[] p = token.split("/", -1);
        int vi = index(p[0], nv, lineNo);
        int ti = p.length > 1 && !p[1].isEmpty() ? index(p[1], nvt, lineNo) : -1;
        int ni = p.length > 2 && !p[2].isEmpty() ? index(p[2], nvn, lineNo) : -1;
        return new int[]{vi, ti, ni};
    }

    /** Índices OBJ: base 1; negativos cuentan desde el final de lo leído hasta esa línea. */
    private static int index(String s, int size, int lineNo) {
        int i;
        try {
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Índice no válido en la línea " + lineNo + ": " + s, e);
        }
        int idx = i < 0 ? size + i : i - 1;
        if (i == 0 || idx < 0 || idx >= size) {
            throw new IllegalArgumentException("Índice fuera de rango en la línea " + lineNo + ": " + s);
        }
        return idx;
    }

    // ---------------------------------------------------------------- MTL

    /** Lee un MTL. Las rutas de textura quedan relativas a la carpeta del OBJ ({@code folder} + archivo). */
    public static Map<String, Material> readMtl(Reader reader, String folder) throws IOException {
        Map<String, Material> out = new LinkedHashMap<>();
        BufferedReader br = new BufferedReader(reader);
        String line;
        int lineNo = 0;
        Material m = null;
        while ((line = br.readLine()) != null) {
            lineNo++;
            line = line.strip();
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] t = line.split("\\s+");
            String key = t[0];
            if (key.equals("newmtl")) {
                m = Material.named(rest(line));
                out.put(m.name(), m);
                continue;
            }
            if (m == null) {
                continue;
            }
            Material next = switch (key) {
                case "Kd" -> m.with(rgb(t, lineNo), null, null, null, null);
                case "Ks" -> m.with(null, rgb(t, lineNo), null, null, null);
                case "Ns" -> {
                    need(t, 2, lineNo);
                    yield m.with(null, null, d(t[1], lineNo), null, null);
                }
                // La ruta es el último elemento: las opciones (-s, -o, -bm…) van antes.
                case "map_Kd" -> m.with(null, null, null, texture(folder, t[t.length - 1]), null);
                case "map_D", "map_d" -> m.with(null, null, null, null, texture(folder, t[t.length - 1]));
                default -> m; // d, Tr, Ka, illum, map_Bump… no se usan
            };
            if (next != m) {
                m = next;
                out.put(m.name(), m);
            }
        }
        return out;
    }

    private static String texture(String folder, String file) {
        String path = safePath(file);
        return path == null ? null : folder + path;
    }

    /**
     * Ruta relativa segura con barras normales; null si es absoluta o sube de carpeta ({@code ..}), para que un
     * MTL no pueda leer archivos fuera del modelo.
     */
    static String safePath(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String p = raw.strip().replace('\\', '/');
        if (p.startsWith("/") || p.matches("^[A-Za-z]:.*") || p.contains(":")) {
            return null;
        }
        for (String part : p.split("/")) {
            if (part.equals("..")) {
                return null;
            }
        }
        while (p.startsWith("./")) {
            p = p.substring(2);
        }
        return p.isEmpty() ? null : p;
    }

    private static String folderOf(String path) {
        int i = path.lastIndexOf('/');
        return i < 0 ? "" : path.substring(0, i + 1);
    }

    // ---------------------------------------------------------------- Ayudas

    private static double[] rgb(String[] t, int lineNo) {
        need(t, 2, lineNo);
        double r = d(t[1], lineNo);
        // "Kd 0.5" vale como gris.
        double g = t.length > 2 ? d(t[2], lineNo) : r;
        double b = t.length > 3 ? d(t[3], lineNo) : r;
        return new double[]{clamp01(r), clamp01(g), clamp01(b)};
    }

    private static double clamp01(double x) {
        return Math.max(0, Math.min(1, x));
    }

    private static String rest(String line) {
        int i = line.indexOf(' ');
        int tab = line.indexOf('\t');
        if (i < 0 || (tab >= 0 && tab < i)) {
            i = tab;
        }
        String r = i < 0 ? "" : line.substring(i).strip();
        return r.isEmpty() ? DEFAULT_MATERIAL : r;
    }

    private static void need(String[] t, int n, int lineNo) {
        if (t.length < n) {
            throw new IllegalArgumentException("Línea " + lineNo + " incompleta: " + String.join(" ", t));
        }
    }

    private static double d(String s, int lineNo) {
        try {
            double x = Double.parseDouble(s);
            if (!Double.isFinite(x)) {
                throw new NumberFormatException(s);
            }
            return x;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Número no válido en la línea " + lineNo + ": " + s, e);
        }
    }
}
