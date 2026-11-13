import math

import numpy as np

from cuerpo import PgeyTablaPolar, pgeyHull2d, pgeyRadioPolar
from malla import PgeyMalla, pgeyIsla, pgeySmoothstep

PGEY_ZONAS = ["shorts_base", "shorts_stripe", "shorts_waist"]
PGEY_AREA_H = 0.35


class PgeyCorte:
    pgeyArista = 0.012
    pgeyCintura = 0.056
    pgeyLargoEntrepierna = 0.21
    pgeyHolguraCintura = 0.008
    pgeyHolguraCadera = 0.016
    pgeyHolguraPierna = 0.020
    pgeyHolguraBasta = 0.032
    pgeyCaida = 0.85
    pgeyPretina = 0.04
    pgeyFranjaColumnas = 2
    pgeyLargoRef = None
    pgeyVTope = 0.12


def pgeyConstruir(pgeyC, pgeyCorte=PgeyCorte):
    pgeyK = pgeyC.pgeyAlto / 1.86
    pgeyE = pgeyCorte.pgeyArista
    pgeyYW = pgeyC.pgeyEntrepierna + pgeyCorte.pgeyCintura * pgeyC.pgeyAlto
    pgeyYCr = pgeyC.pgeyEntrepierna + 0.004
    pgeyYH = pgeyC.pgeyEntrepierna - pgeyCorte.pgeyLargoEntrepierna * pgeyK
    pgeyPelvis = PgeyTablaPolar(pgeyC, pgeyYCr - 0.004, pgeyYW + 0.01)

    pgeyYHip = (pgeyYW + pgeyYCr) / 2
    pgeyDensos = np.radians(np.arange(0.0, 360.25, 0.25))
    pgeyRr = pgeyPelvis.pgeyRadio(np.full(len(pgeyDensos), pgeyYHip), pgeyDensos)
    pgeyPts = np.stack((pgeyRr * np.sin(pgeyDensos), pgeyRr * np.cos(pgeyDensos)), axis=1)
    pgeyArco = np.concatenate(([0.0], np.cumsum(np.linalg.norm(np.diff(pgeyPts, axis=0), axis=1))))

    def pgeyArcoDe(pgeyT):
        return float(np.interp(pgeyT, pgeyDensos, pgeyArco))

    def pgeyThDe(pgeyA):
        return float(np.interp(pgeyA, pgeyArco, pgeyDensos))

    pgeyCuartos = [0.0, math.pi / 2, math.pi]
    pgeyThIzq = []
    for pgeyA, pgeyB in zip(pgeyCuartos, pgeyCuartos[1:]):
        pgeyNq = max(4, round((pgeyArcoDe(pgeyB) - pgeyArcoDe(pgeyA)) / pgeyE))
        pgeyThIzq += [pgeyThDe(pgeyArcoDe(pgeyA) + (pgeyArcoDe(pgeyB) - pgeyArcoDe(pgeyA)) * pgeyI / pgeyNq) for pgeyI in range(pgeyNq)]
    pgeyThIzq.append(math.pi)
    pgeyN = 2 * (len(pgeyThIzq) - 1)
    pgeyTh = np.zeros(pgeyN)
    pgeyTh[: len(pgeyThIzq)] = pgeyThIzq
    for pgeyJ in range(len(pgeyThIzq), pgeyN):
        pgeyTh[pgeyJ] = 2 * math.pi - pgeyTh[pgeyN - pgeyJ]
    pgeyJ90 = pgeyThIzq.index(min(pgeyThIzq, key=lambda pgeyT: abs(pgeyT - math.pi / 2)))
    pgeyMitad = pgeyN // 2

    pgeyRC = max(4, round((pgeyYW - pgeyYCr) / pgeyE))
    pgeyP = {}

    def pgeyRadioCaida(pgeyY, pgeyT):
        pgeyRy = float(pgeyPelvis.pgeyRadio(pgeyY, pgeyT))
        pgeyYs = np.arange(pgeyY, pgeyYW, 0.006)
        pgeyRu = float(max(pgeyPelvis.pgeyRadio(pgeyYs, np.full(len(pgeyYs), pgeyT)))) if len(pgeyYs) else pgeyRy
        return pgeyRy + pgeyCorte.pgeyCaida * max(0.0, pgeyRu - pgeyRy)

    for pgeyR in range(pgeyRC + 1):
        pgeyY = pgeyYW - (pgeyYW - pgeyYCr) * pgeyR / pgeyRC
        pgeyHol = pgeyCorte.pgeyHolguraCintura + (pgeyCorte.pgeyHolguraCadera - pgeyCorte.pgeyHolguraCintura) * pgeySmoothstep(pgeyYW, pgeyYW - 0.05, pgeyY)
        pgeyZc = float(pgeyPelvis.pgeyCentroZ(pgeyY))
        for pgeyJ in range(pgeyN):
            pgeyRad = pgeyRadioCaida(pgeyY, pgeyTh[pgeyJ]) + pgeyHol
            pgeyP[(pgeyR, pgeyJ)] = np.array((math.sin(pgeyTh[pgeyJ]) * pgeyRad, pgeyY, pgeyZc + math.cos(pgeyTh[pgeyJ]) * pgeyRad))

    pgeyM = PgeyMalla(PGEY_ZONAS)
    pgeyIds = {pgeyKey: pgeyM.pgeyVert(pgeyQ) for pgeyKey, pgeyQ in pgeyP.items()}

    pgeyF_, pgeyB_ = pgeyP[(pgeyRC, 0)], pgeyP[(pgeyRC, pgeyMitad)]
    pgeyNS = max(3, round(np.linalg.norm(pgeyF_ - pgeyB_) / pgeyE) - 1)
    pgeyCostura = []
    for pgeyI in range(1, pgeyNS + 1):
        pgeyT = pgeyI / (pgeyNS + 1)
        pgeyQ = pgeyB_ + (pgeyF_ - pgeyB_) * pgeyT
        pgeyQ[1] -= 0.012 * math.sin(math.pi * pgeyT)
        pgeyCostura.append(pgeyM.pgeyVert(pgeyQ))
    pgeyM.pgeyEmpujarAfuera(pgeyC, 0.006, pgeyFijos=set(range(len(pgeyM.pgeyV))) - set(pgeyCostura))

    pgeyNL = max(6, round((pgeyYCr - pgeyYH) / pgeyE))
    pgeyPiernas = {}
    for pgeyLado in (1, -1):
        pgeyCols = list(range(0, pgeyMitad + 1)) if pgeyLado > 0 else [0] + list(range(pgeyN - 1, pgeyMitad - 1, -1))
        pgeyAnillo0 = [pgeyIds[(pgeyRC, pgeyJ)] for pgeyJ in pgeyCols] + pgeyCostura
        pgeyD_ = [pgeyM.pgeyV[pgeyI].copy() for pgeyI in pgeyAnillo0]
        pgeyLargoD = np.concatenate(([0.0], np.cumsum([np.linalg.norm(pgeyD_[(pgeyI + 1) % len(pgeyD_)] - pgeyD_[pgeyI])
                                                     for pgeyI in range(len(pgeyD_))])))
        pgeyQM = pgeyLargoD[:-1] / pgeyLargoD[-1]
        pgeyFilas = [pgeyAnillo0]
        for pgeyR in range(1, pgeyNL + 1):
            pgeyY = pgeyYCr - (pgeyYCr - pgeyYH) * pgeyR / pgeyNL
            pgeyW = float(pgeySmoothstep(0.0, 0.07, pgeyYCr - pgeyY))
            pgeyAnillo = _pgeyAnilloPierna(pgeyC, pgeyLado, pgeyY, pgeyYCr, pgeyQM, pgeyD_[0], pgeyCorte, pgeySmoothstep(pgeyYCr - 0.03, pgeyYH, pgeyY))
            pgeyFila = []
            for pgeyI, pgeyQ in enumerate(pgeyD_):
                pgeyD = np.array((pgeyQ[0], pgeyY, pgeyQ[2]))
                pgeyFila.append(pgeyM.pgeyVert(pgeyD * (1 - pgeyW) + pgeyAnillo[pgeyI] * pgeyW))
            pgeyFilas.append(pgeyFila)
        pgeyPiernas[pgeyLado] = (pgeyCols, pgeyFilas)

    pgeyMLat = pgeyJ90
    pgeyMIn = pgeyMitad + 1 + pgeyNS // 2
    pgeyPelvisPierna = {1: list(range(0, pgeyMitad + 1)), -1: [0] + list(range(pgeyN - 1, pgeyMitad - 1, -1))}

    def pgeyArcos(pgeyFila):
        pgeyPos = [pgeyM.pgeyV[pgeyI] for pgeyI in pgeyFila]
        pgeyL = len(pgeyPos)
        pgeyCerrado = pgeyL > pgeyMitad + 1

        def pgeyRecorre(pgeyDesde, pgeyPaso, pgeyHasta):
            pgeyOut, pgeyAcc, pgeyI = {pgeyDesde: 0.0}, 0.0, pgeyDesde
            while pgeyI != pgeyHasta:
                pgeyI2 = (pgeyI + pgeyPaso) % pgeyL
                pgeyAcc += float(np.linalg.norm(pgeyPos[pgeyI2] - pgeyPos[pgeyI]))
                pgeyOut[pgeyI2] = pgeyAcc
                pgeyI = pgeyI2
            return pgeyOut
        pgeyFr = pgeyRecorre(0, +1, pgeyMLat)
        pgeyEs = pgeyRecorre(pgeyMitad, -1, pgeyMLat)
        if pgeyCerrado:
            pgeyFr.update({pgeyI: -pgeyA for pgeyI, pgeyA in pgeyRecorre(0, -1, pgeyMIn).items() if pgeyI != 0})
            pgeyEs.update({pgeyI: -pgeyA for pgeyI, pgeyA in pgeyRecorre(pgeyMitad, +1, pgeyMIn).items() if pgeyI != pgeyMitad})
        return pgeyFr, pgeyEs

    pgeyVEsc = (pgeyYW - pgeyYH) / (0.97 * PGEY_AREA_H)
    if pgeyCorte.pgeyLargoRef is not None:
        pgeyPasoV = (pgeyYW - (pgeyC.pgeyEntrepierna - pgeyCorte.pgeyLargoRef * pgeyK)) / 0.97
        pgeyVCint = 0.97 - (pgeyYW - pgeyYH) / pgeyPasoV

    def pgeyVDe(pgeyY):
        if pgeyCorte.pgeyLargoRef is None:
            return (pgeyYW - pgeyY) / (PGEY_AREA_H * pgeyVEsc)
        pgeyV = 0.97 - (pgeyY - pgeyYH) / pgeyPasoV
        if pgeyVCint < 0 and pgeyV < pgeyCorte.pgeyVTope:
            pgeyV = pgeyCorte.pgeyVTope * (pgeyV - pgeyVCint) / (pgeyCorte.pgeyVTope - pgeyVCint)
        return pgeyV

    def pgeyUvBase(pgeyLado, pgeyFilaIds, pgeyI, pgeyDelante, pgeyFr, pgeyEs):
        pgeyY = pgeyM.pgeyV[pgeyFilaIds[pgeyI]][1]
        pgeyV = pgeyVDe(pgeyY)
        pgeyNom = ("short_izq_" if pgeyLado > 0 else "short_der_") + ("frente" if pgeyDelante else "espalda")
        if pgeyDelante:
            pgeyS = pgeyFr[pgeyI]
            pgeyLim = pgeyFr[pgeyMLat] if pgeyS >= 0 else -min(pgeyFr.values())
            return pgeyIsla(pgeyNom, 0.5 + pgeyLado * 0.5 * pgeyS / max(pgeyLim, 1e-6), pgeyV)
        pgeyS = pgeyEs[pgeyI]
        pgeyLim = pgeyEs[pgeyMLat] if pgeyS >= 0 else -min(pgeyEs.values())
        return pgeyIsla(pgeyNom, 0.5 - pgeyLado * 0.5 * pgeyS / max(pgeyLim, 1e-6), pgeyV)

    pgeyRW = max(2, round(pgeyCorte.pgeyPretina / pgeyE))
    pgeyFc = pgeyCorte.pgeyFranjaColumnas
    for pgeyLado in (1, -1):
        pgeyCols, pgeyFilas = pgeyPiernas[pgeyLado]
        pgeyTodas = [[pgeyIds[(pgeyR, pgeyJ)] for pgeyJ in pgeyPelvisPierna[pgeyLado]] for pgeyR in range(pgeyRC)] + pgeyFilas
        pgeyInfo = [pgeyArcos(pgeyF) for pgeyF in pgeyTodas]
        for pgeyR in range(len(pgeyTodas) - 1):
            pgeyA, pgeyB = pgeyTodas[pgeyR], pgeyTodas[pgeyR + 1]
            pgeyCerrado = len(pgeyA) == len(pgeyB) and len(pgeyA) > pgeyMitad + 1
            for pgeyI in range(len(pgeyA) if pgeyCerrado else pgeyMitad):
                pgeyI2 = (pgeyI + 1) % len(pgeyA)
                if pgeyLado > 0:
                    pgeyVs = [(pgeyR, pgeyI), (pgeyR + 1, pgeyI), (pgeyR + 1, pgeyI2), (pgeyR, pgeyI2)]
                else:
                    pgeyVs = [(pgeyR, pgeyI), (pgeyR, pgeyI2), (pgeyR + 1, pgeyI2), (pgeyR + 1, pgeyI)]
                pgeyYm = (pgeyM.pgeyV[pgeyA[pgeyI]][1] + pgeyM.pgeyV[pgeyA[pgeyI2]][1]) / 2
                pgeyIm = pgeyI + 0.5
                if pgeyR < pgeyRW:
                    pgeyZona = "shorts_waist"
                elif pgeyMLat - pgeyFc <= pgeyI < pgeyMLat + pgeyFc:
                    pgeyZona = "shorts_stripe"
                else:
                    pgeyZona = "shorts_base"
                pgeyDelante = pgeyIm < pgeyMLat or pgeyIm > pgeyMIn
                pgeyUv = []
                for pgeyRr_, pgeyIi in pgeyVs:
                    pgeyFilaIds = pgeyTodas[pgeyRr_]
                    if pgeyZona == "shorts_waist":
                        pgeyJ = pgeyPelvisPierna[pgeyLado][pgeyIi]
                        pgeyU = 1.0 if (pgeyLado < 0 and pgeyJ == 0) else pgeyJ / pgeyN
                        pgeyUv.append(pgeyIsla("pretina", pgeyU, (pgeyRW - pgeyRr_) / pgeyRW * 0.8))
                    elif pgeyZona == "shorts_stripe":
                        pgeyY = pgeyM.pgeyV[pgeyFilaIds[pgeyIi]][1]
                        pgeyUv.append(pgeyIsla("franja_izq" if pgeyLado > 0 else "franja_der",
                                       (pgeyYW - pgeyY) / (pgeyYW - pgeyYH), (pgeyIi - (pgeyMLat - pgeyFc)) / (2 * pgeyFc)))
                    else:
                        pgeyUv.append(pgeyUvBase(pgeyLado, pgeyFilaIds, pgeyIi, pgeyDelante, *pgeyInfo[pgeyRr_]))
                pgeyM.pgeyCara([pgeyTodas[pgeyRr_][pgeyIi] for pgeyRr_, pgeyIi in pgeyVs], pgeyUv, pgeyZona)
        for pgeyR, pgeyFila in enumerate(pgeyTodas):
            for pgeyI in pgeyFila:
                if pgeyR <= 2:
                    pgeyM.pgeyPeso("fijar", pgeyI, 1.0)
                elif pgeyR <= 5:
                    pgeyM.pgeyPeso("fijar", pgeyI, (5 - pgeyR) / 3)
                if pgeyR <= 1 or pgeyR >= len(pgeyTodas) - 2:
                    pgeyM.pgeyPeso("doble", pgeyI, 1.0)

    pgeyDatos = {"cintura": [pgeyIds[(0, pgeyJ)] for pgeyJ in range(pgeyN)], "columnas": pgeyN, "filas_pelvis": pgeyRC,
             "costura": pgeyCostura, "bastas": {pgeyLado: pgeyPiernas[pgeyLado][1][-1] for pgeyLado in (1, -1)}}
    return pgeyM, pgeyDatos


