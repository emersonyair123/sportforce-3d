package com.sportforce3d.three.procedural;

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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class ProceduralModelProvider implements ModelProvider {

    private static final Logger log = LoggerFactory.getLogger(ProceduralModelProvider.class);

    public static final double SHORTS_CROTCH = GarmentGenerator.CROTCH;
    public static final double SHORTS_HIP_FREEZE = GarmentGenerator.HIP_FREEZE;
    public static final double JERSEY_DRAPE_TOP = GarmentGenerator.DRAPE_TOP;
    public static final double JERSEY_DRAPE_BOTTOM = GarmentGenerator.DRAPE_BOTTOM;
    public static final double ARMPIT = GarmentGenerator.ARMPIT;

    @Override
    public String name() {
        return "Procedural";
    }

    @Override
    public ModelData load(BodyType type) {
        long start = System.nanoTime();
        BodyShape b = BodyShape.of(type);

        CompletableFuture<List<Part>> mannequin = async(() -> MannequinGenerator.build(b));
        Map<Zone, CompletableFuture<MeshData>> jobs = new EnumMap<>(Zone.class);
        jobs.put(Zone.JERSEY_BODY, async(() -> GarmentGenerator.jerseyBody(b)));
        jobs.put(Zone.JERSEY_SLEEVES, async(() -> GarmentGenerator.sleeves(b)));
        jobs.put(Zone.JERSEY_COLLAR, async(() -> GarmentGenerator.collar(b)));
        jobs.put(Zone.JERSEY_CUFFS, async(() -> GarmentGenerator.cuffs(b)));
        jobs.put(Zone.JERSEY_SIDES, async(() -> GarmentGenerator.sides(b)));
        jobs.put(Zone.SHORTS_BASE, async(() -> GarmentGenerator.shorts(b)));
        jobs.put(Zone.SHORTS_WAIST, async(() -> GarmentGenerator.waistband(b)));
        jobs.put(Zone.SHORTS_STRIPE, async(() -> GarmentGenerator.stripes(b)));
        jobs.put(Zone.SOCKS_LEG, async(() -> GarmentGenerator.socks(b)));
        jobs.put(Zone.SOCKS_BAND, async(() -> GarmentGenerator.sockBands(b)));
        CompletableFuture<Map<Zone, MeshData>> shoes = async(() -> ShoeGenerator.build(b));

        Map<Zone, List<Part>> zones = new EnumMap<>(Zone.class);
        jobs.forEach((zone, job) -> zones.put(zone, List.of(new Part(zone.meshName(), job.join()))));
        shoes.join().forEach((zone, mesh) -> zones.put(zone, List.of(new Part(zone.meshName(), mesh))));

        Map<Garment, Focus> focus = new EnumMap<>(Garment.class);
        focus.put(Garment.JERSEY, new Focus(b.out(new Vec3(0, 1.26, 0)), 1.85, 4));
        focus.put(Garment.SHORTS, new Focus(b.out(new Vec3(0, 0.80, 0)), 1.50, 3));
        focus.put(Garment.SOCKS, new Focus(b.out(new Vec3(0, 0.30, 0)), 1.40, 7));
        focus.put(Garment.SHOES, new Focus(b.out(new Vec3(0, 0.07, 0.05)), 0.95, 18));
        Focus full = new Focus(b.out(new Vec3(0, 0.93, 0)), 4.0 * b.height() / 1.812, 5);
        for (Garment g : Garment.values()) {
            focus.putIfAbsent(g, full);
        }

        ModelData data = new ModelData(type, mannequin.join(), zones, focus, b.height());
        log.info("Modelo procedural {} generado en {} ms ({} triángulos)", type,
                (System.nanoTime() - start) / 1_000_000, data.totalFaces());
        return data;
    }

    private static <T> CompletableFuture<T> async(Supplier<T> work) {
        return CompletableFuture.supplyAsync(work);
    }
}
