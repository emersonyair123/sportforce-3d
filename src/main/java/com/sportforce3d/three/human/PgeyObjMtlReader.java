package com.sportforce3d.three.human;

import com.sportforce3d.three.geom.PgeyMeshData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PgeyObjMtlReader {
    public static final String PGEY_DEFAULT_MATERIAL = "default";

    @FunctionalInterface
    public interface PgeyOpener {
        Reader pgeyOpen(String pgeyRelativePath) throws IOException;
    }

    public record PgeyMaterial(String pgeyName, double[] pgeyDiffuse, double[] pgeySpecular, double pgeyShininess, String pgeyDiffuseMap,
                           String pgeyAlphaMap) {

        static PgeyMaterial pgeyNamed(String pgeyName) {
            return new PgeyMaterial(pgeyName, new double[]{1, 1, 1}, new double[]{0, 0, 0}, 0, null, null);
        }

        PgeyMaterial pgeyWith(double[] pgeyKd, double[] pgeyKs, Double pgeyNs, String pgeyMapKd, String pgeyMapD) {
            return new PgeyMaterial(pgeyName, pgeyKd != null ? pgeyKd : pgeyDiffuse, pgeyKs != null ? pgeyKs : pgeySpecular,
                    pgeyNs != null ? pgeyNs : pgeyShininess, pgeyMapKd != null ? pgeyMapKd : pgeyDiffuseMap, pgeyMapD != null ? pgeyMapD : pgeyAlphaMap);
        }
    }

    public record PgeyGroup(String pgeyMaterial, PgeyMeshData pgeyMesh) {
    }

    public record PgeyResult(List<PgeyGroup> pgeyGroups, Map<String, PgeyMaterial> pgeyMaterials, double[] pgeyMin, double[] pgeyMax) {

        public double pgeyHeight() {
            return pgeyMax[1] - pgeyMin[1];
        }

        public PgeyMaterial pgeyMaterial(String pgeyName) {
            return pgeyMaterials.getOrDefault(pgeyName, PgeyMaterial.pgeyNamed(pgeyName));
        }
    }

    private PgeyObjMtlReader() {
    }

    private static final class PgeyFaces {
        int[] pgeyData = new int[9 * 256];
        int pgeySize;

        void pgeyAdd(int[] pgeyA, int[] pgeyB, int[] pgeyC) {
            if (pgeySize + 9 > pgeyData.length) {
                pgeyData = Arrays.copyOf(pgeyData, pgeyData.length * 2);
            }
            System.arraycopy(pgeyA, 0, pgeyData, pgeySize, 3);
            System.arraycopy(pgeyB, 0, pgeyData, pgeySize + 3, 3);
            System.arraycopy(pgeyC, 0, pgeyData, pgeySize + 6, 3);
            pgeySize += 9;
        }
    }

    private static final class PgeyDoubles {
        double[] pgeyData = new double[3 * 1024];
        int pgeySize;

        void pgeyAdd(double pgeyA, double pgeyB, double pgeyC) {
            if (pgeySize + 3 > pgeyData.length) {
                pgeyData = Arrays.copyOf(pgeyData, pgeyData.length * 2);
            }
            pgeyData[pgeySize++] = pgeyA;
            pgeyData[pgeySize++] = pgeyB;
            pgeyData[pgeySize++] = pgeyC;
        }

        int pgeyCount() {
            return pgeySize / 3;
        }
    }

    public static PgeyResult pgeyRead(Reader pgeyObj, PgeyOpener pgeyOpener) throws IOException {
        PgeyDoubles pgeyV = new PgeyDoubles();
        PgeyDoubles pgeyVt = new PgeyDoubles();
        PgeyDoubles pgeyVn = new PgeyDoubles();
        Map<String, PgeyFaces> pgeyByMaterial = new LinkedHashMap<>();
        Map<String, PgeyMaterial> pgeyMaterials = new LinkedHashMap<>();
        PgeyFaces pgeyCurrent = null;

        BufferedReader pgeyBr = new BufferedReader(pgeyObj);
        String pgeyLine;
        int pgeyLineNo = 0;
        while ((pgeyLine = pgeyBr.readLine()) != null) {
            pgeyLineNo++;
            pgeyLine = pgeyLine.strip();
            if (pgeyLine.isEmpty() || pgeyLine.charAt(0) == '#') {
                continue;
            }
            String[] pgeyT = pgeyLine.split("\\s+");
            switch (pgeyT[0]) {
                case "v" -> {
                    pgeyNeed(pgeyT, 4, pgeyLineNo);
                    pgeyV.pgeyAdd(pgeyD(pgeyT[1], pgeyLineNo), pgeyD(pgeyT[2], pgeyLineNo), pgeyD(pgeyT[3], pgeyLineNo));
                }
                case "vt" -> {
                    pgeyNeed(pgeyT, 2, pgeyLineNo);
                    pgeyVt.pgeyAdd(pgeyD(pgeyT[1], pgeyLineNo), pgeyT.length > 2 ? pgeyD(pgeyT[2], pgeyLineNo) : 0, 0);
                }
                case "vn" -> {
                    pgeyNeed(pgeyT, 4, pgeyLineNo);
                    pgeyVn.pgeyAdd(pgeyD(pgeyT[1], pgeyLineNo), pgeyD(pgeyT[2], pgeyLineNo), pgeyD(pgeyT[3], pgeyLineNo));
                }
                case "usemtl" -> pgeyCurrent = pgeyByMaterial.computeIfAbsent(pgeyRest(pgeyLine), pgeyK -> new PgeyFaces());
                case "mtllib" -> {
                    for (int pgeyI = 1; pgeyI < pgeyT.length; pgeyI++) {
                        String pgeyPath = pgeySafePath(pgeyT[pgeyI]);
                        if (pgeyPath == null) {
                            continue;
                        }
                        try (Reader pgeyMtl = pgeyOpener.pgeyOpen(pgeyPath)) {
                            if (pgeyMtl != null) {
                                pgeyMaterials.putAll(pgeyReadMtl(pgeyMtl, pgeyFolderOf(pgeyPath)));
                            }
                        }
                    }
                }
                case "f" -> {
                    pgeyNeed(pgeyT, 4, pgeyLineNo);
                    if (pgeyCurrent == null) {
                        pgeyCurrent = pgeyByMaterial.computeIfAbsent(PGEY_DEFAULT_MATERIAL, pgeyK -> new PgeyFaces());
                    }
                    int pgeyN = pgeyT.length - 1;
                    int[][] pgeyCorners = new int[pgeyN][];
                    for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
                        pgeyCorners[pgeyI] = pgeyCorner(pgeyT[pgeyI + 1], pgeyV.pgeyCount(), pgeyVt.pgeyCount(), pgeyVn.pgeyCount(), pgeyLineNo);
                    }
                    for (int pgeyI = 1; pgeyI < pgeyN - 1; pgeyI++) {
                        pgeyCurrent.pgeyAdd(pgeyCorners[0], pgeyCorners[pgeyI], pgeyCorners[pgeyI + 1]);
                    }
                }
                default -> {
                }
            }
        }

        double[] pgeyMin = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] pgeyMax = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        List<PgeyGroup> pgeyGroups = new ArrayList<>();
        for (Map.Entry<String, PgeyFaces> pgeyE : pgeyByMaterial.entrySet()) {
            if (pgeyE.getValue().pgeySize > 0) {
                pgeyGroups.add(new PgeyGroup(pgeyE.getKey(), pgeyToMesh(pgeyE.getValue(), pgeyV, pgeyVt, pgeyVn, pgeyMin, pgeyMax)));
            }
        }
        if (pgeyGroups.isEmpty()) {
            throw new IllegalArgumentException("El OBJ no tiene caras");
        }
        return new PgeyResult(Collections.unmodifiableList(pgeyGroups), Collections.unmodifiableMap(pgeyMaterials), pgeyMin, pgeyMax);
    }

    private static PgeyMeshData pgeyToMesh(PgeyFaces pgeyFaces, PgeyDoubles pgeyV, PgeyDoubles pgeyVt, PgeyDoubles pgeyVn, double[] pgeyMin, double[] pgeyMax) {
        int[] pgeyF = pgeyFaces.pgeyData;
        int pgeyCorners = pgeyFaces.pgeySize / 3;

        int[] pgeyPointOf = new int[pgeyV.pgeyCount()];
        int[] pgeyTexOf = new int[pgeyVt.pgeyCount()];
        int[] pgeyNormalOf = new int[pgeyVn.pgeyCount()];
        Arrays.fill(pgeyPointOf, -1);
        Arrays.fill(pgeyTexOf, -1);
        Arrays.fill(pgeyNormalOf, -1);
        int pgeyPoints = 0;
        int pgeyTexCoords = 0;
        int pgeyExplicitNormals = 0;
        boolean pgeyMissingTex = false;
        for (int pgeyC = 0; pgeyC < pgeyCorners; pgeyC++) {
            int pgeyVi = pgeyF[pgeyC * 3];
            int pgeyTi = pgeyF[pgeyC * 3 + 1];
            int pgeyNi = pgeyF[pgeyC * 3 + 2];
            if (pgeyPointOf[pgeyVi] < 0) {
                pgeyPointOf[pgeyVi] = pgeyPoints++;
            }
            if (pgeyTi < 0) {
                pgeyMissingTex = true;
            } else if (pgeyTexOf[pgeyTi] < 0) {
                pgeyTexOf[pgeyTi] = pgeyTexCoords++;
            }
            if (pgeyNi >= 0 && pgeyNormalOf[pgeyNi] < 0) {
                pgeyNormalOf[pgeyNi] = pgeyExplicitNormals++;
            }
        }
        int pgeyNoTex = pgeyMissingTex ? pgeyTexCoords++ : -1;

        float[] pgeyPts = new float[pgeyPoints * 3];
        for (int pgeyVi = 0; pgeyVi < pgeyPointOf.length; pgeyVi++) {
            int pgeyP = pgeyPointOf[pgeyVi];
            if (pgeyP >= 0) {
                double pgeyX = pgeyV.pgeyData[pgeyVi * 3];
                double pgeyY = pgeyV.pgeyData[pgeyVi * 3 + 1];
                double pgeyZ = pgeyV.pgeyData[pgeyVi * 3 + 2];
                pgeyPts[pgeyP * 3] = (float) pgeyX;
                pgeyPts[pgeyP * 3 + 1] = (float) -pgeyY;
                pgeyPts[pgeyP * 3 + 2] = (float) -pgeyZ;
                pgeyMin[0] = Math.min(pgeyMin[0], pgeyX);
                pgeyMin[1] = Math.min(pgeyMin[1], pgeyY);
                pgeyMin[2] = Math.min(pgeyMin[2], pgeyZ);
                pgeyMax[0] = Math.max(pgeyMax[0], pgeyX);
                pgeyMax[1] = Math.max(pgeyMax[1], pgeyY);
                pgeyMax[2] = Math.max(pgeyMax[2], pgeyZ);
            }
        }
        float[] pgeyTex = new float[pgeyTexCoords * 2];
        for (int pgeyTi = 0; pgeyTi < pgeyTexOf.length; pgeyTi++) {
            int pgeyT = pgeyTexOf[pgeyTi];
            if (pgeyT >= 0) {
                pgeyTex[pgeyT * 2] = (float) pgeyVt.pgeyData[pgeyTi * 3];
                pgeyTex[pgeyT * 2 + 1] = (float) (1 - pgeyVt.pgeyData[pgeyTi * 3 + 1]);
            }
        }

        double[] pgeySmooth = new double[pgeyPoints * 3];
        for (int pgeyC = 0; pgeyC < pgeyCorners; pgeyC += 3) {
            int pgeyA = pgeyPointOf[pgeyF[pgeyC * 3]];
            int pgeyB = pgeyPointOf[pgeyF[(pgeyC + 1) * 3]];
            int pgeyD = pgeyPointOf[pgeyF[(pgeyC + 2) * 3]];
            double pgeyUx = pgeyPts[pgeyB * 3] - pgeyPts[pgeyA * 3];
            double pgeyUy = pgeyPts[pgeyB * 3 + 1] - pgeyPts[pgeyA * 3 + 1];
            double pgeyUz = pgeyPts[pgeyB * 3 + 2] - pgeyPts[pgeyA * 3 + 2];
            double pgeyWx = pgeyPts[pgeyD * 3] - pgeyPts[pgeyA * 3];
            double pgeyWy = pgeyPts[pgeyD * 3 + 1] - pgeyPts[pgeyA * 3 + 1];
            double pgeyWz = pgeyPts[pgeyD * 3 + 2] - pgeyPts[pgeyA * 3 + 2];
            double pgeyNx = pgeyUy * pgeyWz - pgeyUz * pgeyWy;
            double pgeyNy = pgeyUz * pgeyWx - pgeyUx * pgeyWz;
            double pgeyNz = pgeyUx * pgeyWy - pgeyUy * pgeyWx;
            for (int pgeyK : new int[]{pgeyA, pgeyB, pgeyD}) {
                pgeySmooth[pgeyK * 3] += pgeyNx;
                pgeySmooth[pgeyK * 3 + 1] += pgeyNy;
                pgeySmooth[pgeyK * 3 + 2] += pgeyNz;
            }
        }
        float[] pgeyNormals = new float[(pgeyPoints + pgeyExplicitNormals) * 3];
        for (int pgeyP = 0; pgeyP < pgeyPoints; pgeyP++) {
            pgeyPutNormal(pgeyNormals, pgeyP, pgeySmooth[pgeyP * 3], pgeySmooth[pgeyP * 3 + 1], pgeySmooth[pgeyP * 3 + 2]);
        }
        for (int pgeyNi = 0; pgeyNi < pgeyNormalOf.length; pgeyNi++) {
            int pgeyN = pgeyNormalOf[pgeyNi];
            if (pgeyN >= 0) {
                pgeyPutNormal(pgeyNormals, pgeyPoints + pgeyN, pgeyVn.pgeyData[pgeyNi * 3], -pgeyVn.pgeyData[pgeyNi * 3 + 1], -pgeyVn.pgeyData[pgeyNi * 3 + 2]);
            }
        }

        int[] pgeyOut = new int[pgeyCorners * 3];
        for (int pgeyC = 0; pgeyC < pgeyCorners; pgeyC++) {
            int pgeyP = pgeyPointOf[pgeyF[pgeyC * 3]];
            int pgeyTi = pgeyF[pgeyC * 3 + 1];
            int pgeyNi = pgeyF[pgeyC * 3 + 2];
            pgeyOut[pgeyC * 3] = pgeyP;
            pgeyOut[pgeyC * 3 + 1] = pgeyNi >= 0 ? pgeyPoints + pgeyNormalOf[pgeyNi] : pgeyP;
            pgeyOut[pgeyC * 3 + 2] = pgeyTi >= 0 ? pgeyTexOf[pgeyTi] : pgeyNoTex;
        }
        return new PgeyMeshData(pgeyPts, pgeyNormals, pgeyTex, pgeyOut);
    }

    private static void pgeyPutNormal(float[] pgeyNormals, int pgeyI, double pgeyX, double pgeyY, double pgeyZ) {
        double pgeyLen = Math.sqrt(pgeyX * pgeyX + pgeyY * pgeyY + pgeyZ * pgeyZ);
        if (pgeyLen < 1e-12) {
            pgeyX = 0;
            pgeyY = -1;
            pgeyZ = 0;
            pgeyLen = 1;
        }
        pgeyNormals[pgeyI * 3] = (float) (pgeyX / pgeyLen);
        pgeyNormals[pgeyI * 3 + 1] = (float) (pgeyY / pgeyLen);
        pgeyNormals[pgeyI * 3 + 2] = (float) (pgeyZ / pgeyLen);
    }

    private static int[] pgeyCorner(String pgeyToken, int pgeyNv, int pgeyNvt, int pgeyNvn, int pgeyLineNo) {
        String[] pgeyP = pgeyToken.split("/", -1);
        int pgeyVi = pgeyIndex(pgeyP[0], pgeyNv, pgeyLineNo);
        int pgeyTi = pgeyP.length > 1 && !pgeyP[1].isEmpty() ? pgeyIndex(pgeyP[1], pgeyNvt, pgeyLineNo) : -1;
        int pgeyNi = pgeyP.length > 2 && !pgeyP[2].isEmpty() ? pgeyIndex(pgeyP[2], pgeyNvn, pgeyLineNo) : -1;
        return new int[]{pgeyVi, pgeyTi, pgeyNi};
    }

    private static int pgeyIndex(String pgeyS, int pgeySize, int pgeyLineNo) {
        int pgeyI;
        try {
            pgeyI = Integer.parseInt(pgeyS);
        } catch (NumberFormatException pgeyE) {
            throw new IllegalArgumentException("Índice no válido en la línea " + pgeyLineNo + ": " + pgeyS, pgeyE);
        }
        int pgeyIdx = pgeyI < 0 ? pgeySize + pgeyI : pgeyI - 1;
        if (pgeyI == 0 || pgeyIdx < 0 || pgeyIdx >= pgeySize) {
            throw new IllegalArgumentException("Índice fuera de rango en la línea " + pgeyLineNo + ": " + pgeyS);
        }
        return pgeyIdx;
    }

    public static Map<String, PgeyMaterial> pgeyReadMtl(Reader pgeyReader, String pgeyFolder) throws IOException {
        Map<String, PgeyMaterial> pgeyOut = new LinkedHashMap<>();
        BufferedReader pgeyBr = new BufferedReader(pgeyReader);
        String pgeyLine;
        int pgeyLineNo = 0;
        PgeyMaterial pgeyM = null;
        while ((pgeyLine = pgeyBr.readLine()) != null) {
            pgeyLineNo++;
            pgeyLine = pgeyLine.strip();
            if (pgeyLine.isEmpty() || pgeyLine.charAt(0) == '#') {
                continue;
            }
            String[] pgeyT = pgeyLine.split("\\s+");
            String pgeyKey = pgeyT[0];
            if (pgeyKey.equals("newmtl")) {
                pgeyM = PgeyMaterial.pgeyNamed(pgeyRest(pgeyLine));
                pgeyOut.put(pgeyM.pgeyName(), pgeyM);
                continue;
            }
            if (pgeyM == null) {
                continue;
            }
            PgeyMaterial pgeyNext = switch (pgeyKey) {
                case "Kd" -> pgeyM.pgeyWith(pgeyRgb(pgeyT, pgeyLineNo), null, null, null, null);
                case "Ks" -> pgeyM.pgeyWith(null, pgeyRgb(pgeyT, pgeyLineNo), null, null, null);
                case "Ns" -> {
                    pgeyNeed(pgeyT, 2, pgeyLineNo);
                    yield pgeyM.pgeyWith(null, null, pgeyD(pgeyT[1], pgeyLineNo), null, null);
                }
                case "map_Kd" -> pgeyM.pgeyWith(null, null, null, pgeyTexture(pgeyFolder, pgeyT[pgeyT.length - 1]), null);
                case "map_D", "map_d" -> pgeyM.pgeyWith(null, null, null, null, pgeyTexture(pgeyFolder, pgeyT[pgeyT.length - 1]));
                default -> pgeyM;
            };
            if (pgeyNext != pgeyM) {
                pgeyM = pgeyNext;
                pgeyOut.put(pgeyM.pgeyName(), pgeyM);
            }
        }
        return pgeyOut;
    }

    private static String pgeyTexture(String pgeyFolder, String pgeyFile) {
        String pgeyPath = pgeySafePath(pgeyFile);
        return pgeyPath == null ? null : pgeyFolder + pgeyPath;
    }

    static String pgeySafePath(String pgeyRaw) {
        if (pgeyRaw == null || pgeyRaw.isBlank()) {
            return null;
        }
        String pgeyP = pgeyRaw.strip().replace('\\', '/');
        if (pgeyP.startsWith("/") || pgeyP.matches("^[A-Za-z]:.*") || pgeyP.contains(":")) {
            return null;
        }
        for (String pgeyPart : pgeyP.split("/")) {
            if (pgeyPart.equals("..")) {
                return null;
            }
        }
        while (pgeyP.startsWith("./")) {
            pgeyP = pgeyP.substring(2);
        }
        return pgeyP.isEmpty() ? null : pgeyP;
    }

    private static String pgeyFolderOf(String pgeyPath) {
        int pgeyI = pgeyPath.lastIndexOf('/');
        return pgeyI < 0 ? "" : pgeyPath.substring(0, pgeyI + 1);
    }

    private static double[] pgeyRgb(String[] pgeyT, int pgeyLineNo) {
        pgeyNeed(pgeyT, 2, pgeyLineNo);
        double pgeyR = pgeyD(pgeyT[1], pgeyLineNo);
        double pgeyG = pgeyT.length > 2 ? pgeyD(pgeyT[2], pgeyLineNo) : pgeyR;
        double pgeyB = pgeyT.length > 3 ? pgeyD(pgeyT[3], pgeyLineNo) : pgeyR;
        return new double[]{pgeyClamp01(pgeyR), pgeyClamp01(pgeyG), pgeyClamp01(pgeyB)};
    }

    private static double pgeyClamp01(double pgeyX) {
        return Math.max(0, Math.min(1, pgeyX));
    }

    private static String pgeyRest(String pgeyLine) {
        int pgeyI = pgeyLine.indexOf(' ');
        int pgeyTab = pgeyLine.indexOf('\t');
        if (pgeyI < 0 || (pgeyTab >= 0 && pgeyTab < pgeyI)) {
            pgeyI = pgeyTab;
        }
        String pgeyR = pgeyI < 0 ? "" : pgeyLine.substring(pgeyI).strip();
        return pgeyR.isEmpty() ? PGEY_DEFAULT_MATERIAL : pgeyR;
    }

    private static void pgeyNeed(String[] pgeyT, int pgeyN, int pgeyLineNo) {
        if (pgeyT.length < pgeyN) {
            throw new IllegalArgumentException("Línea " + pgeyLineNo + " incompleta: " + String.join(" ", pgeyT));
        }
    }

    private static double pgeyD(String pgeyS, int pgeyLineNo) {
        try {
            double pgeyX = Double.parseDouble(pgeyS);
            if (!Double.isFinite(pgeyX)) {
                throw new NumberFormatException(pgeyS);
            }
            return pgeyX;
        } catch (NumberFormatException pgeyE) {
            throw new IllegalArgumentException("Número no válido en la línea " + pgeyLineNo + ": " + pgeyS, pgeyE);
        }
    }
}
