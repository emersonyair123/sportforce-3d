import math

import numpy as np

from cuerpo import pgeyHull2d, pgeyRadioPolar
from malla import PgeyMalla, pgeyIsla

PGEY_ZONAS = ["socks_leg", "socks_band"]


class PgeyCorte:
    pgeyArista = 0.012
    pgeyAltoMedia = 0.253
    pgeyAltoBanda = 0.029
    pgeyTobillo = 0.075
    pgeyFondo = 0.03
    pgeyHolgura = 0.0030
    pgeyHolguraPie = 0.0065
    pgeyHolguraBanda = 0.0065
    pgeyAltoRef = None


def _pgeyAlturas(pgeyC, pgeyCorte, pgeyAltoMedia):
    pgeyE = pgeyCorte.pgeyArista
    pgeyH, pgeyS0 = pgeyC.pgeyAlto, pgeyC.pgeySuelo
    pgeyYTop = pgeyS0 + pgeyAltoMedia * pgeyH
    pgeyYBanda = pgeyYTop - pgeyCorte.pgeyAltoBanda * pgeyH
    pgeyYTob = pgeyS0 + pgeyCorte.pgeyTobillo * pgeyH
    pgeyYFin = pgeyS0 + pgeyCorte.pgeyFondo * pgeyH
    pgeyNLeg = max(8, round((pgeyYBanda - pgeyYTob) / pgeyE))
    pgeyNPie = max(4, round((pgeyYTob - pgeyYFin) / (pgeyE / 2)))
    return ([pgeyYTop - (pgeyYTop - pgeyYBanda) * pgeyI / 3 for pgeyI in range(3)]
            + [pgeyYBanda - (pgeyYBanda - pgeyYTob) * pgeyI / pgeyNLeg for pgeyI in range(pgeyNLeg)]
            + [pgeyYTob - (pgeyYTob - pgeyYFin) * pgeyI / pgeyNPie for pgeyI in range(pgeyNPie + 1)])


