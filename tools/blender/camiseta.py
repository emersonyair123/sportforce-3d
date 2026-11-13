import math

import numpy as np

from cuerpo import PgeyTablaPolar, pgeyHull2d, pgeyRadioPolar
from malla import PgeyMalla, pgeyIsla, pgeySmoothstep

PGEY_ZONAS = ["jersey_body", "jersey_sleeves", "jersey_collar", "jersey_cuffs", "jersey_sides"]
PGEY_AREA_W, PGEY_AREA_H = 0.47, 0.58
PGEY_MANGA_H = 0.17
PGEY_ANTEBRAZO_H = 0.36


class PgeyCorte:
    pgeyArista = 0.012
    pgeyRuedoFrente = 0.034
    pgeyRuedoEspalda = 0.027
    pgeyCaida = 0.80
    pgeyHolguraPecho = 0.010
    pgeyHolguraRuedo = 0.022
    pgeyHolguraHombro = 0.007
    pgeyHolguraEscote = 0.006
    pgeyHolguraManga = 0.011
    pgeyEscote = 0.010
    pgeyClavicula = 0.076
    pgeyVProfundidad = 0.06
    pgeyVMaxEscote = 0.05
    pgeyVAngulo = 26.0
    pgeyLargoManga = 0.16
    pgeyPunoFilas = 2
    pgeyAnclajeManga = (0.7, 0.3)
    pgeyLadoColumnas = 3
    pgeyLadoAngulo = 82.0
    pgeySuavizado = 10
    pgeyRelajadoCanesu = 60
    pgeyRedondeoEscote = 10
    pgeyRedondeoVecinos = 4
    pgeyRibete = 0.008
    pgeyMangaLarga = False
    pgeyLargoMangaLarga = 0.49
    pgeyHolguraPuno = 0.006
    pgeyAnclajeMangaLarga = (0.8, 0.6)
    pgeyHolguraHombroLarga = 0.018


