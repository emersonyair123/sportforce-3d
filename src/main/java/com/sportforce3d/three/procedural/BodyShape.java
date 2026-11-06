package com.sportforce3d.three.procedural;

import com.sportforce3d.three.geom.Curve;
import com.sportforce3d.three.geom.Tube;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.model.BodyType;

import java.util.List;

import static com.sportforce3d.three.geom.Curve.angularBell;
import static com.sportforce3d.three.geom.Curve.bell;

/**
 * Anatomía del maniquí en "espacio de diseño": metros de un hombre de 1.80 m, Y hacia arriba, +Z al frente,
 * +X al lado izquierdo del maniquí (a la derecha de la pantalla en vista frontal). Las prendas se generan
 * sobre estas mismas funciones, así que siempre calzan con el cuerpo. {@link #out(Vec3)} aplica la escala
 * del tipo de cuerpo (mujer, juvenil) al emitir los puntos.
 *
 * <p>Torso: en cada altura, una superelipse polar con semiejes distintos al frente y atrás, más "lóbulos"
 * gaussianos (pectorales, busto, omóplatos, glúteos, surco de la columna). Los perfiles se interpolan con
 * curvas monótonas para que hombros, cintura y cadera queden suaves y sin ondulaciones.
 */
public final class BodyShape {

    /** Lado del cuerpo: el izquierdo del maniquí es +X. */
    public enum Side {
        RIGHT(-1), LEFT(1);

        public final int sign;

        Side(int sign) {
            this.sign = sign;
        }
    }

    public static final double TORSO_BOTTOM = 0.775;
    public static final double TORSO_TOP = 1.545;

    private final BodyType type;
    private final double scale;
    private final double girth;
    private final Curve w;
    private final Curve df;
    private final Curve db;
    private final Curve cz;
    private final Curve n;
    private final double pecs;
    private final double bust;
    private final double glutes;
    private final Tube[] arms = new Tube[2];
    private final Tube[] legs = new Tube[2];
    private final Tube neck;
    private final double headScale;

