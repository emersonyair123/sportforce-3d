package com.sportforce3d.three.geom;

public record PgeyVec3(double pgeyX, double pgeyY, double pgeyZ) {

    public static final PgeyVec3 PGEY_ZERO = new PgeyVec3(0, 0, 0);
    public static final PgeyVec3 PGEY_UP = new PgeyVec3(0, 1, 0);
    public static final PgeyVec3 PGEY_FRONT = new PgeyVec3(0, 0, 1);

    public PgeyVec3 pgeyAdd(PgeyVec3 pgeyO) {
        return new PgeyVec3(pgeyX + pgeyO.pgeyX, pgeyY + pgeyO.pgeyY, pgeyZ + pgeyO.pgeyZ);
    }

    public PgeyVec3 pgeySub(PgeyVec3 pgeyO) {
        return new PgeyVec3(pgeyX - pgeyO.pgeyX, pgeyY - pgeyO.pgeyY, pgeyZ - pgeyO.pgeyZ);
    }

    public PgeyVec3 pgeyMul(double pgeyK) {
        return new PgeyVec3(pgeyX * pgeyK, pgeyY * pgeyK, pgeyZ * pgeyK);
    }

    public PgeyVec3 pgeyAddScaled(PgeyVec3 pgeyO, double pgeyK) {
        return new PgeyVec3(pgeyX + pgeyO.pgeyX * pgeyK, pgeyY + pgeyO.pgeyY * pgeyK, pgeyZ + pgeyO.pgeyZ * pgeyK);
    }

    public double pgeyDot(PgeyVec3 pgeyO) {
        return pgeyX * pgeyO.pgeyX + pgeyY * pgeyO.pgeyY + pgeyZ * pgeyO.pgeyZ;
    }

    public PgeyVec3 pgeyCross(PgeyVec3 pgeyO) {
        return new PgeyVec3(pgeyY * pgeyO.pgeyZ - pgeyZ * pgeyO.pgeyY, pgeyZ * pgeyO.pgeyX - pgeyX * pgeyO.pgeyZ, pgeyX * pgeyO.pgeyY - pgeyY * pgeyO.pgeyX);
    }

    public double pgeyLength() {
        return Math.sqrt(pgeyX * pgeyX + pgeyY * pgeyY + pgeyZ * pgeyZ);
    }

    public PgeyVec3 pgeyNormalize() {
        double pgeyLen = pgeyLength();
        return pgeyLen < 1e-12 ? PGEY_ZERO : new PgeyVec3(pgeyX / pgeyLen, pgeyY / pgeyLen, pgeyZ / pgeyLen);
    }

    public PgeyVec3 pgeyLerp(PgeyVec3 pgeyO, double pgeyT) {
        return new PgeyVec3(pgeyX + (pgeyO.pgeyX - pgeyX) * pgeyT, pgeyY + (pgeyO.pgeyY - pgeyY) * pgeyT, pgeyZ + (pgeyO.pgeyZ - pgeyZ) * pgeyT);
    }

    public boolean pgeyIsFinite() {
        return Double.isFinite(pgeyX) && Double.isFinite(pgeyY) && Double.isFinite(pgeyZ);
    }
}