def _pgeyAnilloPierna(pgeyC, pgeyLado, pgeyY, pgeyYCr, pgeyQM, pgeyF, pgeyCorte, pgeyTBasta):
    pgeyPuntos = []
    for pgeyYy in np.arange(pgeyY, pgeyYCr - 0.02, 0.01):
        pgeyP = pgeyC.pgeyPiernaCorte(pgeyYy, pgeyLado)
        if len(pgeyP) >= 3:
            pgeyPuntos.append(pgeyHull2d(pgeyP))
    pgeyP = pgeyC.pgeyPiernaCorte(pgeyY, pgeyLado)
    pgeyActual = pgeyHull2d(pgeyP) if len(pgeyP) >= 3 else pgeyPuntos[0]
    pgeyUnion = pgeyHull2d(np.concatenate(pgeyPuntos)) if pgeyPuntos else pgeyActual
    pgeyCentro = np.array(((pgeyActual[:, 0].max() + pgeyActual[:, 0].min()) / 2, (pgeyActual[:, 1].max() + pgeyActual[:, 1].min()) / 2))
    pgeyT0 = math.atan2(pgeyF[0] - pgeyCentro[0], pgeyF[2] - pgeyCentro[1])
    pgeyPaso = 1 if pgeyLado > 0 else -1
    pgeyAngs = pgeyT0 + pgeyPaso * np.radians(np.arange(0.0, 360.25, 0.5))
    pgeyRa = pgeyRadioPolar(pgeyActual, pgeyCentro, pgeyAngs)
    pgeyRu = pgeyRadioPolar(pgeyUnion, pgeyCentro, pgeyAngs)
    pgeyHol = pgeyCorte.pgeyHolguraPierna + (pgeyCorte.pgeyHolguraBasta - pgeyCorte.pgeyHolguraPierna) * pgeyTBasta
    pgeyRad = pgeyRa + pgeyCorte.pgeyCaida * np.maximum(0.0, pgeyRu - pgeyRa) + pgeyHol
    pgeyPts = np.stack((pgeyCentro[0] + pgeyRad * np.sin(pgeyAngs), pgeyCentro[1] + pgeyRad * np.cos(pgeyAngs)), axis=1)
    pgeyLargo = np.concatenate(([0.0], np.cumsum(np.linalg.norm(np.diff(pgeyPts, axis=0), axis=1))))
    pgeyOut = []
    for pgeyQ in pgeyQM:
        pgeyA = pgeyQ * pgeyLargo[-1]
        pgeyX = float(np.interp(pgeyA, pgeyLargo, pgeyPts[:, 0]))
        pgeyZ = float(np.interp(pgeyA, pgeyLargo, pgeyPts[:, 1]))
        pgeyOut.append(np.array((pgeyX, pgeyY, pgeyZ)))
    return pgeyOut