def pgeyConstruir(pgeyC, pgeyCorte=PgeyCorte):
    pgeyE = pgeyCorte.pgeyArista
    pgeyH, pgeyS0 = pgeyC.pgeyAlto, pgeyC.pgeySuelo
    pgeyYTop = pgeyS0 + pgeyCorte.pgeyAltoMedia * pgeyH
    pgeyYBanda = pgeyYTop - pgeyCorte.pgeyAltoBanda * pgeyH
    pgeyYTob = pgeyS0 + pgeyCorte.pgeyTobillo * pgeyH
    pgeyYFin = pgeyS0 + pgeyCorte.pgeyFondo * pgeyH
    pgeyM = PgeyMalla(PGEY_ZONAS)
    pgeyDatos = {"bordes": {}}
    for pgeyLado in (1, -1):
        pgeySecciones = {}

        def pgeyCorteEn(pgeyY):
            pgeyClave = round(pgeyY / 0.003)
            if pgeyClave not in pgeySecciones:
                pgeyP = pgeyC.pgeyPiernaCorte(pgeyClave * 0.003, pgeyLado)
                pgeyHull = pgeyHull2d(pgeyP)
                pgeyCentro = np.array(((pgeyHull[:, 0].max() + pgeyHull[:, 0].min()) / 2,
                                   (pgeyHull[:, 1].max() + pgeyHull[:, 1].min()) / 2))
                pgeySecciones[pgeyClave] = (pgeyHull, pgeyCentro)
            return pgeySecciones[pgeyClave]

        def pgeySeccion(pgeyY):
            if pgeyY >= pgeyYTob:
                return pgeyCorteEn(pgeyY)
            pgeyHullT, pgeyCentroT = pgeyCorteEn(pgeyYTob)
            pgeyP = pgeyC.pgeyPiernaCorte(pgeyY, pgeyLado)
            if len(pgeyP) < 3:
                return pgeyHullT, pgeyCentroT
            return pgeyHull2d(np.concatenate((pgeyHullT, pgeyP))), pgeyCentroT

        pgeyHull, pgeyCentro = pgeySeccion(pgeyYTop)
        pgeyCirc = float(np.sum(np.linalg.norm(np.diff(np.vstack((pgeyHull, pgeyHull[:1])), axis=0), axis=1)))
        pgeyCols = max(24, 2 * round(pgeyCirc / pgeyE / 2))
        pgeyYs = _pgeyAlturas(pgeyC, pgeyCorte, pgeyCorte.pgeyAltoMedia)
        if pgeyCorte.pgeyAltoRef is None:
            pgeyVCana = [(pgeyFi - 3) / (len(pgeyYs) - 4) for pgeyFi in range(len(pgeyYs))]
        else:
            pgeyRef = _pgeyAlturas(pgeyC, pgeyCorte, pgeyCorte.pgeyAltoRef)[3:]
            pgeyVRef = [pgeyI / (len(pgeyRef) - 1) for pgeyI in range(len(pgeyRef))]
            pgeyVCana = [float(np.interp(pgeyY, pgeyRef[::-1], pgeyVRef[::-1])) for pgeyY in pgeyYs]
        pgeyAngs = [2 * math.pi * pgeyK / pgeyCols for pgeyK in range(pgeyCols)]
        pgeyFilas = []
        for pgeyY in pgeyYs:
            pgeyHull, pgeyCentro = pgeySeccion(pgeyY)
            pgeyRad = pgeyRadioPolar(pgeyHull, pgeyCentro, [pgeyA if pgeyLado > 0 else -pgeyA for pgeyA in pgeyAngs])
            pgeyHol = pgeyCorte.pgeyHolguraBanda if pgeyY > pgeyYBanda + 1e-6 else pgeyCorte.pgeyHolgura if pgeyY >= pgeyYTob else pgeyCorte.pgeyHolguraPie
            pgeyFila = []
            for pgeyA, pgeyR in zip(pgeyAngs, pgeyRad):
                pgeyX = pgeyCentro[0] + pgeyLado * math.sin(pgeyA) * (pgeyR + pgeyHol)
                pgeyZ = pgeyCentro[1] + math.cos(pgeyA) * (pgeyR + pgeyHol)
                pgeyFila.append(pgeyM.pgeyVert((pgeyX, pgeyY, pgeyZ)))
            pgeyFilas.append(pgeyFila)
        pgeyIslaL = "media_izq" if pgeyLado > 0 else "media_der"
        pgeyIslaB = "banda_izq" if pgeyLado > 0 else "banda_der"
        for pgeyI in range(len(pgeyFilas) - 1):
            pgeyBanda = pgeyI < 3
            for pgeyK in range(pgeyCols):
                pgeyK2 = (pgeyK + 1) % pgeyCols
                pgeyUs = (pgeyK / pgeyCols, 1.0 if pgeyK2 == 0 else pgeyK2 / pgeyCols)
                pgeyEsquinas = [(pgeyI, pgeyK, pgeyUs[0]), (pgeyI + 1, pgeyK, pgeyUs[0]), (pgeyI + 1, pgeyK2, pgeyUs[1]), (pgeyI, pgeyK2, pgeyUs[1])]
                if pgeyLado < 0:
                    pgeyEsquinas = pgeyEsquinas[::-1]
                pgeyUv = []
                for pgeyFi, _, pgeyU in pgeyEsquinas:
                    if pgeyBanda:
                        pgeyUv.append(pgeyIsla(pgeyIslaB, pgeyU, (3 - pgeyFi) / 3 * 0.8))
                    else:
                        pgeyUv.append(pgeyIsla(pgeyIslaL, pgeyU, pgeyVCana[pgeyFi]))
                pgeyM.pgeyCara([pgeyFilas[pgeyFi][pgeyKk] for pgeyFi, pgeyKk, _ in pgeyEsquinas], pgeyUv, "socks_band" if pgeyBanda else "socks_leg")
        for pgeyFi in (0, 1):
            for pgeyV in pgeyFilas[pgeyFi]:
                pgeyM.pgeyPeso("doble", pgeyV, 1.0)
        pgeyDatos["bordes"][pgeyLado] = pgeyFilas[0]
    return pgeyM, pgeyDatos