    private BodyShape(BodyType type) {
        this.type = type;
        boolean female = type == BodyType.ADULTO_MUJER;
        boolean junior = type == BodyType.JUVENIL;
        this.scale = female ? 0.955 : junior ? 0.83 : 1.0;
        this.girth = junior ? 0.90 : 1.0;
        this.headScale = junior ? 1.10 : female ? 0.97 : 1.0;

        if (female) {
            w = Curve.of(0.775, .020, 0.80, .100, 0.86, .168, 0.92, .182, 0.97, .176, 1.03, .150, 1.10, .127,
                    1.18, .131, 1.27, .147, 1.34, .157, 1.395, .168, 1.425, .178, 1.455, .158, 1.49, .108,
                    1.515, .074, 1.545, .045);
            df = Curve.of(0.775, .010, 0.80, .052, 0.86, .088, 0.92, .095, 0.97, .093, 1.03, .086, 1.10, .080,
                    1.18, .085, 1.27, .095, 1.34, .099, 1.395, .094, 1.425, .086, 1.455, .076, 1.49, .064,
                    1.515, .056, 1.545, .044);
            db = Curve.of(0.775, .010, 0.80, .062, 0.86, .112, 0.92, .122, 0.97, .110, 1.03, .088, 1.10, .078,
                    1.18, .080, 1.27, .086, 1.34, .090, 1.395, .090, 1.425, .086, 1.455, .076, 1.49, .064,
                    1.515, .058, 1.545, .046);
            pecs = 0;
            bust = 0.033;
            glutes = 0.022;
        } else {
            w = Curve.of(0.775, .020, 0.80, .095, 0.86, .160, 0.92, .171, 0.97, .168, 1.03, .150, 1.10, .139,
                    1.20, .149, 1.29, .163, 1.36, .174, 1.405, .190, 1.435, .196, 1.465, .172, 1.495, .118,
                    1.52, .078, 1.545, .050);
            df = Curve.of(0.775, .010, 0.80, .050, 0.86, .086, 0.92, .094, 0.97, .094, 1.03, .090, 1.10, .088,
                    1.20, .097, 1.29, .108, 1.36, .112, 1.405, .104, 1.435, .094, 1.465, .082, 1.495, .068,
                    1.52, .060, 1.545, .049);
            db = Curve.of(0.775, .010, 0.80, .060, 0.86, .104, 0.92, .114, 0.97, .106, 1.03, .090, 1.10, .084,
                    1.20, .088, 1.29, .094, 1.36, .098, 1.405, .098, 1.435, .092, 1.465, .082, 1.495, .068,
                    1.52, .062, 1.545, .051);
            pecs = junior ? 0.004 : 0.009;
            bust = 0;
            glutes = junior ? 0.012 : 0.016;
        }
        cz = Curve.of(0.80, .000, 0.86, -.004, 0.92, -.008, 0.97, -.004, 1.03, .002, 1.10, .005, 1.20, .007,
                1.29, .007, 1.36, .004, 1.405, .000, 1.435, -.004, 1.465, -.008, 1.495, -.010, 1.545, -.008);
        n = Curve.of(0.80, 2.2, 0.86, 2.5, 0.92, 2.6, 1.03, 2.5, 1.10, 2.4, 1.20, 2.5, 1.29, 2.7, 1.36, 2.9,
                1.435, 2.7, 1.465, 2.4, 1.495, 2.2, 1.52, 2.1, 1.545, 2.0);

        double armK = female ? 0.86 : junior ? 0.92 : 1.0;
        double shoulderX = female ? 0.140 : 0.152;
        for (Side side : Side.values()) {
            int s = side.sign;
            arms[side.ordinal()] = new Tube(List.of(
                    new Vec3(s * shoulderX, 1.418, -0.006),
                    new Vec3(s * (shoulderX + 0.036), 1.362, -0.008),
                    new Vec3(s * (shoulderX + 0.060), 1.240, -0.010),
                    new Vec3(s * (shoulderX + 0.078), 1.115, -0.012),
                    new Vec3(s * (shoulderX + 0.095), 0.990, 0.004),
                    new Vec3(s * (shoulderX + 0.112), 0.872, 0.026),
                    new Vec3(s * (shoulderX + 0.120), 0.800, 0.034),
                    new Vec3(s * (shoulderX + 0.123), 0.715, 0.038)),
                    Curve.of(0, .030, .05, .047, .10, .053, .17, .049, .27, .044, .37, .039, .42, .035,
                            .47, .037, .55, .040, .65, .033, .745, .021, .775, .018, .82, .016, .89, .015,
                            .94, .014, .975, .012, .99, .008, 1.0, .002).scaled(armK),
                    Curve.of(0, .030, .05, .049, .10, .054, .17, .050, .27, .047, .37, .041, .42, .038,
                            .47, .041, .55, .043, .65, .034, .745, .027, .775, .031, .82, .039, .89, .041,
                            .94, .038, .975, .031, .99, .019, 1.0, .004).scaled(armK),
                    Curve.of(0, 0, .27, .004, .55, .002, 1, 0),
                    new Vec3(s, 0, 0));

            double thighK = female ? 1.05 : 1.0;
            double calfK = female ? 0.93 : 1.0;
            legs[side.ordinal()] = new Tube(List.of(
                    new Vec3(s * 0.088, 0.945, 0.000),
                    new Vec3(s * 0.093, 0.720, 0.010),
                    new Vec3(s * 0.097, 0.510, 0.010),
                    new Vec3(s * 0.101, 0.300, -0.004),
                    // La pierna termina dentro de la abertura de la zapatilla (no la atraviesa).
                    new Vec3(s * 0.104, 0.094, -0.012)),
                    Curve.of(0, .060 * thighK, .06, .086 * thighK, .14, .082 * thighK, .25, .074 * thighK,
                            .36, .062, .44, .052, .485, .048, .53, .050, .60, .053 * calfK, .68, .047 * calfK,
                            .78, .036, .88, .031, .96, .033, 1.0, .030),
                    Curve.of(0, .062 * thighK, .06, .090 * thighK, .14, .087 * thighK, .25, .080 * thighK,
                            .36, .067, .44, .056, .485, .052, .53, .055, .60, .060 * calfK, .68, .054 * calfK,
                            .78, .040, .88, .034, .96, .040, 1.0, .040),
                    Curve.of(0, 0, .25, .008, .485, .006, .60, -.014, .78, -.004, 1, 0),
                    new Vec3(s, 0, 0));
        }
        neck = new Tube(List.of(new Vec3(0, 1.470, -0.016), new Vec3(0, 1.560, -0.010), new Vec3(0, 1.665, 0.004)),
                Curve.of(0, .056, .6, .054, 1, .051).scaled(female ? 0.9 : 1.0),
                Curve.of(0, .060, .6, .058, 1, .054).scaled(female ? 0.9 : 1.0),
                Curve.of(0, 0, 1, 0), new Vec3(1, 0, 0));
    }

    public static BodyShape of(BodyType type) {
        return new BodyShape(type);
    }

    public BodyType type() {
        return type;
    }

    /** Altura total aproximada del maniquí ya escalado (m). */
    public double height() {
        return 1.812 * scale;
    }

    // ---------------------------------------------------------------- Escala de salida

    /** Diseño → modelo: aplica la escala del tipo de cuerpo. */
    public Vec3 out(Vec3 p) {
        return new Vec3(p.x() * scale * girth, p.y() * scale, p.z() * scale * girth);
    }