def pgeyConstruir(pgeyC, pgeyCorte=PgeyCorte):
    pgeyK = pgeyC.pgeyAlto / 1.86
    pgeyE = pgeyCorte.pgeyArista
    pgeyH, pgeyS0 = pgeyC.pgeyAlto, pgeyC.pgeySuelo
    pgeyYA = pgeyC.pgeyAxila - 0.004
    pgeyYS = float(pgeyC.pgeyHombro[1][1])
    pgeyYCh = pgeyYA - 0.03
    pgeyHemF = pgeyC.pgeyEntrepierna + pgeyCorte.pgeyRuedoFrente * pgeyH
    pgeyHemB = pgeyC.pgeyEntrepierna + pgeyCorte.pgeyRuedoEspalda * pgeyH
    pgeyXclip = float(np.abs(pgeyC.pgeyTorsoCorte(pgeyYA)[:, 0]).max()) + 0.004
    pgeyMedio = PgeyTablaPolar(pgeyC, pgeyYA - 0.01, pgeyYS + 0.01, pgeyRecorte=pgeyXclip)
    pgeyAlto = PgeyTablaPolar(pgeyC, pgeyYS - 0.16, pgeyC.pgeyBaseCuello + 0.05,
                      pgeyRecorte=lambda pgeyY: None if pgeyY >= pgeyYS + 0.012 else pgeyXclip + max(0.0, pgeyY - pgeyYS) * 6)

    pgeyCache = {}

    def pgeyHullEn(pgeyY):
        pgeyClave = round(pgeyY / 0.004)
        if pgeyClave not in pgeyCache:
            pgeyCache[pgeyClave] = pgeyHull2d(pgeyC.pgeyTorsoCorte(pgeyClave * 0.004))
        return pgeyCache[pgeyClave]

    pgeyUnion = {}

    def pgeyRadioCaida(pgeyY, pgeyTh):
        pgeyClave = round(pgeyY / 0.004)
        if pgeyClave not in pgeyUnion:
            pgeyPts = [pgeyHullEn(pgeyYy) for pgeyYy in np.arange(pgeyClave * 0.004, pgeyYCh + 1e-9, 0.008)] or [pgeyHullEn(pgeyY)]
            pgeyUnion[pgeyClave] = pgeyHull2d(np.concatenate(pgeyPts))
        pgeyCentro = np.array((0.0, pgeyC.pgeyTorsoZ(pgeyY)))
        pgeyRy = pgeyRadioPolar(pgeyHullEn(pgeyY), pgeyCentro, [pgeyTh])[0]
        pgeyRu = pgeyRadioPolar(pgeyUnion[pgeyClave], pgeyCentro, [pgeyTh])[0]
        return pgeyRy + pgeyCorte.pgeyCaida * max(0.0, pgeyRu - pgeyRy)

    pgeyZcCh = pgeyC.pgeyTorsoZ(pgeyYCh)
    pgeyDensos = np.radians(np.arange(0.0, 360.25, 0.25))
    pgeyRr = pgeyRadioPolar(pgeyHullEn(pgeyYCh), np.array((0.0, pgeyZcCh)), pgeyDensos)
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
    pgeyJ270 = pgeyN - pgeyJ90
    pgeyJlIzq = pgeyThIzq.index(min(pgeyThIzq, key=lambda pgeyT: abs(pgeyT - math.radians(pgeyCorte.pgeyLadoAngulo))))
    pgeyJlDer = pgeyN - pgeyJlIzq

    pgeyYM = (pgeyYA + pgeyYS) / 2
    pgeyXPared = max(math.sin(pgeyT) * float(pgeyMedio.pgeyRadio(pgeyYM, pgeyT)) for pgeyT in pgeyTh[: len(pgeyThIzq)])
    pgeyEnPared = [pgeyJ for pgeyJ in range(1, len(pgeyThIzq) - 1)
                if math.sin(pgeyTh[pgeyJ]) * float(pgeyMedio.pgeyRadio(pgeyYM, pgeyTh[pgeyJ])) >= pgeyXPared - 0.004]
    pgeyJ0, pgeyJ1 = min(pgeyEnPared) - 1, max(pgeyEnPared) + 1
    pgeySisas = [(pgeyJ0, pgeyJ1), (pgeyN - pgeyJ1, pgeyN - pgeyJ0)]

    def pgeyEnSisa(pgeyJ):
        return any(pgeyA < pgeyJ < pgeyB for pgeyA, pgeyB in pgeySisas)

    pgeyN1 = max(8, round((pgeyYA - (pgeyHemF + pgeyHemB) / 2) / pgeyE))
    pgeyN2 = max(3, round((pgeyYS - pgeyYA) / pgeyE))
    pgeyRA, pgeyRS = pgeyN1, pgeyN1 + pgeyN2
    pgeyCrudo = {}
    pgeyP = {}

    def pgeyPunto(pgeyT, pgeyY, pgeyZc, pgeyRad):
        return np.array((math.sin(pgeyT) * pgeyRad, pgeyY, pgeyZc + math.cos(pgeyT) * pgeyRad))

    for pgeyJ in range(pgeyN):
        pgeyT = pgeyTh[pgeyJ]
        pgeyHem = pgeyHemB + (pgeyHemF - pgeyHemB) * (1 + math.cos(pgeyT)) / 2
        for pgeyR in range(pgeyN1 + 1):
            pgeyY = pgeyHem + (pgeyYA - pgeyHem) * pgeyR / pgeyN1
            pgeyHol = pgeyCorte.pgeyHolguraPecho + (pgeyCorte.pgeyHolguraRuedo - pgeyCorte.pgeyHolguraPecho) * pgeySmoothstep(pgeyYA - 0.12, pgeyHem, pgeyY)
            pgeyP[(pgeyR, pgeyJ)] = pgeyPunto(pgeyT, pgeyY, pgeyC.pgeyTorsoZ(pgeyY), pgeyRadioCaida(pgeyY, pgeyT) + pgeyHol)
        if pgeyEnSisa(pgeyJ):
            continue
        for pgeyR in range(pgeyN1 + 1, pgeyRS + 1):
            pgeyY = pgeyYA + (pgeyYS - pgeyYA) * (pgeyR - pgeyN1) / pgeyN2
            pgeyZc = float(pgeyMedio.pgeyCentroZ(pgeyY))
            pgeyRad = float(pgeyMedio.pgeyRadio(pgeyY, pgeyT))
            pgeyCrudo[pgeyJ] = pgeyPunto(pgeyT, pgeyY, pgeyZc, pgeyRad)
            pgeyP[(pgeyR, pgeyJ)] = pgeyPunto(pgeyT, pgeyY, pgeyZc, pgeyRad + pgeyCorte.pgeyHolguraPecho)

    pgeyYO = pgeyYS - 0.10
    pgeyO2 = np.array((0.0, pgeyYO, float(pgeyAlto.pgeyCentroZ(pgeyYO))))
    pgeyPc = pgeyC.pgeyTorsoCorte(pgeyC.pgeyBaseCuello + 0.008)
    pgeyZcn = (pgeyPc[:, 1].max() + pgeyPc[:, 1].min()) / 2
    pgeyRxn = (pgeyPc[:, 0].max() - pgeyPc[:, 0].min()) / 2 + pgeyCorte.pgeyEscote
    pgeyRzn = (pgeyPc[:, 1].max() - pgeyPc[:, 1].min()) / 2 + pgeyCorte.pgeyEscote

    def pgeyFueraDelEscote(pgeyQ):
        pgeyRx, pgeyRz = pgeyQ[0], pgeyQ[2] - pgeyZcn
        pgeyA = math.atan2(pgeyRx, pgeyRz)
        return math.hypot(pgeyRx, pgeyRz) - 1.0 / math.sqrt((math.sin(pgeyA) / pgeyRxn) ** 2 + (math.cos(pgeyA) / pgeyRzn) ** 2)

    pgeyCadenas = {}
    for pgeyJ in range(pgeyN):
        pgeyT = pgeyTh[pgeyJ]
        pgeyDh = np.array((math.sin(pgeyT), 0.0, math.cos(pgeyT)))

        def pgeyPlano(pgeyQ):
            return np.array((float(np.dot(pgeyQ - pgeyO2, pgeyDh)), pgeyQ[1]))

        pgeyPuntos = []
        pgeyE0 = 0.0
        if not pgeyEnSisa(pgeyJ):
            pgeyPuntos.append(pgeyCrudo[pgeyJ])
            pgeyE0 = math.atan2(pgeyCrudo[pgeyJ][1] - pgeyO2[1], pgeyPlano(pgeyCrudo[pgeyJ])[0])
        for pgeyEl in np.radians(np.arange(math.degrees(pgeyE0) + 0.5, 89.0, 0.5)):
            pgeyQ = pgeyAlto.pgeySalida(pgeyO2, pgeyDh * math.cos(pgeyEl) + np.array((0.0, math.sin(pgeyEl), 0.0)), 0.45)
            if pgeyQ is None:
                continue
            if not pgeyPuntos and pgeyQ[1] < pgeyYS - 0.003:
                continue
            pgeyF = pgeyFueraDelEscote(pgeyQ)
            if pgeyF <= 0 and pgeyPuntos:
                pgeyFp = pgeyFueraDelEscote(pgeyPuntos[-1])
                pgeyTt = pgeyFp / (pgeyFp - pgeyF) if pgeyFp > pgeyF else 1.0
                pgeyPuntos.append(pgeyPuntos[-1] + (pgeyQ - pgeyPuntos[-1]) * pgeyTt)
                break
            pgeyPuntos.append(pgeyQ)
        pgeyPila = []
        for pgeyQ in (pgeyPlano(pgeyP) for pgeyP in pgeyPuntos):
            while len(pgeyPila) >= 2:
                pgeyA, pgeyB = pgeyPila[-2], pgeyPila[-1]
                if (pgeyB[0] - pgeyA[0]) * (pgeyQ[1] - pgeyA[1]) - (pgeyB[1] - pgeyA[1]) * (pgeyQ[0] - pgeyA[0]) < 0:
                    pgeyPila.pop()
                else:
                    break
            pgeyPila.append(pgeyQ)
        pgeyCadenas[pgeyJ] = np.array(pgeyPila)

    def pgeyLargo(pgeyCad):
        return float(np.linalg.norm(np.diff(pgeyCad, axis=0), axis=1).sum())

    pgeyN3 = max(4, round(np.percentile([pgeyLargo(pgeyCadenas[pgeyJ]) for pgeyJ in range(pgeyN)], 70) / pgeyE))
    pgeyFilas = pgeyN1 + pgeyN2 + pgeyN3 + 1
    for pgeyJ in range(pgeyN):
        pgeyT = pgeyTh[pgeyJ]
        pgeyCad = pgeyCadenas[pgeyJ]
        pgeySeg = np.concatenate(([0.0], np.cumsum(np.linalg.norm(np.diff(pgeyCad, axis=0), axis=1))))
        for pgeyI in range(pgeyN3 + 1):
            pgeyA = pgeySeg[-1] * pgeyI / pgeyN3
            pgeyK2 = int(min(np.searchsorted(pgeySeg, pgeyA, side="right") - 1, len(pgeyCad) - 2))
            pgeyF = (pgeyA - pgeySeg[pgeyK2]) / max(pgeySeg[pgeyK2 + 1] - pgeySeg[pgeyK2], 1e-9)
            pgeyQ = pgeyCad[pgeyK2] + (pgeyCad[pgeyK2 + 1] - pgeyCad[pgeyK2]) * pgeyF
            pgeyTan = (pgeyCad[pgeyK2 + 1] - pgeyCad[pgeyK2]) / max(np.linalg.norm(pgeyCad[pgeyK2 + 1] - pgeyCad[pgeyK2]), 1e-9)
            pgeyFr = pgeyI / pgeyN3
            pgeyHol = (pgeyCorte.pgeyHolguraPecho + (pgeyCorte.pgeyHolguraHombro - pgeyCorte.pgeyHolguraPecho) * pgeySmoothstep(0.0, 0.4, pgeyFr)
                   if pgeyFr < 0.9 else pgeyCorte.pgeyHolguraEscote)
            pgeyQ = pgeyQ + np.array((pgeyTan[1], -pgeyTan[0])) * pgeyHol
            pgeyP[(pgeyRS + pgeyI, pgeyJ)] = np.array((math.sin(pgeyT) * pgeyQ[0], pgeyQ[1], pgeyO2[2] + math.cos(pgeyT) * pgeyQ[0]))

    def pgeyArcoFila(pgeyR, pgeyDesde, pgeyPaso, pgeyHasta):
        pgeyOut = {pgeyDesde: 0.0}
        pgeyAcc, pgeyJ = 0.0, pgeyDesde
        while pgeyJ != pgeyHasta:
            pgeyJn = (pgeyJ + pgeyPaso) % pgeyN
            if (pgeyR, pgeyJn) not in pgeyP or (pgeyR, pgeyJ) not in pgeyP:
                break
            pgeyAcc += float(np.linalg.norm(pgeyP[(pgeyR, pgeyJn)] - pgeyP[(pgeyR, pgeyJ)]))
            pgeyOut[pgeyJn] = pgeyAcc
            pgeyJ = pgeyJn
        return pgeyOut

    pgeySFr, pgeySEs = {}, {}
    for pgeyR in range(pgeyFilas):
        pgeySFr.update({(pgeyR, pgeyJ): pgeyA for pgeyJ, pgeyA in pgeyArcoFila(pgeyR, 0, +1, pgeyJ90).items()})
        pgeySFr.update({(pgeyR, pgeyJ): -pgeyA for pgeyJ, pgeyA in pgeyArcoFila(pgeyR, 0, -1, pgeyJ270).items()})
        pgeySEs.update({(pgeyR, pgeyJ): -pgeyA for pgeyJ, pgeyA in pgeyArcoFila(pgeyR, pgeyN // 2, -1, pgeyJ90).items()})
        pgeySEs.update({(pgeyR, pgeyJ): pgeyA for pgeyJ, pgeyA in pgeyArcoFila(pgeyR, pgeyN // 2, +1, pgeyJ270).items()})
    pgeyYRef = pgeyC.pgeyBaseCuello
    pgeyVRaw = {}
    for pgeyJ in range(pgeyN):
        for pgeyR in range(pgeyRS + 1):
            if (pgeyR, pgeyJ) in pgeyP:
                pgeyVRaw[(pgeyR, pgeyJ)] = pgeyYRef - pgeyP[(pgeyR, pgeyJ)][1]
        pgeyAcc = pgeyYRef - pgeyP[(pgeyRS, pgeyJ)][1]
        for pgeyR in range(pgeyRS + 1, pgeyFilas):
            pgeyAcc -= float(np.linalg.norm(pgeyP[(pgeyR, pgeyJ)] - pgeyP[(pgeyR - 1, pgeyJ)]))
            pgeyVRaw[(pgeyR, pgeyJ)] = pgeyAcc
    pgeySMax = max(abs(pgeyA) for pgeyKey, pgeyA in list(pgeySFr.items()) + list(pgeySEs.items()) if pgeyKey[0] <= pgeyRA)
    pgeyEscala = max(pgeySMax / (0.49 * PGEY_AREA_W), (max(pgeyVRaw.values()) + 0.01) / (0.98 * PGEY_AREA_H))
    for pgeyJ in range(pgeyN):
        pgeyBase = pgeyVRaw[(pgeyRS, pgeyJ)]
        pgeyBajo = min(pgeyVRaw[(pgeyR, pgeyJ)] for pgeyR in range(pgeyRS, pgeyFilas))
        if pgeyBajo < 0.004 * PGEY_AREA_H * pgeyEscala:
            pgeyF = (pgeyBase - 0.004 * PGEY_AREA_H * pgeyEscala) / max(pgeyBase - pgeyBajo, 1e-9)
            for pgeyR in range(pgeyRS + 1, pgeyFilas):
                pgeyVRaw[(pgeyR, pgeyJ)] = pgeyBase - (pgeyBase - pgeyVRaw[(pgeyR, pgeyJ)]) * pgeyF
    pgeyTope = 0.49 * PGEY_AREA_W * pgeyEscala
    for pgeySDict in (pgeySFr, pgeySEs):
        for pgeyR in range(pgeyRA + 1, pgeyFilas):
            pgeyFila = {pgeyKey: pgeyA for pgeyKey, pgeyA in pgeySDict.items() if pgeyKey[0] == pgeyR}
            pgeyLadoMax = {1: max([pgeyA for pgeyA in pgeyFila.values() if pgeyA > 0], default=0.0),
                        -1: -min([pgeyA for pgeyA in pgeyFila.values() if pgeyA < 0], default=0.0)}
            for pgeyKey, pgeyA in pgeyFila.items():
                pgeyLim = pgeyLadoMax[1 if pgeyA > 0 else -1]
                if pgeyLim > pgeyTope:
                    pgeySDict[pgeyKey] = pgeyA * pgeyTope / pgeyLim

    pgeyYClav = pgeyC.pgeyBaseCuello - pgeyCorte.pgeyClavicula * pgeyK
    pgeyYFrente = pgeyP[(pgeyFilas - 1, 0)][1]
    pgeyYPunta = min(max(pgeyYClav - pgeyCorte.pgeyVProfundidad * pgeyK, pgeyYFrente - pgeyCorte.pgeyVMaxEscote * pgeyK), pgeyYFrente - 0.02 * pgeyK)
    pgeyJMax = int(np.argmin([abs(pgeyTh[pgeyJ] - math.radians(pgeyCorte.pgeyVAngulo)) for pgeyJ in range(pgeyN // 2)]))
    pgeyJV = min(range(1, pgeyJMax + 1), key=lambda pgeyJ: (pgeyP[(pgeyFilas - 1, pgeyJ)][1] - pgeyYPunta) / pgeyTh[pgeyJ])
    pgeyThV = pgeyTh[pgeyJV]
    pgeyYEsq = pgeyP[(pgeyFilas - 1, pgeyJV)][1]

    def pgeyAng(pgeyJ):
        return pgeyTh[pgeyJ] if pgeyTh[pgeyJ] <= math.pi else pgeyTh[pgeyJ] - 2 * math.pi

    def pgeyMezclar(pgeyDestino, pgeyA, pgeyB, pgeyF):
        pgeyP[pgeyDestino] = pgeyOrig[pgeyA] + (pgeyOrig[pgeyB] - pgeyOrig[pgeyA]) * pgeyF
        pgeySFr[pgeyDestino] = pgeySOrig[pgeyA] + (pgeySOrig[pgeyB] - pgeySOrig[pgeyA]) * pgeyF
        pgeyVRaw[pgeyDestino] = pgeyVOrig[pgeyA] + (pgeyVOrig[pgeyB] - pgeyVOrig[pgeyA]) * pgeyF

    pgeyOrig, pgeySOrig, pgeyVOrig = dict(pgeyP), dict(pgeySFr), dict(pgeyVRaw)
    pgeyQuitado, pgeyAjustado = set(), set()
    for pgeyJ in list(range(0, pgeyJV)) + list(range(pgeyN - pgeyJV + 1, pgeyN)):
        pgeyYL = pgeyYPunta + (pgeyYEsq - pgeyYPunta) * abs(pgeyAng(pgeyJ)) / pgeyThV
        pgeyArriba = [pgeyR for pgeyR in range(pgeyRA, pgeyFilas) if (pgeyR, pgeyJ) in pgeyP and pgeyP[(pgeyR, pgeyJ)][1] > pgeyYL + 1e-6]
        if not pgeyArriba:
            continue
        pgeyQuitado.update((pgeyR, pgeyJ) for pgeyR in pgeyArriba)
        pgeyR1 = min(pgeyArriba)
        pgeyR0 = pgeyR1 - 1
        pgeyF = (pgeyYL - pgeyOrig[(pgeyR0, pgeyJ)][1]) / (pgeyOrig[(pgeyR1, pgeyJ)][1] - pgeyOrig[(pgeyR0, pgeyJ)][1])
        pgeyDestino = (pgeyR0, pgeyJ) if pgeyF < 0.3 else (pgeyR1, pgeyJ)
        pgeyQuitado.discard(pgeyDestino)
        pgeyMezclar(pgeyDestino, (pgeyR0, pgeyJ), (pgeyR1, pgeyJ), pgeyF)
        pgeyAjustado.add(pgeyDestino)
    for pgeyR in range(pgeyRA, pgeyFilas):
        for pgeyPaso in (+1, -1):
            pgeyJ = 0
            while (pgeyR, pgeyJ) in pgeyQuitado:
                pgeyJ = (pgeyJ + pgeyPaso) % pgeyN
            pgeyJin = (pgeyJ - pgeyPaso) % pgeyN
            if pgeyJ == 0 or (pgeyR, pgeyJin) not in pgeyQuitado or (pgeyR, pgeyJ) in pgeyAjustado:
                continue
            pgeyAObj = pgeyThV * (pgeyOrig[(pgeyR, pgeyJ)][1] - pgeyYPunta) / (pgeyYEsq - pgeyYPunta)
            pgeyAIn, pgeyAJ = abs(pgeyAng(pgeyJin)), abs(pgeyAng(pgeyJ))
            if pgeyAIn < pgeyAObj < pgeyAJ:
                pgeyMezclar((pgeyR, pgeyJ), (pgeyR, pgeyJin), (pgeyR, pgeyJ), (pgeyAObj - pgeyAIn) / (pgeyAJ - pgeyAIn))

    pgeyM = PgeyMalla(PGEY_ZONAS)
    pgeyIdx = {pgeyKey: pgeyM.pgeyVert(pgeyQ) for pgeyKey, pgeyQ in pgeyP.items() if pgeyKey not in pgeyQuitado}
    pgeyLc = pgeyCorte.pgeyLadoColumnas

    def pgeyUvTorso(pgeyR, pgeyJ, pgeyZona, pgeyLado, pgeyDelante):
        if pgeyZona == "jersey_sides":
            pgeyU = (pgeyJ - (pgeyJlIzq - pgeyLc)) if pgeyLado > 0 else (pgeyJlDer + pgeyLc - pgeyJ)
            return pgeyIsla("lado_izq" if pgeyLado > 0 else "lado_der", pgeyU / (2 * pgeyLc), 1 - pgeyR / pgeyRA)
        pgeyV = 0.01 + pgeyVRaw[(pgeyR, pgeyJ)] / (PGEY_AREA_H * pgeyEscala)
        if pgeyDelante:
            return pgeyIsla("frente", 0.5 + pgeySFr[(pgeyR, pgeyJ)] / (PGEY_AREA_W * pgeyEscala), pgeyV)
        return pgeyIsla("espalda", 0.5 + pgeySEs[(pgeyR, pgeyJ)] / (PGEY_AREA_W * pgeyEscala), pgeyV)

    for pgeyR in range(pgeyFilas - 1):
        for pgeyJ in range(pgeyN):
            pgeyJ2 = (pgeyJ + 1) % pgeyN
            if pgeyRA <= pgeyR < pgeyRS and any(pgeyA <= pgeyJ < pgeyB for pgeyA, pgeyB in pgeySisas):
                continue
            pgeyEsq = [(pgeyR, pgeyJ), (pgeyR, pgeyJ2), (pgeyR + 1, pgeyJ2), (pgeyR + 1, pgeyJ)]
            pgeyVivos = [pgeyQ for pgeyQ in pgeyEsq if pgeyQ in pgeyIdx]
            if len(pgeyVivos) < 3:
                continue
            pgeyTm = (pgeyTh[pgeyJ] + (pgeyTh[pgeyJ2] if pgeyJ2 else 2 * math.pi)) / 2
            pgeyDelante = math.cos(pgeyTm) > 0
            pgeyLado = 1 if pgeyTm < math.pi else -1
            pgeyJl = pgeyJlIzq if pgeyLado > 0 else pgeyJlDer
            pgeyZona = "jersey_sides" if pgeyR < pgeyRA and pgeyJl - pgeyLc <= pgeyJ < pgeyJl + pgeyLc else "jersey_body"
            pgeyM.pgeyCara([pgeyIdx[pgeyQ] for pgeyQ in pgeyVivos], [pgeyUvTorso(pgeyQ[0], pgeyQ[1], pgeyZona, pgeyLado, pgeyDelante) for pgeyQ in pgeyVivos], pgeyZona)
    for (pgeyR, pgeyJ), pgeyI in pgeyIdx.items():
        if pgeyR >= pgeyRS:
            pgeyM.pgeyPeso("fijar", pgeyI, float(pgeySmoothstep(0.25, 0.9, (pgeyR - pgeyRS) / pgeyN3)))
        if pgeyR <= 1:
            pgeyM.pgeyPeso("doble", pgeyI, 1.0)

    pgeyMangas = {}
    pgeySisaBordes = set()
    pgeySisaTope = []
    for pgeyLado, (pgeyA, pgeyB) in zip((1, -1), pgeySisas):
        pgeyBorde = [(pgeyRA, pgeyJ) for pgeyJ in range(pgeyA, pgeyB)] + [(pgeyR, pgeyB) for pgeyR in range(pgeyRA, pgeyRS)] + \
                [(pgeyRS, pgeyJ) for pgeyJ in range(pgeyB, pgeyA, -1)] + [(pgeyR, pgeyA) for pgeyR in range(pgeyRS, pgeyRA, -1)]
        pgeySisaBordes |= {pgeyIdx[pgeyQ] for pgeyQ in pgeyBorde}
        pgeySisaTope.append(max((pgeyIdx[pgeyQ] for pgeyQ in pgeyBorde), key=lambda pgeyI: pgeyM.pgeyV[pgeyI][1]))
        pgeyMangas[pgeyLado] = _pgeyManga(pgeyC, pgeyM, pgeyLado, [pgeyIdx[pgeyQ] for pgeyQ in pgeyBorde], pgeyCorte, pgeyK)

    pgeyEscote = _pgeyBucleBorde(pgeyM, pgeyIdx[(pgeyFilas - 1, pgeyJ90)])
    pgeyNb = len(pgeyEscote)
    pgeyPos = {pgeyV: pgeyI for pgeyI, pgeyV in enumerate(pgeyEscote)}
    pgeyPunta = min(range(pgeyNb), key=lambda pgeyI: pgeyM.pgeyV[pgeyEscote[pgeyI]][1] if pgeyM.pgeyV[pgeyEscote[pgeyI]][2] > 0 else 9.0)
    pgeyEsquinas = [pgeyPos[pgeyIdx[(pgeyFilas - 1, pgeyJV)]], pgeyPos[pgeyIdx[(pgeyFilas - 1, pgeyN - pgeyJV)]]]
    for pgeyEI in pgeyEsquinas:
        pgeyPaso = 1 if (pgeyEI - pgeyPunta) % pgeyNb > pgeyNb // 2 else -1
        pgeyTramo = [pgeyEI]
        while pgeyTramo[-1] != pgeyPunta:
            pgeyTramo.append((pgeyTramo[-1] + pgeyPaso) % pgeyNb)
        pgeyLargos = np.concatenate(([0.0], np.cumsum([np.linalg.norm(pgeyM.pgeyV[pgeyEscote[pgeyB]] - pgeyM.pgeyV[pgeyEscote[pgeyA]])
                                                   for pgeyA, pgeyB in zip(pgeyTramo, pgeyTramo[1:])])))
        pgeyC0, pgeyC1 = pgeyM.pgeyV[pgeyEscote[pgeyTramo[0]]].copy(), pgeyM.pgeyV[pgeyEscote[pgeyTramo[-1]]].copy()
        for pgeyI, pgeyF in zip(pgeyTramo[1:-1], pgeyLargos[1:-1] / pgeyLargos[-1]):
            pgeyAntes = pgeyM.pgeyV[pgeyEscote[pgeyI]]
            pgeySa, pgeyNa, _ = pgeyC.pgeyCercano(pgeyAntes)
            pgeyDist = max(float(np.dot(pgeyAntes - pgeySa, pgeyNa)), pgeyCorte.pgeyHolguraEscote)
            pgeyQ = pgeyC0 + (pgeyC1 - pgeyC0) * pgeyF
            pgeySq, pgeyNq, _ = pgeyC.pgeyCercano(pgeyQ)
            pgeyM.pgeyV[pgeyEscote[pgeyI]] = pgeySq + pgeyNq * pgeyDist
    pgeyLibres = [pgeyI for pgeyI in range(pgeyNb) if min(abs(pgeyI - pgeyPunta), pgeyNb - abs(pgeyI - pgeyPunta)) > 2
              and min(min(abs(pgeyI - pgeyE), pgeyNb - abs(pgeyI - pgeyE)) for pgeyE in pgeyEsquinas) <= pgeyCorte.pgeyRedondeoVecinos]
    for _ in range(pgeyCorte.pgeyRedondeoEscote):
        pgeyNuevo = {pgeyI: (pgeyM.pgeyV[pgeyEscote[pgeyI - 1]] + pgeyM.pgeyV[pgeyEscote[(pgeyI + 1) % pgeyNb]]) / 2 for pgeyI in pgeyLibres}
        for pgeyI, pgeyQ in pgeyNuevo.items():
            pgeyM.pgeyV[pgeyEscote[pgeyI]] = pgeyM.pgeyV[pgeyEscote[pgeyI]] * 0.5 + pgeyQ * 0.5

    pgeyTramos = []
    for pgeyEI in pgeyEsquinas:
        pgeyPaso = 1 if (pgeyEI - pgeyPunta) % pgeyNb > pgeyNb // 2 else -1
        pgeyT = [pgeyEI]
        while pgeyT[-1] != pgeyPunta:
            pgeyT.append((pgeyT[-1] + pgeyPaso) % pgeyNb)
        pgeyTramos.append(pgeyT)
    pgeyResto = [pgeyEsquinas[1]]
    pgeyPaso = 1 if (pgeyEsquinas[0] - pgeyEsquinas[1]) % pgeyNb < (pgeyPunta - pgeyEsquinas[1]) % pgeyNb else -1
    while pgeyResto[-1] != pgeyEsquinas[0]:
        pgeyResto.append((pgeyResto[-1] + pgeyPaso) % pgeyNb)
    pgeyTramos.append(pgeyResto)
    pgeyRectas = {}
    for pgeyT in pgeyTramos:
        pgeyPol = [pgeyM.pgeyV[pgeyEscote[pgeyI]].copy() for pgeyI in pgeyT]
        for pgeyI in pgeyT[1:-1]:
            pgeyRectas[pgeyEscote[pgeyI]] = (pgeyPol, pgeyEscote[(pgeyI - 1) % pgeyNb], pgeyEscote[(pgeyI + 1) % pgeyNb])
    pgeyCentroV = {pgeyJ for pgeyJ in range(pgeyN) if abs(pgeyAng(pgeyJ)) < 2 * pgeyThV}
    pgeyCanesu = {pgeyI for (pgeyR, pgeyJ), pgeyI in pgeyIdx.items() if pgeyR > pgeyRS or (pgeyR >= pgeyRA and pgeyJ in pgeyCentroV)} \
        - set(pgeyEscote) - pgeySisaBordes
    pgeyM.pgeyRelajar(pgeyCanesu | set(pgeyRectas), pgeyCorte.pgeyRelajadoCanesu, pgeyRectas=pgeyRectas)

    pgeyAdy = pgeyM.pgeyVecinos()
    pgeyAnillo, pgeyVistos = set(pgeyEscote), set(pgeyEscote)
    for pgeyPeso in (1.0, 1.0, 0.95, 0.85, 0.65, 0.4):
        for pgeyV in pgeyAnillo:
            pgeyM.pgeyPeso("fijar", pgeyV, pgeyPeso)
        pgeyAnillo = {pgeyW for pgeyV in pgeyAnillo for pgeyW in pgeyAdy[pgeyV]} - pgeyVistos
        pgeyVistos |= pgeyAnillo

    pgeyBordes = set(pgeyEscote) | {pgeyIdx[(0, pgeyJ)] for pgeyJ in range(pgeyN)}
    pgeyBordes |= {pgeyV for pgeyFin in pgeyMangas.values() for pgeyV in pgeyFin}
    pgeyM.pgeySuavizar(pgeyCorte.pgeySuavizado, 0.5, set(range(len(pgeyM.pgeyV))) - pgeyBordes)

    pgeyDatos = {"filas": pgeyFilas, "columnas": pgeyN, "escala_uv": pgeyEscala, "sisa_tope": pgeySisaTope,
             "escote": _pgeyBucleBorde(pgeyM, pgeyIdx[(pgeyFilas - 1, pgeyJ90)]),
             "esquinas_v": [pgeyEscote[pgeyE] for pgeyE in pgeyEsquinas],
             "ruedo": [pgeyIdx[(0, pgeyJ)] for pgeyJ in range(pgeyN)], "mangas": pgeyMangas}
    return pgeyM, pgeyDatos


def _pgeyManga(pgeyC, pgeyM, pgeyLado, pgeyBorde, pgeyCorte, pgeyK):
    pgeyE = pgeyCorte.pgeyArista
    _, pgeyD = pgeyC.pgeyBrazo[pgeyLado]
    pgeyA0 = pgeyC.pgeyBrazoPunto(pgeyLado, 0.0)
    pgeyFa = np.array((0.0, 0.0, 1.0)) - pgeyD * pgeyD[2]
    pgeyFa /= np.linalg.norm(pgeyFa)
    pgeySa = np.cross(pgeyD, pgeyFa)
    if pgeySa[0] * pgeyLado < 0:
        pgeySa = -pgeySa
    pgeyL = [pgeyM.pgeyV[pgeyI] for pgeyI in pgeyBorde]
    pgeySM = [float(np.dot(pgeyQ - pgeyA0, pgeyD)) for pgeyQ in pgeyL]
    pgeyCentro = np.mean(pgeyL, axis=0)
    pgeyAl = []
    for pgeyQ in pgeyL:
        pgeyW = pgeyQ - pgeyCentro - pgeyD * np.dot(pgeyQ - pgeyCentro, pgeyD)
        pgeyAl.append(math.atan2(float(np.dot(pgeyW, pgeySa)), float(np.dot(pgeyW, pgeyFa))) % (2 * math.pi))
    pgeyLarga = pgeyCorte.pgeyMangaLarga
    pgeySFin = (pgeyCorte.pgeyLargoMangaLarga if pgeyLarga else pgeyCorte.pgeyLargoManga) * pgeyK
    pgeyAnillos = max(4, round((pgeySFin - float(np.mean(pgeySM))) / pgeyE))
    pgeyRadios = {}
    pgeyEje = pgeyC.pgeyBrazoEje(pgeyLado, pgeySFin + 0.01) if pgeyLarga else None

    def pgeyMarco(pgeyS):
        if pgeyEje is None:
            return pgeyA0 + pgeyD * pgeyS, pgeyFa, pgeySa
        pgeyP, pgeyT = pgeyEje(pgeyS)
        pgeyF = np.array((0.0, 0.0, 1.0)) - pgeyT * pgeyT[2]
        pgeyF /= np.linalg.norm(pgeyF)
        pgeySv = np.cross(pgeyT, pgeyF)
        if pgeySv[0] * pgeyLado < 0:
            pgeySv = -pgeySv
        return pgeyP, pgeyF, pgeySv

    def pgeyTubo(pgeyS, pgeyA):
        pgeyCentroS, pgeyF, pgeySv = pgeyMarco(pgeyS)
        pgeyDirec = pgeyF * math.cos(pgeyA) + pgeySv * math.sin(pgeyA)
        pgeyClave = (round(max(pgeyS, 0.05), 3), round(pgeyA, 3))
        if pgeyClave not in pgeyRadios:
            if pgeyEje is None:
                pgeyRadios[pgeyClave] = pgeyC.pgeyBrazoRadio(pgeyLado, max(pgeyS, 0.05), pgeyDirec)
            else:
                pgeyCs, pgeyFs, pgeySvs = pgeyMarco(max(pgeyS, 0.05))
                pgeyHit = pgeyC.pgeyRayo(pgeyCs, pgeyFs * math.cos(pgeyA) + pgeySvs * math.sin(pgeyA), 0.2)
                pgeyRadios[pgeyClave] = pgeyHit[2] if pgeyHit else 0.05
        pgeyHol = pgeyCorte.pgeyHolguraManga
        if pgeyLarga:
            pgeyHol = pgeyCorte.pgeyHolguraHombroLarga + (pgeyCorte.pgeyHolguraManga - pgeyCorte.pgeyHolguraHombroLarga) * float(
                pgeySmoothstep(0.08 * pgeyK, 0.20 * pgeyK, pgeyS))
            pgeyHol += (pgeyCorte.pgeyHolguraPuno - pgeyCorte.pgeyHolguraManga) * float(pgeySmoothstep(pgeySFin - 0.14 * pgeyK, pgeySFin, pgeyS))
        return pgeyCentroS + pgeyDirec * (pgeyRadios[pgeyClave] + pgeyHol)

    pgeySMin = min(pgeySM)
    pgeyEscala = 1.0 if pgeyLarga else max(1.0, (pgeySFin - pgeySMin) / (0.97 * PGEY_MANGA_H))
    pgeyTMezcla = 0.7
    if pgeyLarga:
        pgeyTMezcla = 0.7 * (pgeyCorte.pgeyLargoManga * pgeyK - float(np.mean(pgeySM))) / (pgeySFin - float(np.mean(pgeySM)))
    pgeyAnclaje = pgeyCorte.pgeyAnclajeMangaLarga if pgeyLarga else pgeyCorte.pgeyAnclajeManga
    pgeyIds = [list(pgeyBorde)]
    for pgeyI in range(1, pgeyAnillos + 1):
        pgeyT = pgeyI / pgeyAnillos
        pgeyW = float(pgeySmoothstep(0.0, pgeyTMezcla, pgeyT))
        pgeyFila = []
        for pgeyQ, pgeyS, pgeyA in zip(pgeyL, pgeySM, pgeyAl):
            pgeySi = pgeyS + (pgeySFin - pgeyS) * pgeyT
            pgeyFin = pgeyTubo(pgeySFin, pgeyA)
            pgeyFila.append(pgeyM.pgeyVert((1 - pgeyW) * (pgeyQ + (pgeyFin - pgeyQ) * pgeyT) + pgeyW * pgeyTubo(pgeySi, pgeyA)))
            pgeyM.pgeyPeso("fijar", pgeyFila[-1], pgeyAnclaje[0] + (pgeyAnclaje[1] - pgeyAnclaje[0]) * pgeyT)
            if pgeyI >= pgeyAnillos - 1:
                pgeyM.pgeyPeso("doble", pgeyFila[-1], 1.0)
        pgeyIds.append(pgeyFila)
    pgeyNb = len(pgeyBorde)
    pgeyIslaM = "manga_izq" if pgeyLado > 0 else "manga_der"
    pgeyIslaP = "puno_izq" if pgeyLado > 0 else "puno_der"
    pgeyIslaA = "antebrazo_izq" if pgeyLado > 0 else "antebrazo_der"

    def pgeySAnillo(pgeyRing, pgeyBb):
        return pgeySM[pgeyBb] + (pgeySFin - pgeySM[pgeyBb]) * pgeyRing / pgeyAnillos

    pgeyICorte = pgeyAnillos
    if pgeyLarga:
        pgeyICorte = max(pgeyI for pgeyI in range(pgeyAnillos + 1)
                      if max(pgeySAnillo(pgeyI, pgeyBb) for pgeyBb in range(pgeyNb)) <= pgeySMin + 0.97 * PGEY_MANGA_H)
    for pgeyI in range(pgeyAnillos):
        pgeyPuno = pgeyI >= pgeyAnillos - pgeyCorte.pgeyPunoFilas
        for pgeyB in range(pgeyNb):
            pgeyB2 = (pgeyB + 1) % pgeyNb
            pgeyUs = {pgeyB: pgeyAl[pgeyB] / (2 * math.pi), pgeyB2: pgeyAl[pgeyB2] / (2 * math.pi)}
            if abs(pgeyUs[pgeyB] - pgeyUs[pgeyB2]) > 0.5:
                pgeyUs = {pgeyX: (pgeyU + 1 if pgeyU < 0.5 else pgeyU) for pgeyX, pgeyU in pgeyUs.items()}
            pgeyEsquinas = [(pgeyI, pgeyB), (pgeyI, pgeyB2), (pgeyI + 1, pgeyB2), (pgeyI + 1, pgeyB)]
            pgeyUv = []
            for pgeyRing, pgeyBb in pgeyEsquinas:
                if pgeyPuno:
                    pgeyV = (pgeyRing - (pgeyAnillos - pgeyCorte.pgeyPunoFilas)) / pgeyCorte.pgeyPunoFilas * 0.85
                    pgeyUv.append(pgeyIsla(pgeyIslaP, pgeyUs[pgeyBb], pgeyV))
                elif pgeyI >= pgeyICorte:
                    pgeyUv.append(pgeyIsla(pgeyIslaA, pgeyUs[pgeyBb], (pgeySAnillo(pgeyRing, pgeyBb) - pgeySAnillo(pgeyICorte, pgeyBb)) / PGEY_ANTEBRAZO_H))
                else:
                    pgeySVert = pgeySM[pgeyBb] + (pgeySFin - pgeySM[pgeyBb]) * pgeyRing / pgeyAnillos
                    pgeyUv.append(pgeyIsla(pgeyIslaM, pgeyUs[pgeyBb], 0.01 + (pgeySVert - pgeySMin) / (PGEY_MANGA_H * pgeyEscala)))
            pgeyM.pgeyCara([pgeyIds[pgeyRing][pgeyBb] for pgeyRing, pgeyBb in pgeyEsquinas], pgeyUv, "jersey_cuffs" if pgeyPuno else "jersey_sleeves")
    pgeyInterior = {pgeyV for pgeyFila in pgeyIds[1:-1] for pgeyV in pgeyFila}
    pgeyM.pgeySuavizar(4, 0.5, pgeyInterior)
    return pgeyIds[-1]


def _pgeyBucleBorde(pgeyM, pgeyInicio):
    pgeyUsos = {}
    for pgeyIdx, _, _ in pgeyM.pgeyCaras:
        for pgeyA in range(len(pgeyIdx)):
            pgeyE = tuple(sorted((pgeyIdx[pgeyA], pgeyIdx[(pgeyA + 1) % len(pgeyIdx)])))
            pgeyUsos[pgeyE] = pgeyUsos.get(pgeyE, 0) + 1
    pgeyAdy = {}
    for (pgeyA, pgeyB), pgeyU in pgeyUsos.items():
        if pgeyU == 1:
            pgeyAdy.setdefault(pgeyA, []).append(pgeyB)
            pgeyAdy.setdefault(pgeyB, []).append(pgeyA)
    pgeyBucle, pgeyPrev, pgeyCur = [pgeyInicio], None, pgeyInicio
    while True:
        pgeySig = [pgeyX for pgeyX in pgeyAdy[pgeyCur] if pgeyX != pgeyPrev]
        if not pgeySig or pgeySig[0] == pgeyInicio:
            break
        pgeyPrev, pgeyCur = pgeyCur, pgeySig[0]
        pgeyBucle.append(pgeyCur)
        if len(pgeyBucle) > 10000:
            raise RuntimeError("borde sin cerrar")
    return pgeyBucle
