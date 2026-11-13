package com.sportforce3d.render;

public record PgeyMask(int pgeyX, int pgeyY, int pgeyWidth, int pgeyHeight, byte[] pgeyAlpha) {

    public static final PgeyMask PGEY_EMPTY = new PgeyMask(0, 0, 0, 0, new byte[0]);

    public PgeyMask {
        if (pgeyAlpha == null || pgeyAlpha.length != pgeyWidth * pgeyHeight) {
            throw new IllegalArgumentException("Máscara inválida");
        }
    }

    public boolean pgeyIsEmpty() {
        return pgeyWidth == 0 || pgeyHeight == 0;
    }

    public int pgeyAt(int pgeyPx, int pgeyPy) {
        int pgeyLx = pgeyPx - pgeyX;
        int pgeyLy = pgeyPy - pgeyY;
        if (pgeyLx < 0 || pgeyLy < 0 || pgeyLx >= pgeyWidth || pgeyLy >= pgeyHeight) {
            return 0;
        }
        return pgeyAlpha[pgeyLy * pgeyWidth + pgeyLx] & 0xFF;
    }

    public static PgeyMask pgeyFromAlpha(PgeyArgbImage pgeyImg) {
        return pgeyCrop(pgeyImg, true);
    }

    static PgeyMask pgeyCrop(PgeyArgbImage pgeyImg, boolean pgeyAlphaChannel) {
        return pgeyCrop(pgeyImg, pgeyAlphaChannel, pgeyBounds(pgeyImg));
    }

    static int[] pgeyBounds(PgeyArgbImage pgeyImg) {
        int pgeyW = pgeyImg.pgeyWidth();
        int pgeyH = pgeyImg.pgeyHeight();
        int[] pgeyP = pgeyImg.pgeyPixels();
        int pgeyX0 = pgeyW;
        int pgeyY0 = pgeyH;
        int pgeyX1 = -1;
        int pgeyY1 = -1;
        for (int pgeyYy = 0; pgeyYy < pgeyH; pgeyYy++) {
            int pgeyRow = pgeyYy * pgeyW;
            for (int pgeyXx = 0; pgeyXx < pgeyW; pgeyXx++) {
                if ((pgeyP[pgeyRow + pgeyXx] >>> 24) != 0) {
                    if (pgeyXx < pgeyX0) {
                        pgeyX0 = pgeyXx;
                    }
                    if (pgeyXx > pgeyX1) {
                        pgeyX1 = pgeyXx;
                    }
                    if (pgeyYy < pgeyY0) {
                        pgeyY0 = pgeyYy;
                    }
                    pgeyY1 = pgeyYy;
                }
            }
        }
        return pgeyX1 < 0 ? null : new int[]{pgeyX0, pgeyY0, pgeyX1 - pgeyX0 + 1, pgeyY1 - pgeyY0 + 1};
    }

    static PgeyMask pgeyCrop(PgeyArgbImage pgeyImg, boolean pgeyAlphaChannel, int[] pgeyBox) {
        if (pgeyBox == null) {
            return PGEY_EMPTY;
        }
        int pgeyW = pgeyImg.pgeyWidth();
        int[] pgeyP = pgeyImg.pgeyPixels();
        byte[] pgeyOut = new byte[pgeyBox[2] * pgeyBox[3]];
        for (int pgeyYy = 0; pgeyYy < pgeyBox[3]; pgeyYy++) {
            int pgeySrc = (pgeyBox[1] + pgeyYy) * pgeyW + pgeyBox[0];
            int pgeyDst = pgeyYy * pgeyBox[2];
            for (int pgeyXx = 0; pgeyXx < pgeyBox[2]; pgeyXx++) {
                int pgeyC = pgeyP[pgeySrc + pgeyXx];
                int pgeyV = pgeyAlphaChannel ? pgeyC >>> 24 : (((pgeyC >> 16) & 0xFF) + ((pgeyC >> 8) & 0xFF) + (pgeyC & 0xFF)) / 3;
                pgeyOut[pgeyDst + pgeyXx] = (byte) pgeyV;
            }
        }
        return new PgeyMask(pgeyBox[0], pgeyBox[1], pgeyBox[2], pgeyBox[3], pgeyOut);
    }
}