    /** La cabeza escala distinto (los juveniles tienen la cabeza proporcionalmente más grande). */
    public Vec3 outHead(Vec3 p) {
        double pivot = 1.62;
        Vec3 local = new Vec3(p.x() * headScale, pivot + (p.y() - pivot) * headScale, p.z() * headScale);
        return new Vec3(local.x() * scale, local.y() * scale, local.z() * scale);
    }

    // ---------------------------------------------------------------- Torso

    public double torsoCenterZ(double y) {
        return cz.at(y);
    }

    /** Radio del torso en el ángulo polar {@code phi} (0 = frente, π/2 = izquierda +X) y altura {@code y}. */
    public double torsoRadius(double phi, double y) {
        double a = w.at(y);
        double front = df.at(y);
        double back = db.at(y);
        double e = n.at(y);
        double s = Math.abs(Math.sin(phi));
        double c = Math.cos(phi);
        double d = c >= 0 ? front : back;
        double r = 1.0 / Math.pow(Math.pow(s / a, e) + Math.pow(Math.abs(c) / d, e), 1.0 / e);
        return r + lobes(phi, y);
    }

    private double lobes(double phi, double y) {
        double r = 0;
        if (pecs > 0) {
            r += pecs * (angularBell(phi, 0.42, 0.32) + angularBell(phi, -0.42, 0.32)) * bell(y, 1.325, 0.05);
        }
        if (bust > 0) {
            r += bust * (angularBell(phi, 0.43, 0.30) + angularBell(phi, -0.43, 0.30)) * bell(y, 1.30, 0.042);
        }
        r += 0.004 * angularBell(phi, 0, 0.6) * bell(y, 1.07, 0.06);
        r += 0.007 * (angularBell(phi, Math.PI + 0.62, 0.33) + angularBell(phi, Math.PI - 0.62, 0.33)) * bell(y, 1.37, 0.06);
        r -= 0.005 * angularBell(phi, Math.PI, 0.13) * bell(y, 1.22, 0.15);
        r += glutes * (angularBell(phi, Math.PI + 0.48, 0.40) + angularBell(phi, Math.PI - 0.48, 0.40)) * bell(y, 0.925, 0.05);
        return r;
    }

    /** Punto del torso (espacio de diseño) con {@code extraRadius} radial. */
    public Vec3 torsoPoint(double phi, double y, double extraRadius) {
        double r = torsoRadius(phi, y) + extraRadius;
        return new Vec3(r * Math.sin(phi), y, cz.at(y) + r * Math.cos(phi));
    }

    /** Eje del torso a la altura y (para las pistas de normal hacia afuera). */
    public Vec3 torsoAxis(double y) {
        return new Vec3(0, y, cz.at(y));
    }

    // ---------------------------------------------------------------- Extremidades

    public Tube arm(Side side) {
        return arms[side.ordinal()];
    }

    public Tube leg(Side side) {
        return legs[side.ordinal()];
    }

    public Tube neck() {
        return neck;
    }

    /** Fracción de la pierna a la altura de diseño {@code y} (búsqueda sobre la espina). */
    public double legFractionAt(Side side, double y) {
        Tube leg = leg(side);
        double lo = 0;
        double hi = 1;
        for (int i = 0; i < 40; i++) {
            double mid = (lo + hi) / 2;
            if (leg.spine(mid).y() > y) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    /** Centro del tobillo (espacio de diseño), donde se apoya la zapatilla. */
    public Vec3 ankle(Side side) {
        Vec3 c = leg(side).spine(1.0);
        return new Vec3(c.x(), 0.085, c.z());
    }

    // ---------------------------------------------------------------- Cabeza

    /** Perfil de la cabeza (sin rostro, estilo maniquí): radios por altura. */
    public static final Curve HEAD_RX = Curve.of(1.600, .010, 1.615, .034, 1.640, .055, 1.670, .067, 1.705, .075,
            1.740, .077, 1.770, .071, 1.792, .057, 1.806, .035, 1.814, .004);
    public static final Curve HEAD_RZ_FRONT = Curve.of(1.600, .012, 1.615, .040, 1.640, .072, 1.670, .090,
            1.705, .097, 1.740, .098, 1.770, .091, 1.792, .073, 1.806, .044, 1.814, .004);
    public static final Curve HEAD_RZ_BACK = Curve.of(1.600, .008, 1.615, .020, 1.640, .046, 1.670, .076,
            1.705, .094, 1.740, .099, 1.770, .093, 1.792, .075, 1.806, .045, 1.814, .004);
    public static final Curve HEAD_CZ = Curve.of(1.600, .036, 1.640, .026, 1.670, .018, 1.705, .012,
            1.740, .008, 1.770, .004, 1.814, .000);
}
