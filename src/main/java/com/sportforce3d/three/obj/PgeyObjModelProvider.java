package com.sportforce3d.three.obj;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyBodyType;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelData.PgeyFocus;
import com.sportforce3d.three.model.PgeyModelData.PgeyPart;
import com.sportforce3d.three.model.PgeyModelProvider;
import com.sportforce3d.three.model.PgeyZone;
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

public final class PgeyObjModelProvider implements PgeyModelProvider {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeyObjModelProvider.class);

    @Override
    public String pgeyName() {
        return "OBJ";
    }

    public static String pgeyResourceFor(PgeyBodyType pgeyType) {
        return "/models/" + pgeyType.pgeyKey() + ".obj";
    }

    public static boolean pgeyAvailable(PgeyBodyType pgeyType) {
        return PgeyObjModelProvider.class.getResource(pgeyResourceFor(pgeyType)) != null;
    }

    @Override
    public PgeyModelData pgeyLoad(PgeyBodyType pgeyType) {
        try (InputStream pgeyIn = PgeyObjModelProvider.class.getResourceAsStream(pgeyResourceFor(pgeyType))) {
            if (pgeyIn == null) {
                throw new IllegalStateException("No existe " + pgeyResourceFor(pgeyType));
            }
            return pgeyParse(new InputStreamReader(pgeyIn, StandardCharsets.UTF_8), pgeyType);
        } catch (IOException pgeyE) {
            throw new UncheckedIOException(pgeyE);
        }
    }

    private static final class PgeyObjObject {
        final String pgeyName;
        final List<int[]> pgeyTriangles = new ArrayList<>();

        PgeyObjObject(String pgeyName) {
            this.pgeyName = pgeyName;
        }
    }

    public static PgeyModelData pgeyParse(Reader pgeyReader, PgeyBodyType pgeyType) throws IOException {
        List<double[]> pgeyV = new ArrayList<>();
        List<double[]> pgeyVt = new ArrayList<>();
        List<double[]> pgeyVn = new ArrayList<>();
        Map<String, PgeyObjObject> pgeyObjects = new LinkedHashMap<>();
        PgeyObjObject pgeyCurrent = null;

        BufferedReader pgeyBr = new BufferedReader(pgeyReader);
        String pgeyLine;
        int pgeyLineNo = 0;
        while ((pgeyLine = pgeyBr.readLine()) != null) {
            pgeyLineNo++;
            pgeyLine = pgeyLine.strip();
            if (pgeyLine.isEmpty() || pgeyLine.startsWith("#")) {
                continue;
            }
            String[] pgeyT = pgeyLine.split("\\s+");
            switch (pgeyT[0]) {
                case "v" -> pgeyV.add(new double[]{pgeyD(pgeyT[1]), pgeyD(pgeyT[2]), pgeyD(pgeyT[3])});
                case "vt" -> pgeyVt.add(new double[]{pgeyD(pgeyT[1]), pgeyT.length > 2 ? pgeyD(pgeyT[2]) : 0});
                case "vn" -> pgeyVn.add(new double[]{pgeyD(pgeyT[1]), pgeyD(pgeyT[2]), pgeyD(pgeyT[3])});
                case "o", "g" -> {
                    String pgeyName = pgeyT.length > 1 ? pgeyLine.substring(2).strip() : "sin_nombre";
                    pgeyCurrent = pgeyObjects.computeIfAbsent(pgeyName, PgeyObjObject::new);
                }
                case "f" -> {
                    if (pgeyCurrent == null) {
                        pgeyCurrent = pgeyObjects.computeIfAbsent("sin_nombre", PgeyObjObject::new);
                    }
                    int pgeyN = pgeyT.length - 1;
                    int[][] pgeyCorners = new int[pgeyN][];
                    for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                        pgeyCorners[pgeyI] = pgeyCorner(pgeyT[pgeyI + 1], pgeyV.size(), pgeyVt.size(), pgeyVn.size(), pgeyLineNo);
                    }
                    for (int pgeyI = 1; pgeyI < pgeyN - 1; pgeyI++) {
                        pgeyCurrent.pgeyTriangles.add(pgeyConcat(pgeyCorners[0], pgeyCorners[pgeyI], pgeyCorners[pgeyI + 1]));
                    }
                }
                default -> {
                }
            }
        }

        List<PgeyPart> pgeyMannequin = new ArrayList<>();
        Map<PgeyZone, List<PgeyPart>> pgeyZones = new EnumMap<>(PgeyZone.class);
        for (PgeyObjObject pgeyO : pgeyObjects.values()) {
            if (pgeyO.pgeyTriangles.isEmpty()) {
                continue;
            }
            PgeyMeshData pgeyMesh = pgeyToMesh(pgeyO, pgeyV, pgeyVt, pgeyVn);
            String pgeyLower = pgeyO.pgeyName.toLowerCase(Locale.ROOT);
            Optional<PgeyZone> pgeyZone = PgeyZone.pgeyFromMeshName(pgeyLower);
            if (pgeyZone.isPresent()) {
                pgeyZones.computeIfAbsent(pgeyZone.get(), pgeyZ -> new ArrayList<>()).add(new PgeyPart(pgeyO.pgeyName, pgeyMesh));
            } else if (pgeyLower.startsWith("mannequin") || pgeyLower.startsWith("body")) {
                pgeyMannequin.add(new PgeyPart(pgeyO.pgeyName, pgeyMesh));
            } else {
                pgeyLog.warn("Objeto OBJ ignorado (nombre no reconocido): {}", pgeyO.pgeyName);
            }
        }
        double pgeyHeight = pgeyV.stream().mapToDouble(pgeyP -> pgeyP[1]).max().orElse(1.8);
        return new PgeyModelData(pgeyType, pgeyMannequin, pgeyZones, pgeyFocusFrom(pgeyZones, pgeyHeight), pgeyHeight);
    }

    private static PgeyMeshData pgeyToMesh(PgeyObjObject pgeyO, List<double[]> pgeyV, List<double[]> pgeyVt, List<double[]> pgeyVn) {
        int pgeyCount = pgeyO.pgeyTriangles.size() * 3;
        float[] pgeyPoints = new float[pgeyCount * 3];
        float[] pgeyNormals = new float[pgeyCount * 3];
        float[] pgeyTex = new float[pgeyCount * 2];
        int[] pgeyFaces = new int[pgeyO.pgeyTriangles.size() * PgeyMeshData.PGEY_FACE_STRIDE];
        Map<Integer, PgeyVec3> pgeySmooth = new LinkedHashMap<>();
        boolean pgeyNeedSmooth = pgeyO.pgeyTriangles.stream().anyMatch(pgeyTri -> pgeyTri[2] < 0 || pgeyTri[5] < 0 || pgeyTri[8] < 0);
        if (pgeyNeedSmooth) {
            for (int[] pgeyTri : pgeyO.pgeyTriangles) {
                PgeyVec3 pgeyA = pgeyVec(pgeyV.get(pgeyTri[0]));
                PgeyVec3 pgeyB = pgeyVec(pgeyV.get(pgeyTri[3]));
                PgeyVec3 pgeyC = pgeyVec(pgeyV.get(pgeyTri[6]));
                PgeyVec3 pgeyFn = pgeyB.pgeySub(pgeyA).pgeyCross(pgeyC.pgeySub(pgeyA));
                for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                    pgeySmooth.merge(pgeyTri[pgeyK * 3], pgeyFn, PgeyVec3::pgeyAdd);
                }
            }
        }
        int pgeyI = 0;
        for (int[] pgeyTri : pgeyO.pgeyTriangles) {
            for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                double[] pgeyP = pgeyV.get(pgeyTri[pgeyK * 3]);
                pgeyPoints[pgeyI * 3] = (float) pgeyP[0];
                pgeyPoints[pgeyI * 3 + 1] = (float) -pgeyP[1];
                pgeyPoints[pgeyI * 3 + 2] = (float) -pgeyP[2];
                PgeyVec3 pgeyN = pgeyTri[pgeyK * 3 + 2] >= 0 ? pgeyVec(pgeyVn.get(pgeyTri[pgeyK * 3 + 2])).pgeyNormalize()
                        : pgeySmooth.getOrDefault(pgeyTri[pgeyK * 3], PgeyVec3.PGEY_UP).pgeyNormalize();
                pgeyNormals[pgeyI * 3] = (float) pgeyN.pgeyX();
                pgeyNormals[pgeyI * 3 + 1] = (float) -pgeyN.pgeyY();
                pgeyNormals[pgeyI * 3 + 2] = (float) -pgeyN.pgeyZ();
                if (pgeyTri[pgeyK * 3 + 1] >= 0) {
                    double[] pgeyUv = pgeyVt.get(pgeyTri[pgeyK * 3 + 1]);
                    pgeyTex[pgeyI * 2] = (float) pgeyUv[0];
                    pgeyTex[pgeyI * 2 + 1] = (float) (1 - pgeyUv[1]);
                }
                int pgeyF = (pgeyI / 3) * PgeyMeshData.PGEY_FACE_STRIDE + pgeyK * 3;
                pgeyFaces[pgeyF] = pgeyI;
                pgeyFaces[pgeyF + 1] = pgeyI;
                pgeyFaces[pgeyF + 2] = pgeyI;
                pgeyI++;
            }
        }
        return new PgeyMeshData(pgeyPoints, pgeyNormals, pgeyTex, pgeyFaces);
    }

    private static Map<HepyGarment, PgeyFocus> pgeyFocusFrom(Map<PgeyZone, List<PgeyPart>> pgeyZones, double pgeyHeight) {
        Map<HepyGarment, PgeyFocus> pgeyFocus = new EnumMap<>(HepyGarment.class);
        Map<HepyGarment, double[]> pgeyBoxes = new EnumMap<>(HepyGarment.class);
        pgeyZones.forEach((pgeyZone, pgeyParts) -> {
            double[] pgeyBox = pgeyBoxes.computeIfAbsent(pgeyZone.pgeyGarment(),
                    pgeyG -> new double[]{Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE});
            for (PgeyPart pgeyP : pgeyParts) {
                float[] pgeyPts = pgeyP.pgeyMesh().pgeyPoints();
                for (int pgeyK = 0; pgeyK < pgeyPts.length; pgeyK += 3) {
                    double pgeyY = -pgeyPts[pgeyK + 1];
                    double pgeyZ = -pgeyPts[pgeyK + 2];
                    pgeyBox[0] = Math.min(pgeyBox[0], pgeyY);
                    pgeyBox[1] = Math.min(pgeyBox[1], pgeyZ);
                    pgeyBox[2] = Math.max(pgeyBox[2], pgeyY);
                    pgeyBox[3] = Math.max(pgeyBox[3], pgeyZ);
                }
            }
        });
        pgeyBoxes.forEach((pgeyG, pgeyBox) -> {
            double pgeySize = Math.max(0.12, pgeyBox[2] - pgeyBox[0]);
            pgeyFocus.put(pgeyG, new PgeyFocus(new PgeyVec3(0, (pgeyBox[0] + pgeyBox[2]) / 2, (pgeyBox[1] + pgeyBox[3]) / 2),
                    Math.max(0.9, pgeySize * 3.2), pgeyG == HepyGarment.SHOES ? 18 : 4));
        });
        PgeyFocus pgeyFull = new PgeyFocus(new PgeyVec3(0, pgeyHeight / 2, 0), 4.0 * pgeyHeight / 1.812, 5);
        for (HepyGarment pgeyG : HepyGarment.values()) {
            pgeyFocus.putIfAbsent(pgeyG, pgeyFull);
        }
        pgeyFocus.put(HepyGarment.KIT, pgeyFull);
        return pgeyFocus;
    }

    private static int[] pgeyCorner(String pgeyToken, int pgeyNv, int pgeyNvt, int pgeyNvn, int pgeyLineNo) {
        String[] pgeyP = pgeyToken.split("/", -1);
        int pgeyVi = pgeyIndex(pgeyP[0], pgeyNv, pgeyLineNo);
        int pgeyTi = pgeyP.length > 1 && !pgeyP[1].isEmpty() ? pgeyIndex(pgeyP[1], pgeyNvt, pgeyLineNo) : -1;
        int pgeyNi = pgeyP.length > 2 && !pgeyP[2].isEmpty() ? pgeyIndex(pgeyP[2], pgeyNvn, pgeyLineNo) : -1;
        return new int[]{pgeyVi, pgeyTi, pgeyNi};
    }

    private static int pgeyIndex(String pgeyS, int pgeySize, int pgeyLineNo) {
        int pgeyI = Integer.parseInt(pgeyS);
        int pgeyIdx = pgeyI < 0 ? pgeySize + pgeyI : pgeyI - 1;
        if (pgeyIdx < 0 || pgeyIdx >= pgeySize) {
            throw new IllegalArgumentException("Índice fuera de rango en la línea " + pgeyLineNo + ": " + pgeyS);
        }
        return pgeyIdx;
    }

    private static int[] pgeyConcat(int[] pgeyA, int[] pgeyB, int[] pgeyC) {
        return new int[]{pgeyA[0], pgeyA[1], pgeyA[2], pgeyB[0], pgeyB[1], pgeyB[2], pgeyC[0], pgeyC[1], pgeyC[2]};
    }

    private static double pgeyD(String pgeyS) {
        return Double.parseDouble(pgeyS);
    }

    private static PgeyVec3 pgeyVec(double[] pgeyP) {
        return new PgeyVec3(pgeyP[0], pgeyP[1], pgeyP[2]);
    }
}
