package com.sportforce3d.three.human;

import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelData.PgeyFocus;
import com.sportforce3d.three.model.PgeyModelData.PgeyPart;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.three.procedural.PgeyBodyShape;
import com.sportforce3d.three.procedural.PgeyProceduralModelProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public final class PgeyOutfitFitter {
    public record PgeyDressedPerson(PgeyHumanModel pgeyBody, PgeyModelData pgeyOutfit, Map<HepyGarment, PgeyClothShading> pgeyShading) {
    }

    private static final double PGEY_CELL = 0.02;
    private static final double PGEY_MIN_EASE = 0.002;
    private static final double PGEY_MAX_EASE = 0.035;
    private static final double PGEY_SHOULDER_MAX_EASE = 0.012;
    private static final double PGEY_SLEEVE_TUBE = 0.02;
    private static final double PGEY_SLEEVE_FLARE = 0.10;
    private static final double PGEY_DRAPE_SLOPE = 0.10;
    private static final double PGEY_COLLAR_THICKNESS = 0.55;
    private static final double PGEY_COLLAR_HEIGHT = 0.6;
    private static final double PGEY_COLLAR_LOWER = 0.003;
    private static final double PGEY_GAP = 0.004;
    private static final double PGEY_SOCK_GAP = 0.0025;
    private static final double PGEY_FOLLOWER_GAP = 0.002;
    private static final int PGEY_SMOOTH_PASSES = 4;
    private static final double PGEY_BRIDGE = Math.toRadians(9);

    private enum PgeyRegion { JERSEY, TORSO, LEG, ARM, BLEND, SHOE }

    private record PgeyCloth(double pgeyEase, int pgeyTaubin) {
    }

    private static final Map<PgeyZone, PgeyCloth> PGEY_BASES = Map.of(
            PgeyZone.JERSEY_BODY, new PgeyCloth(0.011, 60),
            PgeyZone.JERSEY_SLEEVES, new PgeyCloth(0.008, 15),
            PgeyZone.SHORTS_BASE, new PgeyCloth(0.010, 30),
            PgeyZone.SOCKS_LEG, new PgeyCloth(PGEY_SOCK_GAP, 0));

    private static final Map<PgeyZone, PgeyZone> PGEY_FOLLOWS = Map.of(
            PgeyZone.JERSEY_COLLAR, PgeyZone.JERSEY_BODY,
            PgeyZone.JERSEY_SIDES, PgeyZone.JERSEY_BODY,
            PgeyZone.JERSEY_CUFFS, PgeyZone.JERSEY_SLEEVES,
            PgeyZone.SHORTS_WAIST, PgeyZone.SHORTS_BASE,
            PgeyZone.SHORTS_STRIPE, PgeyZone.SHORTS_BASE,
            PgeyZone.SOCKS_BAND, PgeyZone.SOCKS_LEG);

    private final PgeyBodyFrame pgeyFrom;
    private final PgeyBodyFrame pgeyTo;
    private final PgeyTriangleIndex pgeySkin;
    private final double pgeyRatio;
    private final double pgeyBlendTop;
    private final double pgeyBlendBottom;
    private final double pgeyHipFreeze;
    private final double pgeyDrapeTop;
    private final double pgeyDrapeBottom;
    private final ThreadLocal<PgeyTriangleIndex.PgeyQuery> pgeySkinQ;

    private PgeyOutfitFitter(PgeyBodyFrame pgeyFrom, PgeyBodyFrame pgeyTo, PgeyTriangleIndex pgeySkin, PgeyBodyShape pgeyShape) {
        this.pgeyFrom = pgeyFrom;
        this.pgeyTo = pgeyTo;
        this.pgeySkin = pgeySkin;
        this.pgeyRatio = pgeyTo.pgeyHeight() / pgeyFrom.pgeyHeight();
        double pgeyScale = pgeyShape.pgeyHeight() / 1.812;
        pgeyBlendTop = (PgeyProceduralModelProvider.PGEY_SHORTS_CROTCH + 0.03) * pgeyScale;
        pgeyBlendBottom = (PgeyProceduralModelProvider.PGEY_SHORTS_CROTCH - 0.07) * pgeyScale;
        pgeyHipFreeze = PgeyProceduralModelProvider.PGEY_SHORTS_HIP_FREEZE * pgeyScale;
        pgeyDrapeTop = PgeyProceduralModelProvider.PGEY_JERSEY_DRAPE_TOP * pgeyScale;
        pgeyDrapeBottom = PgeyProceduralModelProvider.PGEY_JERSEY_DRAPE_BOTTOM * pgeyScale;
        pgeySkinQ = ThreadLocal.withInitial(this.pgeySkin::pgeyQuery);
    }

    public static PgeyDressedPerson pgeyFit(PgeyHumanModel pgeyHuman, PgeyModelData pgeySource) {
        return pgeyFit(pgeyHuman, pgeySource, Map.of());
    }

    public static PgeyDressedPerson pgeyFit(PgeyHumanModel pgeyHuman, PgeyModelData pgeySource,
                                    Map<HepyGarment, PgeySimulatedGarments.PgeyPiece> pgeySimulated) {
        PgeyBodyShape pgeyShape = PgeyBodyShape.pgeyOf(pgeySource.pgeyBodyType());
        PgeyHumanModel.PgeyPart pgeySkinPart = pgeySkinOf(pgeyHuman);
        PgeyTriangleIndex pgeySkin = PgeyTriangleIndex.pgeyOf(List.of(pgeySkinPart.pgeyMesh()), PGEY_CELL);
        PgeyTriangleIndex pgeyMannequin = PgeyTriangleIndex.pgeyOf(pgeySource.pgeyMannequin().stream().map(PgeyPart::pgeyMesh).toList(), PGEY_CELL);
        PgeyBodyFrame pgeyFrom = PgeyBodyFrame.pgeyOf(pgeyShape, pgeyMannequin.pgeyVertices());
        PgeyBodyFrame pgeyTo = PgeyBodyFrame.pgeyMeasure(pgeySkin.pgeyVertices(), pgeySkin.pgeyTriangles(), pgeyHuman.pgeyMinY(), pgeyHuman.pgeyHeight());
        return new PgeyOutfitFitter(pgeyFrom, pgeyTo, pgeySkin, pgeyShape).pgeyDress(pgeyHuman, pgeySkinPart, pgeySource, pgeySimulated);
    }

    static PgeyHumanModel.PgeyPart pgeySkinOf(PgeyHumanModel pgeyHuman) {
        PgeyHumanModel.PgeyPart pgeyBest = null;
        for (PgeyHumanModel.PgeyPart pgeyP : pgeyHuman.pgeyParts()) {
            if (!pgeyP.pgeyTransparent() && (pgeyBest == null || pgeyP.pgeyMesh().pgeyFaceCount() > pgeyBest.pgeyMesh().pgeyFaceCount())) {
                pgeyBest = pgeyP;
            }
        }
        return pgeyBest != null ? pgeyBest : pgeyHuman.pgeyParts().get(0);
    }

    private record PgeyFitted(PgeyMeshData pgeyMesh, int[] pgeyCanon, double[] pgeySrc, double[] pgeyDst, boolean[] pgeyInside) {
    }

    private PgeyDressedPerson pgeyDress(PgeyHumanModel pgeyHuman, PgeyHumanModel.PgeyPart pgeySkinPart, PgeyModelData pgeySource,
                                Map<HepyGarment, PgeySimulatedGarments.PgeyPiece> pgeySimulated) {
        PgeyShoes pgeyShoes = new PgeyShoes(pgeySource);
        Map<PgeyZone, List<PgeyFitted>> pgeyBase = new EnumMap<>(PgeyZone.class);
        PGEY_BASES.forEach((pgeyZone, pgeyCloth) -> {
            if (!pgeySimulated.containsKey(pgeyZone.pgeyGarment())) {
                pgeyBase.put(pgeyZone, pgeySource.pgeyZones().get(pgeyZone).stream().map(pgeyP -> pgeyFitBase(pgeyZone, pgeyCloth, pgeyP.pgeyMesh())).toList());
            }
        });
        if (!pgeySimulated.containsKey(HepyGarment.JERSEY)) {
            pgeyHideSleeveTops(pgeyBase.get(PgeyZone.JERSEY_SLEEVES), pgeyBase.get(PgeyZone.JERSEY_BODY));
        }

        Map<PgeyZone, List<PgeyPart>> pgeyZones = new EnumMap<>(PgeyZone.class);
        pgeySimulated.forEach((pgeyG, pgeyPiece) -> pgeyPiece.pgeyZones().forEach((pgeyZone, pgeyParts) -> pgeyZones.put(pgeyZone, pgeyG != HepyGarment.SOCKS
                ? pgeyParts : pgeyParts.stream().map(pgeyP -> new PgeyPart(pgeyP.pgeyName(), pgeyRemoveBelow(pgeyP.pgeyMesh(), pgeyShoes, 0.55))).toList())));
        double pgeyH = pgeyHuman.pgeyHeight();
        pgeySource.pgeyZones().forEach((pgeyZone, pgeyParts) -> {
            if (pgeySimulated.containsKey(pgeyZone.pgeyGarment())) {
                return;
            }
            List<PgeyPart> pgeyOut = new ArrayList<>();
            for (int pgeyI = 0; pgeyI < pgeyParts.size(); pgeyI++) {
                PgeyPart pgeyPart = pgeyParts.get(pgeyI);
                PgeyMeshData pgeyMesh;
                if (pgeyZone.pgeyGarment() == HepyGarment.SHOES) {
                    pgeyMesh = pgeyShoes.pgeyPlace(pgeyPart.pgeyMesh());
                } else if (pgeyBase.containsKey(pgeyZone)) {
                    pgeyMesh = pgeyToMesh(pgeyBase.get(pgeyZone).get(pgeyI));
                } else if (pgeyZone == PgeyZone.JERSEY_COLLAR) {
                    pgeyMesh = pgeyFollowNeckline(pgeyPart.pgeyMesh(), pgeyBase.get(PgeyZone.JERSEY_BODY));
                } else {
                    pgeyMesh = pgeyFollow(pgeyPart.pgeyMesh(), pgeyBase.get(PGEY_FOLLOWS.get(pgeyZone)));
                }
                if (pgeyZone.pgeyGarment() == HepyGarment.SOCKS) {
                    pgeyMesh = pgeyRemoveBelow(pgeyMesh, pgeyShoes, 0.55);
                }
                if (pgeyZone == PgeyZone.JERSEY_BODY) {
                    pgeyMesh = PgeyClothOps.pgeyHemFold(pgeyMesh, pgeyLowest(pgeyMesh, 0.06 * pgeyH), 0.005, 0.006);
                } else if (pgeyZone == PgeyZone.SHORTS_BASE) {
                    pgeyMesh = PgeyClothOps.pgeyHemFold(pgeyMesh, pgeyLowest(pgeyMesh, 0.03 * pgeyH), 0.005, 0.006);
                }
                pgeyOut.add(new PgeyPart(pgeyPart.pgeyName(), pgeyMesh));
            }
            pgeyZones.put(pgeyZone, pgeyOut);
        });

        List<PgeyVec3> pgeyOpenings = new ArrayList<>();
        pgeySimulated.values().forEach(pgeyPiece -> pgeyOpenings.addAll(pgeyPiece.pgeyOpenings()));
        List<PgeyHumanModel.PgeyPart> pgeyParts = new ArrayList<>();
        for (PgeyHumanModel.PgeyPart pgeyP : pgeyHuman.pgeyParts()) {
            pgeyParts.add(pgeyP == pgeySkinPart ? new PgeyHumanModel.PgeyPart(pgeyP.pgeyName(),
                    pgeyHideCovered(pgeyRemoveBelow(pgeyP.pgeyMesh(), pgeyShoes, 0.75), pgeyZones, pgeyOpenings, pgeySimulated.keySet()),
                    pgeyP.pgeyDiffuse(), pgeyP.pgeySpecular(), pgeyP.pgeySpecularPower(), pgeyP.pgeyTexture(), pgeyP.pgeyTransparent()) : pgeyP);
        }
        PgeyHumanModel pgeyBody = new PgeyHumanModel(pgeyHuman.pgeyPersona(), pgeyParts, pgeyHuman.pgeyMinY(), pgeyHuman.pgeyHeight());
        PgeyModelData pgeyOutfit = new PgeyModelData(pgeySource.pgeyBodyType(), List.of(), pgeyZones, pgeyFocus(pgeyZones, pgeyHuman), pgeyHuman.pgeyHeight());
        Map<HepyGarment, PgeyClothShading> pgeyShading = pgeyShading(pgeyZones, PgeyOutfitFitter.pgeySkinOf(pgeyBody).pgeyMesh(), pgeySimulated.keySet());
        pgeySimulated.forEach((pgeyG, pgeyPiece) -> pgeyShading.put(pgeyG, pgeyPiece.pgeyShading()));
        return new PgeyDressedPerson(pgeyBody, pgeyOutfit, pgeyShading);
    }

    private static final Map<HepyGarment, Integer> PGEY_SHADING_SIZE = Map.of(HepyGarment.JERSEY, 1024, HepyGarment.SHORTS, 1024,
            HepyGarment.SOCKS, 512);
    private static final int PGEY_AO_RAYS = 14;
    private static final double PGEY_AO_DISTANCE = 0.12;

    private Map<HepyGarment, PgeyClothShading> pgeyShading(Map<PgeyZone, List<PgeyPart>> pgeyZones, PgeyMeshData pgeyVisibleSkin,
                                               java.util.Set<HepyGarment> pgeyBaked) {
        List<PgeyMeshData> pgeyAll = new ArrayList<>();
        pgeyAll.add(pgeyVisibleSkin);
        pgeyZones.values().forEach(pgeyParts -> pgeyParts.forEach(pgeyPt -> pgeyAll.add(pgeyPt.pgeyMesh())));
        PgeyTriangleIndex pgeyOccluders = PgeyTriangleIndex.pgeyOf(pgeyAll, PGEY_CELL);
        ThreadLocal<PgeyTriangleIndex.PgeyQuery> pgeyQ = ThreadLocal.withInitial(pgeyOccluders::pgeyQuery);
        PgeyVec3[] pgeyHemisphere = pgeyHemisphere(PGEY_AO_RAYS);
        Map<HepyGarment, PgeyClothShading> pgeyOut = new EnumMap<>(HepyGarment.class);
        PGEY_SHADING_SIZE.forEach((pgeyGarment, pgeySize) -> {
            if (pgeyBaked.contains(pgeyGarment)) {
                return;
            }
            List<PgeyMeshData> pgeyMeshes = new ArrayList<>();
            List<PgeyClothShading.PgeyPointData> pgeyData = new ArrayList<>();
            pgeyZones.forEach((pgeyZone, pgeyParts) -> {
                if (pgeyZone.pgeyGarment() != pgeyGarment) {
                    return;
                }
                for (PgeyPart pgeyPt : pgeyParts) {
                    pgeyMeshes.add(pgeyPt.pgeyMesh());
                    pgeyData.add(pgeyPointData(pgeyZone, pgeyPt.pgeyMesh(), pgeyQ, pgeyHemisphere));
                }
            });
            if (!pgeyMeshes.isEmpty()) {
                pgeyOut.put(pgeyGarment, PgeyClothShading.pgeyBake(pgeyMeshes, pgeyData, pgeySize));
            }
        });
        return pgeyOut;
    }

    private static PgeyVec3[] pgeyHemisphere(int pgeyN) {
        PgeyVec3[] pgeyD = new PgeyVec3[pgeyN];
        double pgeyGolden = Math.PI * (3 - Math.sqrt(5));
        for (int pgeyI = 0; pgeyI < pgeyN; pgeyI++) {
            double pgeyZ = 1 - (pgeyI + 0.5) / pgeyN * 0.85;
            double pgeyR = Math.sqrt(1 - pgeyZ * pgeyZ);
            double pgeyA = pgeyGolden * pgeyI;
            pgeyD[pgeyI] = new PgeyVec3(Math.cos(pgeyA) * pgeyR, Math.sin(pgeyA) * pgeyR, pgeyZ);
        }
        return pgeyD;
    }

    private PgeyClothShading.PgeyPointData pgeyPointData(PgeyZone pgeyZone, PgeyMeshData pgeyMesh, ThreadLocal<PgeyTriangleIndex.PgeyQuery> pgeyQ,
                                             PgeyVec3[] pgeyHemisphere) {
        int pgeyN = pgeyMesh.pgeyPointCount();
        float[] pgeyOpen = new float[pgeyN];
        float[] pgeyWrinkle = new float[pgeyN];
        float[] pgeyAngle = new float[pgeyN];
        float[] pgeyPts = pgeyMesh.pgeyPoints();
        float[] pgeyNrm = pgeyMesh.pgeyNormals();
        int[] pgeyNormalOf = new int[pgeyN];
        int[] pgeyF = pgeyMesh.pgeyFaces();
        for (int pgeyK = 0; pgeyK < pgeyF.length; pgeyK += 3) {
            pgeyNormalOf[pgeyF[pgeyK]] = pgeyF[pgeyK + 1];
        }
        double pgeyH = pgeyTo.pgeyHeight();
        IntStream.range(0, pgeyN).parallel().forEach(pgeyI -> {
            PgeyVec3 pgeyP = new PgeyVec3(pgeyPts[pgeyI * 3], -pgeyPts[pgeyI * 3 + 1], -pgeyPts[pgeyI * 3 + 2]);
            int pgeyNi = pgeyNormalOf[pgeyI] * 3;
            PgeyVec3 pgeyNv = new PgeyVec3(pgeyNrm[pgeyNi], -pgeyNrm[pgeyNi + 1], -pgeyNrm[pgeyNi + 2]).pgeyNormalize();
            PgeyVec3 pgeyT = Math.abs(pgeyNv.pgeyY()) < 0.9 ? PgeyVec3.PGEY_UP.pgeyCross(pgeyNv).pgeyNormalize() : new PgeyVec3(1, 0, 0).pgeyCross(pgeyNv).pgeyNormalize();
            PgeyVec3 pgeyB = pgeyNv.pgeyCross(pgeyT);
            PgeyVec3 pgeyO = pgeyP.pgeyAddScaled(pgeyNv, 0.003);
            double pgeyOcc = 0;
            for (PgeyVec3 pgeyD : pgeyHemisphere) {
                PgeyVec3 pgeyDir = pgeyT.pgeyMul(pgeyD.pgeyX()).pgeyAdd(pgeyB.pgeyMul(pgeyD.pgeyY())).pgeyAdd(pgeyNv.pgeyMul(pgeyD.pgeyZ()));
                double pgeyHit = pgeyQ.get().pgeyRay(pgeyO, pgeyDir, PGEY_AO_DISTANCE);
                if (!Double.isNaN(pgeyHit)) {
                    pgeyOcc += 1 - pgeyHit / PGEY_AO_DISTANCE;
                }
            }
            pgeyOpen[pgeyI] = (float) Math.max(0, 1 - 1.4 * pgeyOcc / pgeyHemisphere.length);

            double pgeyY = pgeyP.pgeyY();
            int pgeySign = pgeyP.pgeyX() >= 0 ? 1 : -1;
            double pgeySide = Math.abs(pgeyNv.pgeyX());
            double pgeyInner = Math.max(0, -pgeyNv.pgeyX() * pgeySign);
            double pgeyW = 0;
            double pgeyA = Math.PI / 2;
            switch (pgeyZone.pgeyGarment()) {
                case JERSEY -> {
                    if (pgeyZone == PgeyZone.JERSEY_SLEEVES || pgeyZone == PgeyZone.JERSEY_CUFFS) {
                        pgeyW = 0.65 * pgeyInner * (0.4 + 0.6 * pgeySmoothstep(pgeyTo.pgeyArmpit - 0.02 * pgeyH, pgeyTo.pgeyArmpit - 0.10 * pgeyH, pgeyY));
                    } else if (pgeyZone != PgeyZone.JERSEY_COLLAR) {
                        double pgeyArmpit = pgeyBell(pgeyY, pgeyTo.pgeyArmpit - 0.04 * pgeyH, 0.045 * pgeyH) * pgeySmoothstep(0.35, 0.85, pgeySide);
                        double pgeyWaist = 0.5 * pgeyBell(pgeyY, pgeyTo.pgeyFloor + 0.585 * pgeyH, 0.04 * pgeyH) * (0.3 + 0.7 * pgeySide);
                        if (pgeyArmpit > pgeyWaist) {
                            pgeyA = (pgeyP.pgeyX() > 0) == (pgeyP.pgeyZ() > 0) ? Math.toRadians(55) : Math.toRadians(125);
                        }
                        pgeyW = Math.max(pgeyW, Math.max(pgeyArmpit, pgeyWaist));
                    } else {
                        pgeyW = 0;
                    }
                }
                case SHORTS -> {
                    double pgeyCrotch = pgeyBell(pgeyY, pgeyTo.pgeyCrotch - 0.03 * pgeyH, 0.045 * pgeyH) * (0.3 + 0.7 * pgeyInner);
                    double pgeyHip = 0.5 * pgeyBell(pgeyY, pgeyTo.pgeyCrotch + 0.04 * pgeyH, 0.04 * pgeyH) * pgeySmoothstep(0.2, 0.7, pgeyNv.pgeyZ());
                    if (pgeyCrotch > 0.2) {
                        pgeyA = pgeySign > 0 ? Math.toRadians(60) : Math.toRadians(120);
                    }
                    pgeyW = Math.max(pgeyCrotch, pgeyHip);
                }
                case SOCKS -> {
                    double pgeyKnee = pgeyBell(pgeyY, pgeyTo.pgeyFloor + 0.285 * pgeyH, 0.035 * pgeyH) * pgeySmoothstep(0.1, 0.7, -pgeyNv.pgeyZ());
                    double pgeyAnkle = 0.45 * pgeyBell(pgeyY, pgeyTo.pgeyFloor + 0.075 * pgeyH, 0.03 * pgeyH);
                    pgeyW = Math.max(pgeyKnee, pgeyAnkle);
                }
                default -> pgeyW = 0;
            }
            pgeyWrinkle[pgeyI] = (float) pgeyW;
            pgeyAngle[pgeyI] = (float) pgeyA;
        });
        return new PgeyClothShading.PgeyPointData(pgeyOpen, pgeyWrinkle, pgeyAngle);
    }

    private static double pgeyBell(double pgeyX, double pgeyCenter, double pgeyWidth) {
        double pgeyD = (pgeyX - pgeyCenter) / pgeyWidth;
        return Math.exp(-pgeyD * pgeyD);
    }

    private static java.util.function.IntPredicate pgeyLowest(PgeyMeshData pgeyMesh, double pgeyBand) {
        float[] pgeyP = pgeyMesh.pgeyPoints();
        double pgeyMin = Double.MAX_VALUE;
        for (int pgeyI = 1; pgeyI < pgeyP.length; pgeyI += 3) {
            pgeyMin = Math.min(pgeyMin, -pgeyP[pgeyI]);
        }
        double pgeyLimit = pgeyMin + pgeyBand;
        return pgeyI -> -pgeyP[pgeyI * 3 + 1] < pgeyLimit;
    }

    private static PgeyRegion pgeyRegion(PgeyZone pgeyZone) {
        return switch (pgeyZone) {
            case JERSEY_BODY -> PgeyRegion.JERSEY;
            case JERSEY_SLEEVES -> PgeyRegion.ARM;
            case SHORTS_BASE -> PgeyRegion.BLEND;
            case SOCKS_LEG -> PgeyRegion.LEG;
            default -> PgeyRegion.TORSO;
        };
    }

    private PgeyFitted pgeyFitBase(PgeyZone pgeyZone, PgeyCloth pgeyCloth, PgeyMeshData pgeyMesh) {
        PgeyRegion pgeyRegion = pgeyRegion(pgeyZone);
        float[] pgeyPts = pgeyMesh.pgeyPoints();
        int[] pgeyCanon = new int[pgeyMesh.pgeyPointCount()];
        int[] pgeyFirstOf = pgeyWeld(pgeyPts, pgeyCanon);
        int pgeyM = pgeyFirstOf.length;
        double[] pgeySrc = new double[pgeyM * 3];
        double[] pgeyDst = new double[pgeyM * 3];
        boolean[] pgeyInside = new boolean[pgeyM];
        IntStream.range(0, pgeyM).parallel().forEach(pgeyC -> {
            int pgeyI = pgeyFirstOf[pgeyC] * 3;
            PgeyVec3 pgeyP = new PgeyVec3(pgeyPts[pgeyI], -pgeyPts[pgeyI + 1], -pgeyPts[pgeyI + 2]);
            PgeyPlaced pgeyPlaced = pgeyTransfer(pgeyP, pgeyRegion);
            PgeyVec3 pgeyQ = pgeyPlaced.pgeyP();
            pgeyInside[pgeyC] = pgeyPlaced.pgeyInside();
            pgeySrc[pgeyC * 3] = pgeyP.pgeyX();
            pgeySrc[pgeyC * 3 + 1] = pgeyP.pgeyY();
            pgeySrc[pgeyC * 3 + 2] = pgeyP.pgeyZ();
            pgeyDst[pgeyC * 3] = pgeyQ.pgeyX();
            pgeyDst[pgeyC * 3 + 1] = pgeyQ.pgeyY();
            pgeyDst[pgeyC * 3 + 2] = pgeyQ.pgeyZ();
        });
        int pgeyPasses = pgeyRegion == PgeyRegion.BLEND ? PGEY_SMOOTH_PASSES * 3 : PGEY_SMOOTH_PASSES;
        pgeySmoothDisplacement(pgeyMesh, pgeyCanon, pgeySrc, pgeyDst, pgeyInside, pgeyM, pgeyPasses);
        IntStream.range(0, pgeyM).parallel().filter(pgeyC -> !pgeyInside[pgeyC]).forEach(pgeyC -> pgeyPushOut(pgeyDst, pgeyC, pgeyCloth.pgeyEase()));
        if (pgeyCloth.pgeyTaubin() > 0) {
            int[][] pgeyNb = PgeyClothOps.pgeyNeighbors(pgeyMesh, pgeyCanon, pgeyM);
            PgeyClothOps.pgeyTaubin(pgeyDst, pgeyNb, PgeyClothOps.pgeyBoundary(pgeyMesh, pgeyCanon, pgeyM), pgeyInside, pgeyCloth.pgeyTaubin());
        } else {
            double pgeyGap = pgeyZone.pgeyGarment() == HepyGarment.SOCKS ? PGEY_SOCK_GAP : PGEY_GAP;
            IntStream.range(0, pgeyM).parallel().filter(pgeyC -> !pgeyInside[pgeyC]).forEach(pgeyC -> pgeyPushOut(pgeyDst, pgeyC, pgeyGap));
        }
        return new PgeyFitted(pgeyMesh, pgeyCanon, pgeySrc, pgeyDst, pgeyInside);
    }

    private static PgeyMeshData pgeyToMesh(PgeyFitted pgeyF) {
        float[] pgeyPts = pgeyF.pgeyMesh().pgeyPoints();
        float[] pgeyOut = new float[pgeyPts.length];
        for (int pgeyI = 0; pgeyI < pgeyF.pgeyCanon().length; pgeyI++) {
            int pgeyC = pgeyF.pgeyCanon()[pgeyI];
            pgeyOut[pgeyI * 3] = (float) pgeyF.pgeyDst()[pgeyC * 3];
            pgeyOut[pgeyI * 3 + 1] = (float) -pgeyF.pgeyDst()[pgeyC * 3 + 1];
            pgeyOut[pgeyI * 3 + 2] = (float) -pgeyF.pgeyDst()[pgeyC * 3 + 2];
        }
        return pgeyWithNormals(pgeyF.pgeyMesh(), pgeyOut);
    }

    private PgeyMeshData pgeyFollow(PgeyMeshData pgeyMesh, List<PgeyFitted> pgeyBase) {
        int pgeyTotal = pgeyBase.stream().mapToInt(pgeyB -> pgeyB.pgeySrc().length).sum();
        double[] pgeySrc = new double[pgeyTotal];
        double[] pgeyDst = new double[pgeyTotal];
        int pgeyAt = 0;
        for (PgeyFitted pgeyB : pgeyBase) {
            System.arraycopy(pgeyB.pgeySrc(), 0, pgeySrc, pgeyAt, pgeyB.pgeySrc().length);
            System.arraycopy(pgeyB.pgeyDst(), 0, pgeyDst, pgeyAt, pgeyB.pgeyDst().length);
            pgeyAt += pgeyB.pgeySrc().length;
        }
        float[] pgeyPts = pgeyMesh.pgeyPoints();
        double[] pgeyP = new double[pgeyPts.length];
        for (int pgeyI = 0; pgeyI < pgeyPts.length; pgeyI += 3) {
            pgeyP[pgeyI] = pgeyPts[pgeyI];
            pgeyP[pgeyI + 1] = -pgeyPts[pgeyI + 1];
            pgeyP[pgeyI + 2] = -pgeyPts[pgeyI + 2];
        }
        double[] pgeyMoved = PgeyClothOps.pgeyFollow(pgeySrc, pgeyDst, pgeyP);
        IntStream.range(0, pgeyMoved.length / 3).parallel().forEach(pgeyC -> pgeyPushOut(pgeyMoved, pgeyC, PGEY_FOLLOWER_GAP));
        float[] pgeyOut = new float[pgeyPts.length];
        for (int pgeyI = 0; pgeyI < pgeyPts.length; pgeyI += 3) {
            pgeyOut[pgeyI] = (float) pgeyMoved[pgeyI];
            pgeyOut[pgeyI + 1] = (float) -pgeyMoved[pgeyI + 1];
            pgeyOut[pgeyI + 2] = (float) -pgeyMoved[pgeyI + 2];
        }
        return pgeyWithNormals(pgeyMesh, pgeyOut);
    }

    private PgeyMeshData pgeyFollowNeckline(PgeyMeshData pgeyCollar, List<PgeyFitted> pgeyBody) {
        PgeyFitted pgeyB = pgeyBody.get(0);
        PgeyMeshData pgeyFittedBody = pgeyToMesh(pgeyB);
        double pgeyTop = Double.NEGATIVE_INFINITY;
        double pgeyBottom = Double.POSITIVE_INFINITY;
        for (int pgeyI = 1; pgeyI < pgeyB.pgeySrc().length; pgeyI += 3) {
            pgeyTop = Math.max(pgeyTop, pgeyB.pgeySrc()[pgeyI]);
            pgeyBottom = Math.min(pgeyBottom, pgeyB.pgeySrc()[pgeyI]);
        }
        double pgeyMiddle = (pgeyTop + pgeyBottom) / 2;
        float[] pgeyOut = PgeyClothOps.pgeyFollowEdge(pgeyB.pgeyMesh(), pgeyFittedBody, pgeyI -> -pgeyB.pgeyMesh().pgeyPoints()[pgeyI * 3 + 1] > pgeyMiddle,
                pgeyCollar.pgeyPoints(), PGEY_COLLAR_THICKNESS, PGEY_COLLAR_HEIGHT, PGEY_COLLAR_LOWER);
        double[] pgeyModel = new double[pgeyOut.length];
        for (int pgeyI = 0; pgeyI < pgeyOut.length; pgeyI += 3) {
            pgeyModel[pgeyI] = pgeyOut[pgeyI];
            pgeyModel[pgeyI + 1] = -pgeyOut[pgeyI + 1];
            pgeyModel[pgeyI + 2] = -pgeyOut[pgeyI + 2];
        }
        IntStream.range(0, pgeyModel.length / 3).parallel().forEach(pgeyC -> pgeyPushOut(pgeyModel, pgeyC, PGEY_FOLLOWER_GAP));
        for (int pgeyI = 0; pgeyI < pgeyOut.length; pgeyI += 3) {
            pgeyOut[pgeyI] = (float) pgeyModel[pgeyI];
            pgeyOut[pgeyI + 1] = (float) -pgeyModel[pgeyI + 1];
            pgeyOut[pgeyI + 2] = (float) -pgeyModel[pgeyI + 2];
        }
        return pgeyWithNormals(pgeyCollar, pgeyOut);
    }

    private void pgeyHideSleeveTops(List<PgeyFitted> pgeySleeves, List<PgeyFitted> pgeyBody) {
        PgeyTriangleIndex pgeyIndex = PgeyTriangleIndex.pgeyOf(pgeyBody.stream().map(PgeyOutfitFitter::pgeyToMesh).toList(), PGEY_CELL);
        ThreadLocal<PgeyTriangleIndex.PgeyQuery> pgeyQ = ThreadLocal.withInitial(pgeyIndex::pgeyQuery);
        for (PgeyFitted pgeyS : pgeySleeves) {
            double[] pgeySrc = pgeyS.pgeySrc();
            double[] pgeyD = pgeyS.pgeyDst();
            IntStream.range(0, pgeyD.length / 3).parallel().forEach(pgeyC -> {
                PgeyVec3 pgeyO = new PgeyVec3(pgeySrc[pgeyC * 3], pgeySrc[pgeyC * 3 + 1], pgeySrc[pgeyC * 3 + 2]);
                if (pgeyO.pgeyY() <= pgeyFrom.pgeyArmpit || !pgeyFrom.pgeyInsideTorso(pgeyO)) {
                    return;
                }
                PgeyVec3 pgeyP = new PgeyVec3(pgeyD[pgeyC * 3], pgeyD[pgeyC * 3 + 1], pgeyD[pgeyC * 3 + 2]);
                PgeyTriangleIndex.PgeyHit pgeyHit = pgeyQ.get().pgeyNearest(pgeyP, 0.04);
                if (pgeyHit == null) {
                    return;
                }
                double pgeySd = pgeyP.pgeySub(pgeyHit.pgeyPoint()).pgeyDot(pgeyHit.pgeyNormal());
                if (pgeySd > -0.004) {
                    PgeyVec3 pgeyIn = pgeyP.pgeyAddScaled(pgeyHit.pgeyNormal(), -(pgeySd + 0.004));
                    pgeyD[pgeyC * 3] = pgeyIn.pgeyX();
                    pgeyD[pgeyC * 3 + 1] = pgeyIn.pgeyY();
                    pgeyD[pgeyC * 3 + 2] = pgeyIn.pgeyZ();
                }
            });
        }
    }

    private record PgeyPlaced(PgeyVec3 pgeyP, boolean pgeyInside) {
    }

    private PgeyPlaced pgeyTransfer(PgeyVec3 pgeyP, PgeyRegion pgeyRegion) {
        return switch (pgeyRegion) {
            case JERSEY -> pgeyTorso(pgeyP, pgeyP.pgeyY(), true);
            case TORSO -> pgeyTorso(pgeyP, pgeyP.pgeyY(), false);
            case LEG -> pgeyLeg(pgeyP);
            case ARM -> pgeyArm(pgeyP);
            case BLEND -> {
                double pgeyW = pgeySmoothstep(pgeyBlendTop, pgeyBlendBottom, pgeyP.pgeyY());
                double pgeySurfaceY = Math.max(pgeyP.pgeyY(), pgeyHipFreeze);
                if (pgeyW <= 0) {
                    yield pgeyTorso(pgeyP, pgeySurfaceY, false);
                }
                if (pgeyW >= 1) {
                    yield pgeyLeg(pgeyP);
                }
                PgeyPlaced pgeyA = pgeyTorso(pgeyP, pgeySurfaceY, false);
                PgeyPlaced pgeyB = pgeyLeg(pgeyP);
                PgeyVec3 pgeyMid = pgeyA.pgeyP().pgeyLerp(pgeyB.pgeyP(), pgeyW);
                PgeyVec3 pgeyC = pgeyTo.pgeyLegCenter(pgeyP.pgeyX() >= 0 ? 1 : -1, pgeyMid.pgeyY());
                double pgeyRa = Math.hypot(pgeyA.pgeyP().pgeyX() - pgeyC.pgeyX(), pgeyA.pgeyP().pgeyZ() - pgeyC.pgeyZ());
                double pgeyRb = Math.hypot(pgeyB.pgeyP().pgeyX() - pgeyC.pgeyX(), pgeyB.pgeyP().pgeyZ() - pgeyC.pgeyZ());
                PgeyVec3 pgeyH = new PgeyVec3(pgeyMid.pgeyX() - pgeyC.pgeyX(), 0, pgeyMid.pgeyZ() - pgeyC.pgeyZ());
                double pgeyLen = pgeyH.pgeyLength();
                PgeyVec3 pgeyQ = pgeyLen < 1e-9 ? pgeyMid
                        : new PgeyVec3(pgeyC.pgeyX() + pgeyH.pgeyX() / pgeyLen * (pgeyRa + (pgeyRb - pgeyRa) * pgeyW), pgeyMid.pgeyY(), pgeyC.pgeyZ() + pgeyH.pgeyZ() / pgeyLen * (pgeyRa + (pgeyRb - pgeyRa) * pgeyW));
                yield new PgeyPlaced(pgeyQ, pgeyW < 0.5 ? pgeyA.pgeyInside() : pgeyB.pgeyInside());
            }
            case SHOE -> new PgeyPlaced(pgeyP, false);
        };
    }

    private PgeyPlaced pgeyTorso(PgeyVec3 pgeyP, double pgeySurfaceY, boolean pgeyDrape) {
        PgeyVec3 pgeyAxis = pgeyFrom.pgeyTorsoCenter(pgeyP.pgeyY());
        double pgeyW = pgeySmoothstep(pgeyFrom.pgeyArmpit - 0.03 * pgeyFrom.pgeyHeight(), pgeyFrom.pgeyArmpit + 0.03 * pgeyFrom.pgeyHeight(), pgeyP.pgeyY());
        PgeyPlaced pgeyFlat = null;
        if (pgeyW < 1) {
            double pgeyY = pgeyMapY(pgeyP.pgeyY());
            PgeyVec3 pgeyAxisTo = pgeyTo.pgeyTorsoCenter(Math.max(pgeyY, pgeyTo.pgeyCrotch + 0.01));
            PgeyPlaced pgeyF = pgeyDrape
                    ? pgeyVertical(pgeyP, pgeyAxis, pgeyAxisTo, pgeyDir -> pgeyDrapedFrom(pgeyP.pgeyY(), pgeyDir), pgeyDir -> pgeyDrapedTo(pgeyP.pgeyY(), pgeyAxisTo, pgeyDir))
                    : pgeyVertical(pgeyP, pgeyAxis, pgeyAxisTo, pgeyDir -> pgeyFrom.pgeyTorsoRadius(pgeySurfaceY, pgeyDir), pgeyDir -> pgeyBridged(pgeyAxisTo, pgeyDir));
            pgeyFlat = new PgeyPlaced(new PgeyVec3(pgeyF.pgeyP().pgeyX(), pgeyY, pgeyF.pgeyP().pgeyZ()), pgeyF.pgeyInside());
        }
        if (pgeyW <= 0) {
            return pgeyFlat;
        }
        PgeyPlaced pgeyUp = pgeyShoulder(pgeyP);
        if (pgeyFlat == null) {
            return pgeyUp;
        }
        PgeyVec3 pgeyMid = pgeyFlat.pgeyP().pgeyLerp(pgeyUp.pgeyP(), pgeyW);
        PgeyVec3 pgeyC = pgeyTo.pgeyTorsoCenter(pgeyMid.pgeyY());
        PgeyVec3 pgeyV = pgeyMid.pgeySub(pgeyC);
        double pgeyLen = pgeyV.pgeyLength();
        if (pgeyLen < 1e-9) {
            return new PgeyPlaced(pgeyMid, pgeyW < 0.5 ? pgeyFlat.pgeyInside() : pgeyUp.pgeyInside());
        }
        double pgeyR = pgeyFlat.pgeyP().pgeySub(pgeyC).pgeyLength() * (1 - pgeyW) + pgeyUp.pgeyP().pgeySub(pgeyC).pgeyLength() * pgeyW;
        return new PgeyPlaced(pgeyC.pgeyAddScaled(pgeyV.pgeyMul(1 / pgeyLen), pgeyR), pgeyW < 0.5 ? pgeyFlat.pgeyInside() : pgeyUp.pgeyInside());
    }

    private PgeyPlaced pgeyShoulder(PgeyVec3 pgeyP) {
        PgeyVec3 pgeyC = pgeyFrom.pgeyTorsoCenter(pgeyFrom.pgeyArmpit - 0.15 * pgeyFrom.pgeyHeight());
        PgeyVec3 pgeyCt = pgeyTo.pgeyTorsoCenter(pgeyTo.pgeyArmpit - 0.15 * pgeyTo.pgeyHeight());
        PgeyVec3 pgeyV = pgeyP.pgeySub(pgeyC);
        double pgeyD = pgeyV.pgeyLength();
        if (pgeyD < 1e-6) {
            return new PgeyPlaced(pgeyCt, true);
        }
        PgeyVec3 pgeyDir = pgeyV.pgeyMul(1 / pgeyD);
        double pgeySide = Math.abs(pgeyDir.pgeyX()) / Math.max(1e-6, Math.hypot(pgeyDir.pgeyX(), pgeyDir.pgeyZ()));
        double pgeyTolerance = pgeySide > 0.6 ? 1.35 : 2.2;
        double pgeyExit = pgeyFrom.pgeyTorsoExit(pgeyC, pgeyDir);
        double[] pgeyR = pgeyRadius(Double.isNaN(pgeyExit) ? pgeyExit : Math.min(pgeyExit, pgeyD), pgeyD, pgeySkinQ.get().pgeyRay(pgeyCt, pgeyDir, 0.8), pgeyTolerance,
                pgeySide > 0.6 ? PGEY_SHOULDER_MAX_EASE : PGEY_MAX_EASE);
        PgeyVec3 pgeyQ = pgeyCt.pgeyAddScaled(pgeyDir, pgeyR[0]);
        double pgeyCap = pgeyMapY(pgeyP.pgeyY()) + 0.025;
        double pgeyT = pgeySmoothstep(pgeyCap - 0.01, pgeyCap + 0.01, pgeyQ.pgeyY());
        if (pgeyT <= 0) {
            return new PgeyPlaced(pgeyQ, false);
        }
        PgeyVec3 pgeyAxisTo = pgeyTo.pgeyTorsoCenter(pgeyCap);
        PgeyPlaced pgeyFlat = pgeyVertical(pgeyP, pgeyFrom.pgeyTorsoCenter(pgeyP.pgeyY()), pgeyAxisTo, pgeyDirH -> pgeyFrom.pgeyTorsoRadius(pgeyP.pgeyY(), pgeyDirH),
                pgeyDirH -> pgeyBridged(pgeyAxisTo, pgeyDirH));
        PgeyVec3 pgeyF = new PgeyVec3(pgeyFlat.pgeyP().pgeyX(), Math.min(pgeyCap, pgeyQ.pgeyY()), pgeyFlat.pgeyP().pgeyZ());
        PgeyVec3 pgeyMid = pgeyQ.pgeyLerp(pgeyF, pgeyT);
        PgeyVec3 pgeyC0 = pgeyTo.pgeyTorsoCenter(pgeyMid.pgeyY());
        PgeyVec3 pgeyH = new PgeyVec3(pgeyMid.pgeyX() - pgeyC0.pgeyX(), 0, pgeyMid.pgeyZ() - pgeyC0.pgeyZ());
        double pgeyLen = pgeyH.pgeyLength();
        if (pgeyLen < 1e-9) {
            return new PgeyPlaced(pgeyMid, false);
        }
        double pgeyRq = Math.hypot(pgeyQ.pgeyX() - pgeyC0.pgeyX(), pgeyQ.pgeyZ() - pgeyC0.pgeyZ());
        double pgeyRf = Math.hypot(pgeyF.pgeyX() - pgeyC0.pgeyX(), pgeyF.pgeyZ() - pgeyC0.pgeyZ());
        double pgeyRr = pgeyRq + (pgeyRf - pgeyRq) * pgeyT;
        return new PgeyPlaced(new PgeyVec3(pgeyC0.pgeyX() + pgeyH.pgeyX() / pgeyLen * pgeyRr, pgeyMid.pgeyY(), pgeyC0.pgeyZ() + pgeyH.pgeyZ() / pgeyLen * pgeyRr), false);
    }

    private PgeyPlaced pgeyLeg(PgeyVec3 pgeyP) {
        int pgeySign = pgeyP.pgeyX() >= 0 ? 1 : -1;
        PgeyVec3 pgeyAxisTo = pgeyTo.pgeyLegCenter(pgeySign, pgeyMapY(pgeyP.pgeyY()));
        return pgeyVertical(pgeyP, pgeyFrom.pgeyLegCenter(pgeySign, pgeyP.pgeyY()), pgeyAxisTo, pgeyDir -> pgeyFrom.pgeyLegRadius(pgeySign, pgeyP.pgeyY(), pgeyDir),
                pgeyDir -> pgeyBridged(pgeyAxisTo, pgeyDir));
    }

    private double pgeyDrapedFrom(double pgeyY, PgeyVec3 pgeyDir) {
        double pgeyRb = pgeyFrom.pgeyTorsoRadius(pgeyY, pgeyDir);
        double pgeyRc = pgeyFrom.pgeyTorsoRadius(pgeyDrapeTop, pgeyDir) * 0.985;
        double pgeyK = 0.62 * pgeySmoothstep(pgeyDrapeTop, pgeyDrapeBottom, pgeyY);
        return Math.max(pgeyRb, pgeyRb + (pgeyRc - pgeyRb) * pgeyK);
    }

    private double pgeyDrapedTo(double pgeyYFrom, PgeyVec3 pgeyAxisTo, PgeyVec3 pgeyDir) {
        double pgeyRb = pgeyBridged(pgeyAxisTo, pgeyDir);
        if (Double.isNaN(pgeyRb) || pgeyYFrom >= pgeyDrapeTop + 0.08 * pgeyFrom.pgeyHeight()) {
            return pgeyRb;
        }
        double pgeyHang = pgeyRb;
        double pgeyTop = pgeyTo.pgeyArmpit - 0.02 * pgeyTo.pgeyHeight();
        for (double pgeyY = Math.ceil(pgeyAxisTo.pgeyY() * 100) / 100; pgeyY <= pgeyTop + 1e-9; pgeyY += 0.01) {
            double pgeyR = pgeyBridged(pgeyTo.pgeyTorsoCenter(pgeyY), pgeyDir);
            if (!Double.isNaN(pgeyR)) {
                pgeyHang = Math.max(pgeyHang, pgeyR * 0.985 - PGEY_DRAPE_SLOPE * (pgeyY - pgeyAxisTo.pgeyY()));
            }
        }
        return pgeyHang;
    }

    private double pgeyBridged(PgeyVec3 pgeyOrigin, PgeyVec3 pgeyDir) {
        double[][] pgeyHull = pgeySections.computeIfAbsent(List.of(Math.round(pgeyOrigin.pgeyX() * 1000), Math.round(pgeyOrigin.pgeyY() * 1000),
                Math.round(pgeyOrigin.pgeyZ() * 1000)), pgeyK -> pgeySection(pgeyOrigin));
        if (pgeyHull.length < 3) {
            return pgeySkinQ.get().pgeyRay(pgeyOrigin, pgeyDir, 0.6);
        }
        double pgeyDx = pgeyDir.pgeyX();
        double pgeyDz = pgeyDir.pgeyZ();
        double pgeyBest = Double.NaN;
        for (int pgeyI = 0; pgeyI < pgeyHull.length; pgeyI++) {
            double[] pgeyA = pgeyHull[pgeyI];
            double[] pgeyB = pgeyHull[(pgeyI + 1) % pgeyHull.length];
            double pgeyEx = pgeyB[0] - pgeyA[0];
            double pgeyEz = pgeyB[1] - pgeyA[1];
            double pgeyDen = pgeyDx * pgeyEz - pgeyDz * pgeyEx;
            if (Math.abs(pgeyDen) < 1e-12) {
                continue;
            }
            double pgeyT = (pgeyA[0] * pgeyEz - pgeyA[1] * pgeyEx) / pgeyDen;
            double pgeyU = (pgeyA[0] * pgeyDz - pgeyA[1] * pgeyDx) / pgeyDen;
            if (pgeyT > 0 && pgeyU >= -1e-9 && pgeyU <= 1 + 1e-9 && (Double.isNaN(pgeyBest) || pgeyT > pgeyBest)) {
                pgeyBest = pgeyT;
            }
        }
        return Double.isNaN(pgeyBest) ? pgeySkinQ.get().pgeyRay(pgeyOrigin, pgeyDir, 0.6) : pgeyBest;
    }

    private final java.util.concurrent.ConcurrentHashMap<List<Long>, double[][]> pgeySections =
            new java.util.concurrent.ConcurrentHashMap<>();

    private double[][] pgeySection(PgeyVec3 pgeyOrigin) {
        List<double[]> pgeyPts = new ArrayList<>();
        PgeyTriangleIndex.PgeyQuery pgeyQ = pgeySkinQ.get();
        for (int pgeyK = 0; pgeyK < 72; pgeyK++) {
            double pgeyA = 2 * Math.PI * pgeyK / 72;
            double pgeyR = pgeyQ.pgeyRay(pgeyOrigin, new PgeyVec3(Math.sin(pgeyA), 0, Math.cos(pgeyA)), 0.6);
            if (!Double.isNaN(pgeyR)) {
                pgeyPts.add(new double[]{Math.sin(pgeyA) * pgeyR, Math.cos(pgeyA) * pgeyR});
            }
        }
        if (pgeyPts.size() < 3) {
            return new double[0][];
        }
        pgeyPts.sort((pgeyP1, pgeyP2) -> pgeyP1[0] != pgeyP2[0] ? Double.compare(pgeyP1[0], pgeyP2[0]) : Double.compare(pgeyP1[1], pgeyP2[1]));
        double[][] pgeyH = new double[pgeyPts.size() * 2][];
        int pgeyN = 0;
        for (int pgeyPass = 0; pgeyPass < 2; pgeyPass++) {
            int pgeyStart = pgeyN;
            for (int pgeyIdx = 0; pgeyIdx < pgeyPts.size(); pgeyIdx++) {
                double[] pgeyPt = pgeyPts.get(pgeyPass == 0 ? pgeyIdx : pgeyPts.size() - 1 - pgeyIdx);
                while (pgeyN >= pgeyStart + 2 && pgeyCross(pgeyH[pgeyN - 2], pgeyH[pgeyN - 1], pgeyPt) <= 0) {
                    pgeyN--;
                }
                pgeyH[pgeyN++] = pgeyPt;
            }
            pgeyN--;
        }
        return Arrays.copyOf(pgeyH, pgeyN);
    }

    private static double pgeyCross(double[] pgeyO, double[] pgeyA, double[] pgeyB) {
        return (pgeyA[0] - pgeyO[0]) * (pgeyB[1] - pgeyO[1]) - (pgeyA[1] - pgeyO[1]) * (pgeyB[0] - pgeyO[0]);
    }

    private PgeyPlaced pgeyVertical(PgeyVec3 pgeyP, PgeyVec3 pgeyAxisFrom, PgeyVec3 pgeyAxisTo, java.util.function.ToDoubleFunction<PgeyVec3> pgeySurfaceFrom,
                            java.util.function.ToDoubleFunction<PgeyVec3> pgeySurfaceTo) {
        PgeyVec3 pgeyH = new PgeyVec3(pgeyP.pgeyX() - pgeyAxisFrom.pgeyX(), 0, pgeyP.pgeyZ() - pgeyAxisFrom.pgeyZ());
        double pgeyD = pgeyH.pgeyLength();
        double pgeyY = pgeyAxisTo.pgeyY();
        if (pgeyD < 1e-6) {
            return new PgeyPlaced(new PgeyVec3(pgeyAxisTo.pgeyX(), pgeyY, pgeyAxisTo.pgeyZ()), true);
        }
        PgeyVec3 pgeyDir = pgeyH.pgeyMul(1 / pgeyD);
        double[] pgeyR = pgeyRadius(pgeySurfaceFrom.applyAsDouble(pgeyDir), pgeyD, pgeySurfaceTo.applyAsDouble(pgeyDir), 2.2, PGEY_MAX_EASE);
        return new PgeyPlaced(new PgeyVec3(pgeyAxisTo.pgeyX() + pgeyDir.pgeyX() * pgeyR[0], pgeyY, pgeyAxisTo.pgeyZ() + pgeyDir.pgeyZ() * pgeyR[0]), pgeyR[1] > 0);
    }

    private PgeyPlaced pgeyArm(PgeyVec3 pgeyP) {
        int pgeySign = pgeyP.pgeyX() >= 0 ? 1 : -1;
        double pgeyS = pgeyFrom.pgeyArmProject(pgeySign, pgeyP);
        PgeyVec3 pgeyC = pgeyFrom.pgeyArmPoint(pgeySign, pgeyS);
        PgeyVec3 pgeyAxis = pgeyFrom.pgeyArmDirection(pgeySign, pgeyS);
        PgeyVec3 pgeyV = pgeyP.pgeySub(pgeyC);
        pgeyV = pgeyV.pgeySub(pgeyAxis.pgeyMul(pgeyV.pgeyDot(pgeyAxis)));
        double pgeyD = pgeyV.pgeyLength();
        double pgeySh = pgeyS >= 0 ? pgeyS * pgeyRatio
                : pgeyS * pgeyRatio * Math.abs(pgeyAxis.pgeyY()) / Math.max(0.3, Math.abs(pgeyTo.pgeyArmDirection(pgeySign, 0).pgeyY()));
        PgeyVec3 pgeyCh = pgeyTo.pgeyArmPoint(pgeySign, pgeySh);
        if (pgeyD < 1e-6) {
            return new PgeyPlaced(pgeyCh, true);
        }
        PgeyVec3[] pgeyFFrom = pgeyFrame(pgeyAxis);
        double pgeyAngle = Math.atan2(pgeyV.pgeyDot(pgeyFFrom[1]), pgeyV.pgeyDot(pgeyFFrom[0]));
        PgeyVec3[] pgeyFTo = pgeyFrame(pgeyTo.pgeyArmDirection(pgeySign, pgeySh));
        PgeyVec3 pgeyDirFrom = pgeyV.pgeyMul(1 / pgeyD);
        PgeyVec3 pgeyDirTo = pgeyFTo[0].pgeyMul(Math.cos(pgeyAngle)).pgeyAdd(pgeyFTo[1].pgeyMul(Math.sin(pgeyAngle)));
        double pgeyTube = PGEY_SLEEVE_TUBE * pgeyTo.pgeyHeight();
        double pgeyMeasureAt = Math.max(pgeySh, pgeyTube);
        PgeyVec3 pgeyCm = pgeyTo.pgeyArmPoint(pgeySign, pgeyMeasureAt);
        PgeyVec3[] pgeyFm = pgeyFrame(pgeyTo.pgeyArmDirection(pgeySign, pgeyMeasureAt));
        PgeyVec3 pgeyDirM = pgeyFm[0].pgeyMul(Math.cos(pgeyAngle)).pgeyAdd(pgeyFm[1].pgeyMul(Math.sin(pgeyAngle)));
        double pgeySFrom = pgeySh >= pgeyTube ? pgeyS : pgeyTube / pgeyRatio;
        double[] pgeyR = pgeyRadius(pgeyFrom.pgeyArmRadius(pgeySign, pgeySFrom, pgeyDirFrom), pgeyD, pgeySkinQ.get().pgeyRay(pgeyCm, pgeyDirM, 0.3), 1.35, PGEY_MAX_EASE);
        double pgeyFlare = PGEY_SLEEVE_FLARE * Math.max(0, pgeySh - pgeyTube);
        return new PgeyPlaced(pgeyCh.pgeyAddScaled(pgeyDirTo, pgeyR[0] + (pgeyR[1] > 0 ? 0 : pgeyFlare)), pgeyR[1] > 0);
    }

    private double[] pgeyRadius(double pgeyRFrom, double pgeyD, double pgeyMeasured, double pgeyTolerance, double pgeyMaxEase) {
        boolean pgeyInside = !Double.isNaN(pgeyRFrom) && pgeyD < pgeyRFrom - 0.001;
        double pgeyE = Double.isNaN(pgeyRFrom) ? 0.008 : pgeyClamp(pgeyD - pgeyRFrom, PGEY_MIN_EASE, pgeyMaxEase);
        double pgeyExpected = (Double.isNaN(pgeyRFrom) ? pgeyD - pgeyE : pgeyRFrom) * pgeyRatio;
        double pgeyRTo = pgeyMeasured;
        if (Double.isNaN(pgeyRTo) || pgeyRTo < 0.4 * pgeyExpected || pgeyRTo > pgeyTolerance * pgeyExpected + 0.03) {
            pgeyRTo = pgeyExpected;
        }
        if (pgeyInside) {
            return new double[]{pgeyRTo * Math.max(0, pgeyD) / pgeyRFrom, 1};
        }
        return new double[]{pgeyRTo + pgeyE, 0};
    }

    double pgeyMapY(double pgeyY) {
        double[] pgeyA = {pgeyFrom.pgeyFloor, pgeyFrom.pgeyCrotch, pgeyFrom.pgeyArmpit, pgeyFrom.pgeyNeck};
        double[] pgeyB = {pgeyTo.pgeyFloor, pgeyTo.pgeyCrotch, pgeyTo.pgeyArmpit, pgeyTo.pgeyNeck};
        if (pgeyY <= pgeyA[0]) {
            return pgeyB[0] + (pgeyY - pgeyA[0]) * pgeyRatio;
        }
        for (int pgeyI = 1; pgeyI < pgeyA.length; pgeyI++) {
            if (pgeyY <= pgeyA[pgeyI]) {
                double pgeyT = (pgeyY - pgeyA[pgeyI - 1]) / (pgeyA[pgeyI] - pgeyA[pgeyI - 1]);
                return pgeyB[pgeyI - 1] + pgeyT * (pgeyB[pgeyI] - pgeyB[pgeyI - 1]);
            }
        }
        return pgeyB[pgeyB.length - 1] + (pgeyY - pgeyA[pgeyA.length - 1]) * pgeyRatio;
    }

    private static PgeyVec3[] pgeyFrame(PgeyVec3 pgeyAxis) {
        PgeyVec3 pgeyF = PgeyVec3.PGEY_FRONT.pgeySub(pgeyAxis.pgeyMul(PgeyVec3.PGEY_FRONT.pgeyDot(pgeyAxis)));
        if (pgeyF.pgeyLength() < 1e-6) {
            pgeyF = new PgeyVec3(1, 0, 0).pgeySub(pgeyAxis.pgeyMul(pgeyAxis.pgeyX()));
        }
        pgeyF = pgeyF.pgeyNormalize();
        return new PgeyVec3[]{pgeyF, pgeyAxis.pgeyCross(pgeyF).pgeyNormalize()};
    }

    private static void pgeySmoothDisplacement(PgeyMeshData pgeyMesh, int[] pgeyCanon, double[] pgeySrc, double[] pgeyDst, boolean[] pgeyInside,
                                           int pgeyM, int pgeyPasses) {
        List<int[]> pgeyEdges = new ArrayList<>();
        int[] pgeyF = pgeyMesh.pgeyFaces();
        int[] pgeyDegree = new int[pgeyM];
        for (int pgeyK = 0; pgeyK < pgeyMesh.pgeyFaceCount(); pgeyK++) {
            int pgeyA = pgeyCanon[pgeyF[pgeyK * PgeyMeshData.PGEY_FACE_STRIDE]];
            int pgeyB = pgeyCanon[pgeyF[pgeyK * PgeyMeshData.PGEY_FACE_STRIDE + 3]];
            int pgeyC = pgeyCanon[pgeyF[pgeyK * PgeyMeshData.PGEY_FACE_STRIDE + 6]];
            for (int[] pgeyE : new int[][]{{pgeyA, pgeyB}, {pgeyB, pgeyC}, {pgeyC, pgeyA}}) {
                if (pgeyInside[pgeyE[0]] == pgeyInside[pgeyE[1]]) {
                    pgeyEdges.add(pgeyE);
                    pgeyDegree[pgeyE[0]]++;
                    pgeyDegree[pgeyE[1]]++;
                }
            }
        }
        double[] pgeyDelta = new double[pgeyM * 3];
        for (int pgeyI = 0; pgeyI < pgeyM * 3; pgeyI++) {
            pgeyDelta[pgeyI] = pgeyDst[pgeyI] - pgeySrc[pgeyI];
        }
        for (int pgeyPass = 0; pgeyPass < pgeyPasses; pgeyPass++) {
            double[] pgeySum = new double[pgeyM * 3];
            for (int[] pgeyE : pgeyEdges) {
                for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                    pgeySum[pgeyE[0] * 3 + pgeyK] += pgeyDelta[pgeyE[1] * 3 + pgeyK];
                    pgeySum[pgeyE[1] * 3 + pgeyK] += pgeyDelta[pgeyE[0] * 3 + pgeyK];
                }
            }
            for (int pgeyC = 0; pgeyC < pgeyM; pgeyC++) {
                if (pgeyDegree[pgeyC] == 0) {
                    continue;
                }
                for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                    pgeyDelta[pgeyC * 3 + pgeyK] = 0.5 * pgeyDelta[pgeyC * 3 + pgeyK] + 0.5 * pgeySum[pgeyC * 3 + pgeyK] / pgeyDegree[pgeyC];
                }
            }
        }
        for (int pgeyI = 0; pgeyI < pgeyM * 3; pgeyI++) {
            pgeyDst[pgeyI] = pgeySrc[pgeyI] + pgeyDelta[pgeyI];
        }
    }

    private void pgeyPushOut(double[] pgeyDst, int pgeyC, double pgeyGap) {
        PgeyTriangleIndex.PgeyQuery pgeyQ = pgeySkinQ.get();
        for (int pgeyIt = 0; pgeyIt < 3; pgeyIt++) {
            PgeyVec3 pgeyP = new PgeyVec3(pgeyDst[pgeyC * 3], pgeyDst[pgeyC * 3 + 1], pgeyDst[pgeyC * 3 + 2]);
            PgeyTriangleIndex.PgeyHit pgeyHit = pgeyQ.pgeyNearest(pgeyP, 0.03);
            if (pgeyHit == null) {
                pgeyHit = pgeyQ.pgeyNearest(pgeyP, 0.12);
            }
            if (pgeyHit == null) {
                return;
            }
            double pgeySd = pgeyP.pgeySub(pgeyHit.pgeyPoint()).pgeyDot(pgeyHit.pgeyNormal());
            if (pgeySd >= pgeyGap) {
                return;
            }
            PgeyVec3 pgeyMoved = pgeyP.pgeyAddScaled(pgeyHit.pgeyNormal(), pgeyGap - pgeySd);
            pgeyDst[pgeyC * 3] = pgeyMoved.pgeyX();
            pgeyDst[pgeyC * 3 + 1] = pgeyMoved.pgeyY();
            pgeyDst[pgeyC * 3 + 2] = pgeyMoved.pgeyZ();
        }
    }

    private final class PgeyShoes {
        private final PgeyFoot[] pgeyShoe = new PgeyFoot[2];
        private final PgeyFoot[] pgeyFoot = new PgeyFoot[2];

        PgeyShoes(PgeyModelData pgeySource) {
            List<double[]> pgeyShoePts = new ArrayList<>();
            pgeySource.pgeyZones().forEach((pgeyZone, pgeyParts) -> {
                if (pgeyZone.pgeyGarment() == HepyGarment.SHOES) {
                    for (PgeyPart pgeyP : pgeyParts) {
                        float[] pgeyPts = pgeyP.pgeyMesh().pgeyPoints();
                        for (int pgeyI = 0; pgeyI < pgeyPts.length; pgeyI += 3) {
                            pgeyShoePts.add(new double[]{pgeyPts[pgeyI], -pgeyPts[pgeyI + 1], -pgeyPts[pgeyI + 2]});
                        }
                    }
                }
            });
            List<double[]> pgeyFootPts = new ArrayList<>();
            double[] pgeyV = pgeySkin.pgeyVertices();
            double pgeyFootTop = pgeyTo.pgeyFloor + 0.045 * pgeyTo.pgeyHeight();
            for (int pgeyI = 0; pgeyI < pgeyV.length; pgeyI += 3) {
                if (pgeyV[pgeyI + 1] < pgeyFootTop && Math.abs(pgeyV[pgeyI]) < 0.2 * pgeyTo.pgeyHeight()) {
                    pgeyFootPts.add(new double[]{pgeyV[pgeyI], pgeyV[pgeyI + 1], pgeyV[pgeyI + 2]});
                }
            }
            for (int pgeyK = 0; pgeyK < 2; pgeyK++) {
                int pgeySign = pgeyK == 0 ? -1 : 1;
                pgeyShoe[pgeyK] = PgeyFoot.pgeyOf(pgeyShoePts, pgeySign);
                pgeyFoot[pgeyK] = PgeyFoot.pgeyOf(pgeyFootPts, pgeySign);
            }
        }

        PgeyMeshData pgeyPlace(PgeyMeshData pgeyMesh) {
            float[] pgeyPts = pgeyMesh.pgeyPoints();
            float[] pgeyOut = new float[pgeyPts.length];
            for (int pgeyI = 0; pgeyI < pgeyPts.length; pgeyI += 3) {
                double pgeyX = pgeyPts[pgeyI];
                double pgeyY = -pgeyPts[pgeyI + 1];
                double pgeyZ = -pgeyPts[pgeyI + 2];
                int pgeyK = pgeyX >= 0 ? 1 : 0;
                PgeyFoot pgeyS = pgeyShoe[pgeyK];
                PgeyFoot pgeyF = pgeyFoot[pgeyK];
                if (pgeyS == null || pgeyF == null) {
                    pgeyOut[pgeyI] = pgeyPts[pgeyI];
                    pgeyOut[pgeyI + 1] = pgeyPts[pgeyI + 1];
                    pgeyOut[pgeyI + 2] = pgeyPts[pgeyI + 2];
                    continue;
                }
                double pgeyQx = pgeyX - pgeyS.pgeyCx;
                double pgeyQz = pgeyZ - pgeyS.pgeyCz;
                double pgeyAlong = pgeyQx * pgeyS.pgeyDx + pgeyQz * pgeyS.pgeyDz;
                double pgeyAcross = pgeyQx * -pgeyS.pgeyDz + pgeyQz * pgeyS.pgeyDx;
                double pgeySl = (pgeyF.pgeyLength + 0.025) / pgeyS.pgeyLength;
                double pgeySw = (pgeyF.pgeyWidth + 0.016) / pgeyS.pgeyWidth;
                double pgeySy = pgeyClamp((pgeySl + pgeySw) / 2, 0.6, 1.25);
                double pgeyNx = pgeyF.pgeyCx + pgeyF.pgeyDx * pgeyAlong * pgeySl - pgeyF.pgeyDz * pgeyAcross * pgeySw;
                double pgeyNz = pgeyF.pgeyCz + pgeyF.pgeyDz * pgeyAlong * pgeySl + pgeyF.pgeyDx * pgeyAcross * pgeySw;
                pgeyOut[pgeyI] = (float) pgeyNx;
                pgeyOut[pgeyI + 1] = (float) -(pgeyY * pgeySy + pgeyTo.pgeyFloor);
                pgeyOut[pgeyI + 2] = (float) -pgeyNz;
            }
            return pgeyWithNormals(pgeyMesh, pgeyOut);
        }

        double pgeyCutHeight(double pgeyX, double pgeyZ, double pgeyK) {
            PgeyFoot pgeyF = pgeyFoot[pgeyX >= 0 ? 1 : 0];
            PgeyFoot pgeyS = pgeyShoe[pgeyX >= 0 ? 1 : 0];
            if (pgeyF == null || pgeyS == null) {
                return Double.NEGATIVE_INFINITY;
            }
            double pgeyQx = pgeyX - pgeyF.pgeyCx;
            double pgeyQz = pgeyZ - pgeyF.pgeyCz;
            double pgeyAlong = pgeyQx * pgeyF.pgeyDx + pgeyQz * pgeyF.pgeyDz;
            double pgeyAcross = pgeyQx * -pgeyF.pgeyDz + pgeyQz * pgeyF.pgeyDx;
            double pgeyHalfL = (pgeyF.pgeyLength + 0.025) / 2;
            double pgeyHalfW = (pgeyF.pgeyWidth + 0.016) / 2;
            if (Math.abs(pgeyAlong) > pgeyHalfL || Math.abs(pgeyAcross) > pgeyHalfW) {
                return Double.NEGATIVE_INFINITY;
            }
            double pgeySy = pgeyClamp(((pgeyF.pgeyLength + 0.025) / pgeyS.pgeyLength + (pgeyF.pgeyWidth + 0.016) / pgeyS.pgeyWidth) / 2, 0.6, 1.25);
            return pgeyTo.pgeyFloor + pgeyS.pgeyHeight * pgeySy * pgeyK;
        }
    }

    private record PgeyFoot(double pgeyCx, double pgeyCz, double pgeyDx, double pgeyDz, double pgeyLength, double pgeyWidth, double pgeyHeight) {

        static PgeyFoot pgeyOf(List<double[]> pgeyPts, int pgeySign) {
            double pgeySx = 0;
            double pgeySz = 0;
            int pgeyN = 0;
            for (double[] pgeyP : pgeyPts) {
                if (pgeyP[0] * pgeySign > 0) {
                    pgeySx += pgeyP[0];
                    pgeySz += pgeyP[2];
                    pgeyN++;
                }
            }
            if (pgeyN < 10) {
                return null;
            }
            double pgeyMx = pgeySx / pgeyN;
            double pgeyMz = pgeySz / pgeyN;
            double pgeyCxx = 0;
            double pgeyCxz = 0;
            double pgeyCzz = 0;
            for (double[] pgeyP : pgeyPts) {
                if (pgeyP[0] * pgeySign > 0) {
                    double pgeyA = pgeyP[0] - pgeyMx;
                    double pgeyB = pgeyP[2] - pgeyMz;
                    pgeyCxx += pgeyA * pgeyA;
                    pgeyCxz += pgeyA * pgeyB;
                    pgeyCzz += pgeyB * pgeyB;
                }
            }
            double pgeyAngle = 0.5 * Math.atan2(2 * pgeyCxz, pgeyCzz - pgeyCxx);
            double pgeyDx = Math.sin(pgeyAngle);
            double pgeyDz = Math.cos(pgeyAngle);
            if (pgeyDz < 0) {
                pgeyDx = -pgeyDx;
                pgeyDz = -pgeyDz;
            }
            double pgeyMinA = Double.MAX_VALUE;
            double pgeyMaxA = -Double.MAX_VALUE;
            double pgeyMinC = Double.MAX_VALUE;
            double pgeyMaxC = -Double.MAX_VALUE;
            double pgeyMaxY = 0;
            for (double[] pgeyP : pgeyPts) {
                if (pgeyP[0] * pgeySign > 0) {
                    double pgeyA = (pgeyP[0] - pgeyMx) * pgeyDx + (pgeyP[2] - pgeyMz) * pgeyDz;
                    double pgeyC = (pgeyP[0] - pgeyMx) * -pgeyDz + (pgeyP[2] - pgeyMz) * pgeyDx;
                    pgeyMinA = Math.min(pgeyMinA, pgeyA);
                    pgeyMaxA = Math.max(pgeyMaxA, pgeyA);
                    pgeyMinC = Math.min(pgeyMinC, pgeyC);
                    pgeyMaxC = Math.max(pgeyMaxC, pgeyC);
                    pgeyMaxY = Math.max(pgeyMaxY, pgeyP[1]);
                }
            }
            double pgeyMidA = (pgeyMinA + pgeyMaxA) / 2;
            double pgeyMidC = (pgeyMinC + pgeyMaxC) / 2;
            return new PgeyFoot(pgeyMx + pgeyDx * pgeyMidA - pgeyDz * pgeyMidC, pgeyMz + pgeyDz * pgeyMidA + pgeyDx * pgeyMidC, pgeyDx, pgeyDz, pgeyMaxA - pgeyMinA,
                    pgeyMaxC - pgeyMinC, pgeyMaxY);
        }
    }

    private static PgeyMeshData pgeyRemoveBelow(PgeyMeshData pgeyMesh, PgeyShoes pgeyShoes, double pgeyK) {
        float[] pgeyP = pgeyMesh.pgeyPoints();
        int[] pgeyF = pgeyMesh.pgeyFaces();
        int[] pgeyKept = new int[pgeyF.length];
        int pgeyN = 0;
        for (int pgeyT = 0; pgeyT < pgeyMesh.pgeyFaceCount(); pgeyT++) {
            boolean pgeyInside = true;
            for (int pgeyC = 0; pgeyC < 3 && pgeyInside; pgeyC++) {
                int pgeyI = pgeyF[pgeyT * PgeyMeshData.PGEY_FACE_STRIDE + pgeyC * 3] * 3;
                double pgeyY = -pgeyP[pgeyI + 1];
                pgeyInside = pgeyY < pgeyShoes.pgeyCutHeight(pgeyP[pgeyI], -pgeyP[pgeyI + 2], pgeyK);
            }
            if (!pgeyInside) {
                System.arraycopy(pgeyF, pgeyT * PgeyMeshData.PGEY_FACE_STRIDE, pgeyKept, pgeyN, PgeyMeshData.PGEY_FACE_STRIDE);
                pgeyN += PgeyMeshData.PGEY_FACE_STRIDE;
            }
        }
        return pgeyN == pgeyF.length ? pgeyMesh : new PgeyMeshData(pgeyMesh.pgeyPoints(), pgeyMesh.pgeyNormals(), pgeyMesh.pgeyTexCoords(), Arrays.copyOf(pgeyKept, pgeyN));
    }

    private static PgeyMeshData pgeyHideCovered(PgeyMeshData pgeyMesh, Map<PgeyZone, List<PgeyPart>> pgeyZones, List<PgeyVec3> pgeyExtraOpenings,
                                        java.util.Set<HepyGarment> pgeySimulated) {
        List<PgeyMeshData> pgeyCloth = new ArrayList<>();
        for (PgeyZone pgeyZ : List.of(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_SIDES, PgeyZone.JERSEY_SLEEVES, PgeyZone.JERSEY_COLLAR,
                PgeyZone.JERSEY_CUFFS, PgeyZone.SHORTS_BASE, PgeyZone.SHORTS_WAIST, PgeyZone.SHORTS_STRIPE)) {
            pgeyZones.getOrDefault(pgeyZ, List.of()).forEach(pgeyP -> pgeyCloth.add(pgeyP.pgeyMesh()));
        }
        if (pgeyCloth.isEmpty()) {
            return pgeyMesh;
        }
        PgeyOpenings pgeyOpenings = new PgeyOpenings();
        for (PgeyZone pgeyZ : List.of(PgeyZone.JERSEY_BODY, PgeyZone.JERSEY_COLLAR, PgeyZone.JERSEY_CUFFS, PgeyZone.SHORTS_BASE)) {
            if (!pgeySimulated.contains(pgeyZ.pgeyGarment())) {
                pgeyZones.getOrDefault(pgeyZ, List.of()).forEach(pgeyPart -> pgeyOpenings.pgeyAddBoundary(pgeyPart.pgeyMesh()));
            }
        }
        pgeyExtraOpenings.forEach(pgeyOpenings::pgeyAdd);
        PgeyTriangleIndex pgeyIndex = PgeyTriangleIndex.pgeyOf(pgeyCloth, PGEY_CELL);
        ThreadLocal<PgeyTriangleIndex.PgeyQuery> pgeyQ = ThreadLocal.withInitial(pgeyIndex::pgeyQuery);
        float[] pgeyP = pgeyMesh.pgeyPoints();
        float[] pgeyNrm = pgeyMesh.pgeyNormals();
        int[] pgeyF = pgeyMesh.pgeyFaces();
        boolean[] pgeyCovered = new boolean[pgeyMesh.pgeyPointCount()];
        IntStream.range(0, pgeyMesh.pgeyPointCount()).parallel().forEach(pgeyI -> {
            PgeyVec3 pgeyO = new PgeyVec3(pgeyP[pgeyI * 3], -pgeyP[pgeyI * 3 + 1], -pgeyP[pgeyI * 3 + 2]);
            PgeyVec3 pgeyN = new PgeyVec3(pgeyNrm[pgeyI * 3], -pgeyNrm[pgeyI * 3 + 1], -pgeyNrm[pgeyI * 3 + 2]).pgeyNormalize();
            PgeyTriangleIndex.PgeyQuery pgeyQuery = pgeyQ.get();
            boolean pgeyUnder = !Double.isNaN(pgeyQuery.pgeyRay(pgeyO, pgeyN, 0.08)) && !pgeyOpenings.pgeyNear(pgeyO, 0.035);
            pgeyCovered[pgeyI] = pgeyUnder || !Double.isNaN(pgeyQuery.pgeyRay(pgeyO, pgeyN.pgeyMul(-1), 0.02));
        });
        int[] pgeyKept = new int[pgeyF.length];
        int pgeyK = 0;
        for (int pgeyT = 0; pgeyT < pgeyMesh.pgeyFaceCount(); pgeyT++) {
            int pgeyB = pgeyT * PgeyMeshData.PGEY_FACE_STRIDE;
            if (!(pgeyCovered[pgeyF[pgeyB]] && pgeyCovered[pgeyF[pgeyB + 3]] && pgeyCovered[pgeyF[pgeyB + 6]])) {
                System.arraycopy(pgeyF, pgeyB, pgeyKept, pgeyK, PgeyMeshData.PGEY_FACE_STRIDE);
                pgeyK += PgeyMeshData.PGEY_FACE_STRIDE;
            }
        }
        return pgeyK == pgeyF.length ? pgeyMesh : new PgeyMeshData(pgeyP, pgeyNrm, pgeyMesh.pgeyTexCoords(), Arrays.copyOf(pgeyKept, pgeyK));
    }

    private static final class PgeyOpenings {
        private static final double PGEY_CELL_SIZE = 0.035;
        private final Map<List<Integer>, List<PgeyVec3>> pgeyGrid = new HashMap<>();

        void pgeyAddBoundary(PgeyMeshData pgeyMesh) {
            float[] pgeyPts = pgeyMesh.pgeyPoints();
            int[] pgeyCanon = new int[pgeyMesh.pgeyPointCount()];
            int[] pgeyFirst = pgeyWeld(pgeyPts, pgeyCanon);
            boolean[] pgeyEdge = PgeyClothOps.pgeyBoundary(pgeyMesh, pgeyCanon, pgeyFirst.length);
            for (int pgeyC = 0; pgeyC < pgeyFirst.length; pgeyC++) {
                if (pgeyEdge[pgeyC]) {
                    int pgeyI = pgeyFirst[pgeyC] * 3;
                    pgeyAdd(new PgeyVec3(pgeyPts[pgeyI], -pgeyPts[pgeyI + 1], -pgeyPts[pgeyI + 2]));
                }
            }
        }

        void pgeyAdd(PgeyVec3 pgeyV) {
            pgeyGrid.computeIfAbsent(pgeyCell(pgeyV), pgeyK -> new ArrayList<>()).add(pgeyV);
        }

        boolean pgeyNear(PgeyVec3 pgeyV, double pgeyR) {
            List<Integer> pgeyC = pgeyCell(pgeyV);
            for (int pgeyDx = -1; pgeyDx <= 1; pgeyDx++) {
                for (int pgeyDy = -1; pgeyDy <= 1; pgeyDy++) {
                    for (int pgeyDz = -1; pgeyDz <= 1; pgeyDz++) {
                        List<PgeyVec3> pgeyList = pgeyGrid.get(List.of(pgeyC.get(0) + pgeyDx, pgeyC.get(1) + pgeyDy, pgeyC.get(2) + pgeyDz));
                        if (pgeyList != null) {
                            for (PgeyVec3 pgeyO : pgeyList) {
                                if (pgeyO.pgeySub(pgeyV).pgeyLength() < pgeyR) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }

        private static List<Integer> pgeyCell(PgeyVec3 pgeyV) {
            return List.of((int) Math.floor(pgeyV.pgeyX() / PGEY_CELL_SIZE), (int) Math.floor(pgeyV.pgeyY() / PGEY_CELL_SIZE),
                    (int) Math.floor(pgeyV.pgeyZ() / PGEY_CELL_SIZE));
        }
    }

    private static PgeyMeshData pgeyWithNormals(PgeyMeshData pgeyMesh, float[] pgeyPoints) {
        float[] pgeyOld = pgeyMesh.pgeyNormals();
        double[] pgeyAcc = new double[pgeyOld.length];
        int[] pgeyF = pgeyMesh.pgeyFaces();
        for (int pgeyT = 0; pgeyT < pgeyMesh.pgeyFaceCount(); pgeyT++) {
            int pgeyB = pgeyT * PgeyMeshData.PGEY_FACE_STRIDE;
            int pgeyA = pgeyF[pgeyB] * 3;
            int pgeyC = pgeyF[pgeyB + 3] * 3;
            int pgeyD = pgeyF[pgeyB + 6] * 3;
            double pgeyUx = pgeyPoints[pgeyC] - pgeyPoints[pgeyA];
            double pgeyUy = pgeyPoints[pgeyC + 1] - pgeyPoints[pgeyA + 1];
            double pgeyUz = pgeyPoints[pgeyC + 2] - pgeyPoints[pgeyA + 2];
            double pgeyWx = pgeyPoints[pgeyD] - pgeyPoints[pgeyA];
            double pgeyWy = pgeyPoints[pgeyD + 1] - pgeyPoints[pgeyA + 1];
            double pgeyWz = pgeyPoints[pgeyD + 2] - pgeyPoints[pgeyA + 2];
            double pgeyNx = pgeyUy * pgeyWz - pgeyUz * pgeyWy;
            double pgeyNy = pgeyUz * pgeyWx - pgeyUx * pgeyWz;
            double pgeyNz = pgeyUx * pgeyWy - pgeyUy * pgeyWx;
            for (int pgeyK = 0; pgeyK < 3; pgeyK++) {
                int pgeyNi = pgeyF[pgeyB + pgeyK * 3 + 1] * 3;
                pgeyAcc[pgeyNi] += pgeyNx;
                pgeyAcc[pgeyNi + 1] += pgeyNy;
                pgeyAcc[pgeyNi + 2] += pgeyNz;
            }
        }
        float[] pgeyNormals = new float[pgeyOld.length];
        for (int pgeyI = 0; pgeyI < pgeyOld.length; pgeyI += 3) {
            double pgeyLen = Math.sqrt(pgeyAcc[pgeyI] * pgeyAcc[pgeyI] + pgeyAcc[pgeyI + 1] * pgeyAcc[pgeyI + 1] + pgeyAcc[pgeyI + 2] * pgeyAcc[pgeyI + 2]);
            if (pgeyAcc[pgeyI] * pgeyOld[pgeyI] + pgeyAcc[pgeyI + 1] * pgeyOld[pgeyI + 1] + pgeyAcc[pgeyI + 2] * pgeyOld[pgeyI + 2] < 0) {
                pgeyLen = -pgeyLen;
            }
            if (Math.abs(pgeyLen) < 1e-12) {
                pgeyNormals[pgeyI] = pgeyOld[pgeyI];
                pgeyNormals[pgeyI + 1] = pgeyOld[pgeyI + 1];
                pgeyNormals[pgeyI + 2] = pgeyOld[pgeyI + 2];
            } else {
                pgeyNormals[pgeyI] = (float) (pgeyAcc[pgeyI] / pgeyLen);
                pgeyNormals[pgeyI + 1] = (float) (pgeyAcc[pgeyI + 1] / pgeyLen);
                pgeyNormals[pgeyI + 2] = (float) (pgeyAcc[pgeyI + 2] / pgeyLen);
            }
        }
        return new PgeyMeshData(pgeyPoints, pgeyNormals, pgeyMesh.pgeyTexCoords(), pgeyMesh.pgeyFaces());
    }

    private static int[] pgeyWeld(float[] pgeyPts, int[] pgeyCanon) {
        Map<List<Integer>, Integer> pgeyIds = new HashMap<>();
        List<Integer> pgeyFirst = new ArrayList<>();
        for (int pgeyI = 0; pgeyI < pgeyCanon.length; pgeyI++) {
            List<Integer> pgeyKey = List.of(Math.round(pgeyPts[pgeyI * 3] * 1e5f), Math.round(pgeyPts[pgeyI * 3 + 1] * 1e5f),
                    Math.round(pgeyPts[pgeyI * 3 + 2] * 1e5f));
            Integer pgeyId = pgeyIds.get(pgeyKey);
            if (pgeyId == null) {
                pgeyId = pgeyFirst.size();
                pgeyIds.put(pgeyKey, pgeyId);
                pgeyFirst.add(pgeyI);
            }
            pgeyCanon[pgeyI] = pgeyId;
        }
        return pgeyFirst.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Map<HepyGarment, PgeyFocus> pgeyFocus(Map<PgeyZone, List<PgeyPart>> pgeyZones, PgeyHumanModel pgeyHuman) {
        double pgeyH = pgeyHuman.pgeyHeight();
        double pgeyK = pgeyH / 1.812;
        Map<HepyGarment, double[]> pgeyBoxes = new EnumMap<>(HepyGarment.class);
        pgeyZones.forEach((pgeyZone, pgeyParts) -> {
            double[] pgeyBox = pgeyBoxes.computeIfAbsent(pgeyZone.pgeyGarment(),
                    pgeyG -> new double[]{Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE});
            for (PgeyPart pgeyP : pgeyParts) {
                float[] pgeyPts = pgeyP.pgeyMesh().pgeyPoints();
                for (int pgeyI = 0; pgeyI < pgeyPts.length; pgeyI += 3) {
                    pgeyBox[0] = Math.min(pgeyBox[0], -pgeyPts[pgeyI + 1]);
                    pgeyBox[1] = Math.max(pgeyBox[1], -pgeyPts[pgeyI + 1]);
                    pgeyBox[2] = Math.min(pgeyBox[2], -pgeyPts[pgeyI + 2]);
                    pgeyBox[3] = Math.max(pgeyBox[3], -pgeyPts[pgeyI + 2]);
                }
            }
        });
        Map<HepyGarment, PgeyFocus> pgeyFocus = new EnumMap<>(HepyGarment.class);
        Map<HepyGarment, double[]> pgeyLook = Map.of(HepyGarment.JERSEY, new double[]{1.85, 4}, HepyGarment.SHORTS, new double[]{1.50, 3},
                HepyGarment.SOCKS, new double[]{1.40, 7}, HepyGarment.SHOES, new double[]{0.95, 18});
        pgeyBoxes.forEach((pgeyG, pgeyBox) -> {
            double[] pgeyL = pgeyLook.getOrDefault(pgeyG, new double[]{1.6, 5});
            pgeyFocus.put(pgeyG, new PgeyFocus(new PgeyVec3(0, (pgeyBox[0] + pgeyBox[1]) / 2, (pgeyBox[2] + pgeyBox[3]) / 2), pgeyL[0] * pgeyK, pgeyL[1]));
        });
        PgeyFocus pgeyFull = pgeyFullBody(pgeyHuman.pgeyMinY(), pgeyH);
        for (HepyGarment pgeyG : HepyGarment.values()) {
            pgeyFocus.putIfAbsent(pgeyG, pgeyFull);
        }
        pgeyFocus.put(HepyGarment.KIT, pgeyFull);
        return pgeyFocus;
    }

    public static PgeyFocus pgeyFullBody(double pgeyMinY, double pgeyHeight) {
        return new PgeyFocus(new PgeyVec3(0, pgeyMinY + 0.535 * pgeyHeight, 0), 4.6 * pgeyHeight / 1.812, 5);
    }

    private static double pgeyClamp(double pgeyX, double pgeyLo, double pgeyHi) {
        return Math.max(pgeyLo, Math.min(pgeyHi, pgeyX));
    }

    private static double pgeySmoothstep(double pgeyEdge0, double pgeyEdge1, double pgeyX) {
        double pgeyT = pgeyClamp((pgeyX - pgeyEdge0) / (pgeyEdge1 - pgeyEdge0), 0, 1);
        return pgeyT * pgeyT * (3 - 2 * pgeyT);
    }
}
