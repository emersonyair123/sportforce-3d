package com.sportforce3d.three.human;

import com.sportforce3d.design.PgeyGarmentCut;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelData.PgeyPart;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.three.obj.PgeyObjModelProvider;
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

public final class PgeySimulatedGarments {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeySimulatedGarments.class);

    static final Map<HepyGarment, String> PGEY_CUTS = Map.of(HepyGarment.JERSEY, "camiseta_futbol_v", HepyGarment.SHORTS, "short_futbol",
            HepyGarment.SOCKS, "medias_futbol");

    private static final double PGEY_RIM_COS = Math.cos(Math.toRadians(70));

    public record PgeyPiece(Map<PgeyZone, List<PgeyPart>> pgeyZones, PgeyClothShading pgeyShading, List<PgeyVec3> pgeyOpenings) {
    }

    private PgeySimulatedGarments() {
    }

    static String pgeyFolder(PgeyPersona pgeyPersona, HepyGarment pgeyGarment) {
        return pgeyFolder(pgeyPersona, pgeyGarment, PgeyGarmentCut.PGEY_DEFAULT);
    }

    static String pgeyFolder(PgeyPersona pgeyPersona, HepyGarment pgeyGarment, PgeyGarmentCut pgeyCut) {
        String pgeySuffix = switch (pgeyGarment) {
            case JERSEY -> pgeyCut.pgeySleeve() == PgeyGarmentCut.PgeySleeve.LARGA ? "_larga" : "";
            case SHORTS -> switch (pgeyCut.pgeyShorts()) {
                case CORTO -> "_corto";
                case MEDIO -> "";
                case LARGO -> "_largo";
            };
            case SOCKS -> switch (pgeyCut.pgeySocks()) {
                case TOBILLERA -> "_tobillera";
                case MEDIA -> "_media";
                case LARGA -> "";
            };
            default -> "";
        };
        return "/models/prendas/" + pgeyPersona.pgeyKey() + "/" + PGEY_CUTS.get(pgeyGarment) + pgeySuffix + "/";
    }

    public static boolean pgeyAvailable(PgeyPersona pgeyPersona, HepyGarment pgeyGarment) {
        return pgeyAvailable(pgeyPersona, pgeyGarment, PgeyGarmentCut.PGEY_DEFAULT);
    }

    public static boolean pgeyAvailable(PgeyPersona pgeyPersona, HepyGarment pgeyGarment, PgeyGarmentCut pgeyCut) {
        return PGEY_CUTS.containsKey(pgeyGarment)
                && PgeySimulatedGarments.class.getResource(pgeyFolder(pgeyPersona, pgeyGarment, pgeyCut) + "prenda.obj") != null;
    }

    public static Map<HepyGarment, PgeyPiece> pgeyLoad(PgeyPersona pgeyPersona) {
        return pgeyLoad(pgeyPersona, PgeyGarmentCut.PGEY_DEFAULT);
    }

    public static Map<HepyGarment, PgeyPiece> pgeyLoad(PgeyPersona pgeyPersona, PgeyGarmentCut pgeyCut) {
        Map<HepyGarment, PgeyPiece> pgeyOut = new EnumMap<>(HepyGarment.class);
        for (HepyGarment pgeyG : PGEY_CUTS.keySet()) {
            PgeyGarmentCut pgeyUse = pgeyCut;
            if (!pgeyAvailable(pgeyPersona, pgeyG, pgeyCut)) {
                if (!pgeyAvailable(pgeyPersona, pgeyG)) {
                    continue;
                }
                pgeyLog.warn("Falta la prenda simulada {}: se usa {}", pgeyFolder(pgeyPersona, pgeyG, pgeyCut), pgeyFolder(pgeyPersona, pgeyG));
                pgeyUse = PgeyGarmentCut.PGEY_DEFAULT;
            }
            try {
                pgeyOut.put(pgeyG, pgeyRead(pgeyPersona, pgeyG, pgeyUse));
            } catch (IOException | RuntimeException pgeyE) {
                pgeyLog.warn("No se pudo cargar la prenda simulada {}: {}", pgeyFolder(pgeyPersona, pgeyG, pgeyUse), pgeyE.toString());
            }
        }
        return pgeyOut;
    }

    static PgeyPiece pgeyRead(PgeyPersona pgeyPersona, HepyGarment pgeyGarment) throws IOException {
        return pgeyRead(pgeyPersona, pgeyGarment, PgeyGarmentCut.PGEY_DEFAULT);
    }

    static PgeyPiece pgeyRead(PgeyPersona pgeyPersona, HepyGarment pgeyGarment, PgeyGarmentCut pgeyCut) throws IOException {
        String pgeyDir = pgeyFolder(pgeyPersona, pgeyGarment, pgeyCut);
        try (InputStream pgeyObj = pgeyOpen(pgeyDir + "prenda.obj"); InputStream pgeyAo = pgeyOpen(pgeyDir + "ao.png");
             InputStream pgeyNormal = pgeyOpen(pgeyDir + "normal.png")) {
            return pgeyRead(new InputStreamReader(pgeyObj, StandardCharsets.UTF_8), ImageIO.read(pgeyAo), ImageIO.read(pgeyNormal),
                    pgeyPersona);
        }
    }

    static PgeyPiece pgeyRead(Reader pgeyObj, BufferedImage pgeyAo, BufferedImage pgeyNormal, PgeyPersona pgeyPersona) throws IOException {
        if (pgeyAo == null || pgeyNormal == null) {
            throw new IOException("ao.png o normal.png ilegibles");
        }
        PgeyModelData pgeyData = PgeyObjModelProvider.pgeyParse(pgeyObj, pgeyPersona.pgeyFallback());
        if (pgeyData.pgeyZones().isEmpty()) {
            throw new IOException("prenda.obj sin zonas reconocidas");
        }
        List<PgeyVec3> pgeyOpenings = new ArrayList<>();
        pgeyData.pgeyZones().values().forEach(pgeyParts -> pgeyParts.forEach(pgeyP -> pgeyOpenings.addAll(pgeyRims(pgeyP.pgeyMesh()))));
        return new PgeyPiece(pgeyData.pgeyZones(), PgeyClothShading.pgeyFromMaps(pgeyAo, pgeyNormal), pgeyOpenings);
    }

    static List<PgeyVec3> pgeyRims(PgeyMeshData pgeyMesh) {
        float[] pgeyP = pgeyMesh.pgeyPoints();
        int[] pgeyF = pgeyMesh.pgeyFaces();
        Map<List<Integer>, Integer> pgeyIds = new HashMap<>();
        int[] pgeyCanon = new int[pgeyMesh.pgeyPointCount()];
        List<Integer> pgeyFirst = new ArrayList<>();
        for (int pgeyI = 0; pgeyI < pgeyCanon.length; pgeyI++) {
            List<Integer> pgeyKey = List.of(Math.round(pgeyP[pgeyI * 3] * 1e5f), Math.round(pgeyP[pgeyI * 3 + 1] * 1e5f),
                    Math.round(pgeyP[pgeyI * 3 + 2] * 1e5f));
            Integer pgeyId = pgeyIds.get(pgeyKey);
            if (pgeyId == null) {
                pgeyId = pgeyFirst.size();
                pgeyIds.put(pgeyKey, pgeyId);
                pgeyFirst.add(pgeyI);
            }
            pgeyCanon[pgeyI] = pgeyId;
        }
        int pgeyFaces = pgeyMesh.pgeyFaceCount();
        double[][] pgeyNormals = new double[pgeyFaces][];
        Map<Long, int[]> pgeyEdges = new HashMap<>();
        for (int pgeyT = 0; pgeyT < pgeyFaces; pgeyT++) {
            int pgeyB = pgeyT * PgeyMeshData.PGEY_FACE_STRIDE;
            int pgeyA = pgeyF[pgeyB];
            int pgeyC = pgeyF[pgeyB + 3];
            int pgeyD = pgeyF[pgeyB + 6];
            double pgeyUx = pgeyP[pgeyC * 3] - pgeyP[pgeyA * 3];
            double pgeyUy = pgeyP[pgeyC * 3 + 1] - pgeyP[pgeyA * 3 + 1];
            double pgeyUz = pgeyP[pgeyC * 3 + 2] - pgeyP[pgeyA * 3 + 2];
            double pgeyWx = pgeyP[pgeyD * 3] - pgeyP[pgeyA * 3];
            double pgeyWy = pgeyP[pgeyD * 3 + 1] - pgeyP[pgeyA * 3 + 1];
            double pgeyWz = pgeyP[pgeyD * 3 + 2] - pgeyP[pgeyA * 3 + 2];
            double pgeyNx = pgeyUy * pgeyWz - pgeyUz * pgeyWy;
            double pgeyNy = pgeyUz * pgeyWx - pgeyUx * pgeyWz;
            double pgeyNz = pgeyUx * pgeyWy - pgeyUy * pgeyWx;
            double pgeyLen = Math.sqrt(pgeyNx * pgeyNx + pgeyNy * pgeyNy + pgeyNz * pgeyNz);
            pgeyNormals[pgeyT] = pgeyLen < 1e-15 ? new double[3] : new double[]{pgeyNx / pgeyLen, pgeyNy / pgeyLen, pgeyNz / pgeyLen};
            int[] pgeyV = {pgeyCanon[pgeyA], pgeyCanon[pgeyC], pgeyCanon[pgeyD]};
            for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                int pgeyV0 = Math.min(pgeyV[pgeyK], pgeyV[(pgeyK + 1) % 3]);
                int pgeyV1 = Math.max(pgeyV[pgeyK], pgeyV[(pgeyK + 1) % 3]);
                long pgeyKey = ((long) pgeyV0 << 32) | pgeyV1;
                int[] pgeyPair = pgeyEdges.computeIfAbsent(pgeyKey, pgeyX -> new int[]{-1, -1});
                pgeyPair[pgeyPair[0] < 0 ? 0 : 1] = pgeyT;
            }
        }
        boolean[] pgeyRim = new boolean[pgeyFirst.size()];
        pgeyEdges.forEach((pgeyKey, pgeyPair) -> {
            if (pgeyPair[1] >= 0) {
                double[] pgeyN0 = pgeyNormals[pgeyPair[0]];
                double[] pgeyN1 = pgeyNormals[pgeyPair[1]];
                if (pgeyN0[0] * pgeyN1[0] + pgeyN0[1] * pgeyN1[1] + pgeyN0[2] * pgeyN1[2] < PGEY_RIM_COS) {
                    pgeyRim[(int) (pgeyKey >>> 32)] = true;
                    pgeyRim[(int) (pgeyKey & 0xFFFFFFFFL)] = true;
                }
            }
        });
        List<PgeyVec3> pgeyOut = new ArrayList<>();
        for (int pgeyC = 0; pgeyC < pgeyRim.length; pgeyC++) {
            if (pgeyRim[pgeyC]) {
                int pgeyI = pgeyFirst.get(pgeyC) * 3;
                pgeyOut.add(new PgeyVec3(pgeyP[pgeyI], -pgeyP[pgeyI + 1], -pgeyP[pgeyI + 2]));
            }
        }
        return pgeyOut;
    }

    private static InputStream pgeyOpen(String pgeyResource) throws IOException {
        InputStream pgeyIn = PgeySimulatedGarments.class.getResourceAsStream(pgeyResource);
        if (pgeyIn == null) {
            throw new IOException("Falta " + pgeyResource);
        }
        return pgeyIn;
    }
}
