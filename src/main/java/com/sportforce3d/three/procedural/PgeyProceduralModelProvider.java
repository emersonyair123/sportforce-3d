package com.sportforce3d.three.procedural;

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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class PgeyProceduralModelProvider implements PgeyModelProvider {

    private static final Logger pgeyLog = LoggerFactory.getLogger(PgeyProceduralModelProvider.class);

    public static final double PGEY_SHORTS_CROTCH = PgeyGarmentGenerator.PGEY_CROTCH;
    public static final double PGEY_SHORTS_HIP_FREEZE = PgeyGarmentGenerator.PGEY_HIP_FREEZE;
    public static final double PGEY_JERSEY_DRAPE_TOP = PgeyGarmentGenerator.PGEY_DRAPE_TOP;
    public static final double PGEY_JERSEY_DRAPE_BOTTOM = PgeyGarmentGenerator.PGEY_DRAPE_BOTTOM;
    public static final double PGEY_ARMPIT = PgeyGarmentGenerator.PGEY_ARMPIT;

    @Override
    public String pgeyName() {
        return "Procedural";
    }

    @Override
    public PgeyModelData pgeyLoad(PgeyBodyType pgeyType) {
        long pgeyStart = System.nanoTime();
        PgeyBodyShape pgeyB = PgeyBodyShape.pgeyOf(pgeyType);

        CompletableFuture<List<PgeyPart>> pgeyMannequin = pgeyAsync(() -> PgeyMannequinGenerator.pgeyBuild(pgeyB));
        Map<PgeyZone, CompletableFuture<PgeyMeshData>> pgeyJobs = new EnumMap<>(PgeyZone.class);
        pgeyJobs.put(PgeyZone.JERSEY_BODY, pgeyAsync(() -> PgeyGarmentGenerator.pgeyJerseyBody(pgeyB)));
        pgeyJobs.put(PgeyZone.JERSEY_SLEEVES, pgeyAsync(() -> PgeyGarmentGenerator.pgeySleeves(pgeyB)));
        pgeyJobs.put(PgeyZone.JERSEY_COLLAR, pgeyAsync(() -> PgeyGarmentGenerator.pgeyCollar(pgeyB)));
        pgeyJobs.put(PgeyZone.JERSEY_CUFFS, pgeyAsync(() -> PgeyGarmentGenerator.pgeyCuffs(pgeyB)));
        pgeyJobs.put(PgeyZone.JERSEY_SIDES, pgeyAsync(() -> PgeyGarmentGenerator.pgeySides(pgeyB)));
        pgeyJobs.put(PgeyZone.SHORTS_BASE, pgeyAsync(() -> PgeyGarmentGenerator.pgeyShorts(pgeyB)));
        pgeyJobs.put(PgeyZone.SHORTS_WAIST, pgeyAsync(() -> PgeyGarmentGenerator.pgeyWaistband(pgeyB)));
        pgeyJobs.put(PgeyZone.SHORTS_STRIPE, pgeyAsync(() -> PgeyGarmentGenerator.pgeyStripes(pgeyB)));
        pgeyJobs.put(PgeyZone.SOCKS_LEG, pgeyAsync(() -> PgeyGarmentGenerator.pgeySocks(pgeyB)));
        pgeyJobs.put(PgeyZone.SOCKS_BAND, pgeyAsync(() -> PgeyGarmentGenerator.pgeySockBands(pgeyB)));
        CompletableFuture<Map<PgeyZone, PgeyMeshData>> pgeyShoes = pgeyAsync(() -> PgeyShoeGenerator.pgeyBuild(pgeyB));

        Map<PgeyZone, List<PgeyPart>> pgeyZones = new EnumMap<>(PgeyZone.class);
        pgeyJobs.forEach((pgeyZone, pgeyJob) -> pgeyZones.put(pgeyZone, List.of(new PgeyPart(pgeyZone.pgeyMeshName(), pgeyJob.join()))));
        pgeyShoes.join().forEach((pgeyZone, pgeyMesh) -> pgeyZones.put(pgeyZone, List.of(new PgeyPart(pgeyZone.pgeyMeshName(), pgeyMesh))));

        Map<HepyGarment, PgeyFocus> pgeyFocus = new EnumMap<>(HepyGarment.class);
        pgeyFocus.put(HepyGarment.JERSEY, new PgeyFocus(pgeyB.pgeyOut(new PgeyVec3(0, 1.26, 0)), 1.85, 4));
        pgeyFocus.put(HepyGarment.SHORTS, new PgeyFocus(pgeyB.pgeyOut(new PgeyVec3(0, 0.80, 0)), 1.50, 3));
        pgeyFocus.put(HepyGarment.SOCKS, new PgeyFocus(pgeyB.pgeyOut(new PgeyVec3(0, 0.30, 0)), 1.40, 7));
        pgeyFocus.put(HepyGarment.SHOES, new PgeyFocus(pgeyB.pgeyOut(new PgeyVec3(0, 0.07, 0.05)), 0.95, 18));
        PgeyFocus pgeyFull = new PgeyFocus(pgeyB.pgeyOut(new PgeyVec3(0, 0.93, 0)), 4.0 * pgeyB.pgeyHeight() / 1.812, 5);
        for (HepyGarment pgeyG : HepyGarment.values()) {
            pgeyFocus.putIfAbsent(pgeyG, pgeyFull);
        }

        PgeyModelData pgeyData = new PgeyModelData(pgeyType, pgeyMannequin.join(), pgeyZones, pgeyFocus, pgeyB.pgeyHeight());
        pgeyLog.info("Modelo procedural {} generado en {} ms ({} triángulos)", pgeyType,
                (System.nanoTime() - pgeyStart) / 1_000_000, pgeyData.pgeyTotalFaces());
        return pgeyData;
    }

    private static <T> CompletableFuture<T> pgeyAsync(Supplier<T> pgeyWork) {
        return CompletableFuture.supplyAsync(pgeyWork);
    }
}
