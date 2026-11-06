package com.sportforce3d.three.human;

import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.ModelData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeMap;

class TmpMediasDebugTest {
    @Test
    void dump() throws Exception {
        HumanModel human = OutfitFitterTest.geometryOnly(Persona.HOMBRE);
        var piece = SimulatedGarments.read(Persona.HOMBRE, Garment.SOCKS);
        TriangleIndex.Query skin = TriangleIndex.of(List.of(OutfitFitter.skinOf(human).mesh()), 0.02).query();
        TreeMap<Long, Integer> hist = new TreeMap<>();
        TreeMap<Long, Double> prof = new TreeMap<>();
        for (List<ModelData.Part> parts : piece.zones().values()) {
            for (ModelData.Part part : parts) {
                float[] pts = part.mesh().points();
                for (int i = 0; i < pts.length; i += 3) {
                    Vec3 q = new Vec3(pts[i], -pts[i + 1], -pts[i + 2]);
                    TriangleIndex.Hit hit = skin.nearest(q, 0.12);
                    double d = hit == null ? 1 : q.sub(hit.point()).dot(hit.normal());
                    if (d <= -0.0005) {
                        long b = Math.round(q.y() * 100);
                        hist.merge(b, 1, Integer::sum);
                        prof.merge(b, d, Math::min);
                    }
                }
            }
        }
        System.out.println("DEBUG alturas(cm)=" + hist + " prof=" + prof);
    }
}
