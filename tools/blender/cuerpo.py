import math

import bpy
import numpy as np
from mathutils import Vector
from mathutils.bvhtree import BVHTree


def pgeyABlender(pgeyP):
    return Vector((pgeyP[0], -pgeyP[2], pgeyP[1]))


def pgeyAModelo(pgeyV):
    return np.array((pgeyV[0], pgeyV[2], -pgeyV[1]))


def pgeyImportar(pgeyRutaObj):
    pgeyAntes = set(bpy.data.objects)
    bpy.ops.wm.obj_import(filepath=str(pgeyRutaObj), forward_axis='NEGATIVE_Z', up_axis='Y')
    pgeyNuevos = [pgeyO for pgeyO in bpy.data.objects if pgeyO not in pgeyAntes]
    for pgeyO in pgeyNuevos:
        bpy.context.view_layer.objects.active = pgeyO
        pgeyO.select_set(True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    pgeyObj = pgeyNuevos[0]
    bpy.ops.object.select_all(action='DESELECT')
    pgeyObj.select_set(True)
    bpy.context.view_layer.objects.active = pgeyObj
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.separate(type='MATERIAL')
    bpy.ops.object.mode_set(mode='OBJECT')
    pgeyPartes = [pgeyO for pgeyO in bpy.data.objects if pgeyO.type == 'MESH' and pgeyO not in pgeyAntes]
    pgeyPiel = max(pgeyPartes, key=lambda pgeyO: len(pgeyO.data.polygons))
    pgeyPiel.name = "persona_piel"
    pgeyOtros = [pgeyO for pgeyO in pgeyPartes if pgeyO is not pgeyPiel]
    for pgeyI, pgeyO in enumerate(pgeyOtros):
        pgeyO.name = "persona_" + (pgeyO.active_material.name if pgeyO.active_material else str(pgeyI))
    return pgeyPiel, pgeyOtros


class PgeyCuerpo:

    def __init__(self, pgeyPiel):
        pgeyMe = pgeyPiel.data
        self.pgeyV = np.array([pgeyAModelo(pgeyX.co) for pgeyX in pgeyMe.vertices])
        pgeyTris = []
        for pgeyP in pgeyMe.polygons:
            pgeyVs = list(pgeyP.vertices)
            for pgeyK in range(1, len(pgeyVs) - 1):
                pgeyTris.append((pgeyVs[0], pgeyVs[pgeyK], pgeyVs[pgeyK + 1]))
        self.pgeyT = np.array(pgeyTris, dtype=np.int64)
        self.pgeyBvh = BVHTree.FromPolygons([Vector(pgeyX) for pgeyX in self.pgeyV], [tuple(pgeyT) for pgeyT in self.pgeyT])
        self.pgeySuelo = float(self.pgeyV[:, 1].min())
        self.pgeyAlto = float(self.pgeyV[:, 1].max()) - self.pgeySuelo
        self._pgeyMedir()

    def pgeyRayo(self, pgeyOrigen, pgeyDireccion, pgeyDist=2.0):
        pgeyD = Vector(pgeyDireccion).normalized()
        pgeyLoc, pgeyNrm, _, pgeyDistHit = self.pgeyBvh.ray_cast(Vector(pgeyOrigen), pgeyD, pgeyDist)
        if pgeyLoc is None:
            return None
        return np.array(pgeyLoc), np.array(pgeyNrm), pgeyDistHit

    def pgeyCercano(self, pgeyP):
        pgeyLoc, pgeyNrm, _, pgeyD = self.pgeyBvh.find_nearest(Vector(pgeyP))
        return np.array(pgeyLoc), np.array(pgeyNrm), pgeyD

    def pgeyCorte(self, pgeyY):
        pgeyV = self.pgeyV
        pgeyA, pgeyB, pgeyC = pgeyV[self.pgeyT[:, 0]], pgeyV[self.pgeyT[:, 1]], pgeyV[self.pgeyT[:, 2]]
        pgeyPts = []
        for pgeyP, pgeyQ in ((pgeyA, pgeyB), (pgeyB, pgeyC), (pgeyC, pgeyA)):
            pgeyYa, pgeyYb = pgeyP[:, 1], pgeyQ[:, 1]
            pgeyM = (pgeyYa - pgeyY) * (pgeyYb - pgeyY) < 0
            pgeyK = (pgeyY - pgeyYa[pgeyM]) / (pgeyYb[pgeyM] - pgeyYa[pgeyM])
            pgeyPts.append(pgeyP[pgeyM][:, [0, 2]] + (pgeyQ[pgeyM][:, [0, 2]] - pgeyP[pgeyM][:, [0, 2]]) * pgeyK[:, None])
        return np.concatenate(pgeyPts) if pgeyPts else np.zeros((0, 2))

    def pgeySegmentos(self, pgeyY):
        pgeyV = self.pgeyV
        pgeyA, pgeyB, pgeyC = pgeyV[self.pgeyT[:, 0]], pgeyV[self.pgeyT[:, 1]], pgeyV[self.pgeyT[:, 2]]
        pgeyLados = []
        for pgeyP, pgeyQ in ((pgeyA, pgeyB), (pgeyB, pgeyC), (pgeyC, pgeyA)):
            pgeyYa, pgeyYb = pgeyP[:, 1], pgeyQ[:, 1]
            pgeyM = (pgeyYa - pgeyY) * (pgeyYb - pgeyY) < 0
            pgeyK = np.where(pgeyM, (pgeyY - pgeyYa) / np.where(pgeyM, pgeyYb - pgeyYa, 1.0), 0.0)
            pgeyPt = pgeyP[:, [0, 2]] + (pgeyQ[:, [0, 2]] - pgeyP[:, [0, 2]]) * pgeyK[:, None]
            pgeyLados.append((pgeyM, pgeyPt))
        pgeySegs = []
        for pgeyI in range(3):
            for pgeyJ in range(pgeyI + 1, 3):
                pgeyM = pgeyLados[pgeyI][0] & pgeyLados[pgeyJ][0]
                pgeySegs.append(np.stack((pgeyLados[pgeyI][1][pgeyM], pgeyLados[pgeyJ][1][pgeyM]), axis=1))
        return np.concatenate(pgeySegs) if pgeySegs else np.zeros((0, 2, 2))

    def pgeyTramosX(self, pgeyY, pgeyHueco=0.008):

        pgeySegs = self.pgeySegmentos(pgeyY)
        if len(pgeySegs) == 0:
            return []
        pgeyIv = np.stack((pgeySegs[:, :, 0].min(axis=1), pgeySegs[:, :, 0].max(axis=1)), axis=1)
        pgeyIv = pgeyIv[np.argsort(pgeyIv[:, 0])]
        pgeyTramos = [[pgeyIv[0, 0], pgeyIv[0, 1]]]
        for pgeyX0, pgeyX1 in pgeyIv[1:]:
            if pgeyX0 - pgeyTramos[-1][1] > pgeyHueco:
                pgeyTramos.append([pgeyX0, pgeyX1])
            else:
                pgeyTramos[-1][1] = max(pgeyTramos[-1][1], pgeyX1)
        return pgeyTramos

    def pgeyTorsoCorte(self, pgeyY):
        pgeyPts = self.pgeyCorte(pgeyY)
        for pgeyT in self.pgeyTramosX(pgeyY):
            if pgeyT[0] <= 0.0 <= pgeyT[1]:
                pgeyM = (pgeyPts[:, 0] >= pgeyT[0] - 1e-6) & (pgeyPts[:, 0] <= pgeyT[1] + 1e-6)
                return pgeyPts[pgeyM]
        return np.zeros((0, 2))

    def pgeyPiernaCorte(self, pgeyY, pgeySigno):
        pgeyPts = self.pgeyCorte(pgeyY)
        if len(pgeyPts) == 0:
            return pgeyPts
        pgeyPts = pgeyPts[pgeyPts[:, 0] * pgeySigno > 0]
        pgeyTramos = [pgeyT for pgeyT in self.pgeyTramosX(pgeyY) if (pgeyT[0] + pgeyT[1]) / 2 * pgeySigno > 0]
        if not pgeyTramos:
            return np.zeros((0, 2))
        pgeyT = min(pgeyTramos, key=lambda pgeyT: abs((pgeyT[0] + pgeyT[1]) / 2))
        pgeyM = (pgeyPts[:, 0] >= pgeyT[0] - 1e-6) & (pgeyPts[:, 0] <= pgeyT[1] + 1e-6)
        return pgeyPts[pgeyM]

    def _pgeyMedir(self):
        pgeyS, pgeyH = self.pgeySuelo, self.pgeyAlto
        pgeyYs = np.arange(pgeyS + 0.30 * pgeyH, pgeyS + 0.70 * pgeyH, 0.002)
        self.pgeyEntrepierna = None
        for pgeyY in pgeyYs:
            if any(pgeyT[0] <= 0.0 <= pgeyT[1] for pgeyT in self.pgeyTramosX(pgeyY)):
                self.pgeyEntrepierna = float(pgeyY)
                break
        self.pgeyAxila = None
        for pgeyY in np.arange(pgeyS + 0.84 * pgeyH, pgeyS + 0.66 * pgeyH, -0.002):
            pgeyTr = self.pgeyTramosX(pgeyY)
            pgeyCentro = [pgeyT for pgeyT in pgeyTr if pgeyT[0] <= 0.0 <= pgeyT[1]]
            if pgeyCentro and sum(1 for pgeyT in pgeyTr if pgeyT[0] > pgeyCentro[0][1]) and sum(1 for pgeyT in pgeyTr if pgeyT[1] < pgeyCentro[0][0]):
                self.pgeyAxila = float(pgeyY)
                break
        def pgeyAncho(pgeyY):
            pgeyP = self.pgeyTorsoCorte(pgeyY)
            return (pgeyP[:, 0].max() - pgeyP[:, 0].min()) / 2 if len(pgeyP) else 0.0
        pgeyYCuello = None
        pgeyAnchos = [(float(pgeyY), pgeyAncho(pgeyY)) for pgeyY in np.arange(self.pgeyAxila, pgeyS + 0.92 * pgeyH, 0.002)]
        pgeyMinimo = min(pgeyA for pgeyY, pgeyA in pgeyAnchos if pgeyY > self.pgeyAxila + 0.08 * pgeyH)
        for pgeyY, pgeyA in pgeyAnchos:
            if pgeyA < pgeyMinimo * 1.35:
                pgeyYCuello = pgeyY
                break
        self.pgeyBaseCuello = pgeyYCuello
        pgeyP = self.pgeyTorsoCorte(pgeyYCuello + 0.02 * pgeyH)
        self.pgeyCuelloCentro = np.array([0.0, (pgeyP[:, 1].max() + pgeyP[:, 1].min()) / 2])
        self.pgeyCuelloRadio = np.array([(pgeyP[:, 0].max() - pgeyP[:, 0].min()) / 2, (pgeyP[:, 1].max() - pgeyP[:, 1].min()) / 2])
        self.pgeyHombro = {}
        for pgeySigno in (1, -1):
            pgeyPerfil = []
            for pgeyX in np.arange(0.0, 0.30, 0.004):
                pgeyHit = self.pgeyRayo((pgeyX * pgeySigno, pgeyS + pgeyH + 0.1, self.pgeyCuelloCentro[1]), (0, -1, 0), 1.0)
                if pgeyHit is not None and pgeyHit[0][1] > self.pgeyAxila:
                    pgeyPerfil.append((float(pgeyX), float(pgeyHit[0][1])))
            pgeyPerfil = [pgeyQ for pgeyQ in pgeyPerfil if pgeyQ[1] <= self.pgeyBaseCuello + 0.005]
            pgeyAcromion = pgeyPerfil[-1]
            for (pgeyX0, pgeyY0), (pgeyX1, pgeyY1) in zip(pgeyPerfil, pgeyPerfil[1:]):
                if pgeyX0 > self.pgeyCuelloRadio[0] * 1.6 and (pgeyY0 - pgeyY1) / (pgeyX1 - pgeyX0) > math.tan(math.radians(35)):
                    pgeyAcromion = (pgeyX0, pgeyY0)
                    break
            self.pgeyHombro[pgeySigno] = np.array(pgeyAcromion)
        self._pgeyBrazos()
        self._pgeyTorsoEje()

    def _pgeyBrazos(self):
        pgeyS, pgeyH = self.pgeySuelo, self.pgeyAlto
        self.pgeyBrazo = {}
        for pgeySigno in (1, -1):
            pgeyCentros = []
            for pgeyY in np.arange(self.pgeyAxila - 0.005, self.pgeyAxila - 0.14 * pgeyH, -0.004):
                pgeyTr = self.pgeyTramosX(pgeyY)
                pgeyCentro = [pgeyT for pgeyT in pgeyTr if pgeyT[0] <= 0.0 <= pgeyT[1]]
                if not pgeyCentro:
                    continue
                pgeyLado = [pgeyT for pgeyT in pgeyTr if (pgeyT[0] > pgeyCentro[0][1] if pgeySigno > 0 else pgeyT[1] < pgeyCentro[0][0])]
                if not pgeyLado:
                    continue
                pgeyT = min(pgeyLado, key=lambda pgeyT: abs((pgeyT[0] + pgeyT[1]) / 2))
                pgeyPts = self.pgeyCorte(pgeyY)
                pgeyM = (pgeyPts[:, 0] >= pgeyT[0]) & (pgeyPts[:, 0] <= pgeyT[1])
                if pgeyM.sum() < 6:
                    continue
                pgeyC = pgeyPts[pgeyM].mean(axis=0)
                pgeyCentros.append((pgeyC[0], pgeyY, pgeyC[1]))
            pgeyC = np.array(pgeyCentros)
            pgeyMedia = pgeyC.mean(axis=0)
            _, _, pgeyVt = np.linalg.svd(pgeyC - pgeyMedia)
            pgeyD = pgeyVt[0]
            if pgeyD[1] > 0:
                pgeyD = -pgeyD
            self.pgeyBrazo[pgeySigno] = (pgeyMedia, pgeyD / np.linalg.norm(pgeyD))

    def pgeyBrazoPunto(self, pgeySigno, pgeyS):
        pgeyMedia, pgeyD = self.pgeyBrazo[pgeySigno]
        pgeyY = self.pgeyHombro[pgeySigno][1]
        pgeyA = np.array((self.pgeyHombro[pgeySigno][0] * pgeySigno, pgeyY, self.pgeyTorsoZ(pgeyY)))
        pgeyS0 = float(np.dot(pgeyA - pgeyMedia, pgeyD))
        return pgeyMedia + pgeyD * (pgeyS0 + pgeyS)

    def pgeyRodilla(self):
        pgeyS, pgeyH = self.pgeySuelo, self.pgeyAlto
        pgeyMejor = None
        for pgeyY in np.arange(pgeyS + 0.22 * pgeyH, pgeyS + 0.34 * pgeyH, 0.003):
            pgeyP = self.pgeyPiernaCorte(pgeyY, 1)
            if len(pgeyP) < 3:
                continue
            pgeyA = float(pgeyP[:, 0].max() - pgeyP[:, 0].min())
            if pgeyMejor is None or pgeyA < pgeyMejor[1]:
                pgeyMejor = (float(pgeyY), pgeyA)
        return pgeyMejor[0]

    def pgeyBrazoEje(self, pgeySigno, pgeySMax):
        pgeyMedia, pgeyD = self.pgeyBrazo[pgeySigno]
        pgeyA0 = self.pgeyBrazoPunto(pgeySigno, 0.0)
        pgeyMuestras = []
        for pgeyY in np.arange(self.pgeyAxila - 0.005, self.pgeySuelo + 0.25 * self.pgeyAlto, -0.003):
            pgeyTr = self.pgeyTramosX(pgeyY)
            pgeyCentro = [pgeyT for pgeyT in pgeyTr if pgeyT[0] <= 0.0 <= pgeyT[1]]
            if not pgeyCentro:
                continue
            pgeyLado = [pgeyT for pgeyT in pgeyTr if (pgeyT[0] > pgeyCentro[0][1] if pgeySigno > 0 else pgeyT[1] < pgeyCentro[0][0])]
            if not pgeyLado:
                continue
            pgeyT = min(pgeyLado, key=lambda pgeyT: abs((pgeyT[0] + pgeyT[1]) / 2))
            pgeyPts = self.pgeyCorte(pgeyY)
            pgeyM = (pgeyPts[:, 0] >= pgeyT[0]) & (pgeyPts[:, 0] <= pgeyT[1])
            if pgeyM.sum() < 6:
                continue
            pgeyC = pgeyPts[pgeyM].mean(axis=0)
            pgeyP = np.array((pgeyC[0], pgeyY, pgeyC[1]))
            pgeyS = float(np.dot(pgeyP - pgeyA0, pgeyD))
            if pgeyS > pgeySMax + 0.03:
                break
            pgeyMuestras.append((pgeyS, pgeyP))
        pgeyMuestras.sort(key=lambda pgeyQ: pgeyQ[0])
        pgeySs = np.array([pgeyQ[0] for pgeyQ in pgeyMuestras])
        pgeyPs = np.array([pgeyQ[1] for pgeyQ in pgeyMuestras])
        pgeyGrilla = np.arange(0.0, pgeySMax + 0.0101, 0.01)
        pgeyMedidos = np.stack([np.interp(pgeyGrilla, pgeySs, pgeyPs[:, pgeyI]) for pgeyI in range(3)], axis=1)
        pgeySuave = np.array([pgeyMedidos[max(0, pgeyI - 2):pgeyI + 3].mean(axis=0) for pgeyI in range(len(pgeyGrilla))])
        pgeyRecta = pgeyA0 + np.outer(pgeyGrilla, pgeyD)
        pgeyW = np.clip((pgeyGrilla - 0.10) / 0.08, 0.0, 1.0)
        pgeyW = (pgeyW * pgeyW * (3 - 2 * pgeyW))[:, None]
        pgeyEje = pgeyRecta * (1 - pgeyW) + pgeySuave * pgeyW

        def pgeyEn(pgeyS):
            pgeyS = min(max(pgeyS, 0.0), pgeyGrilla[-1])
            pgeyP = np.array([np.interp(pgeyS, pgeyGrilla, pgeyEje[:, pgeyI]) for pgeyI in range(3)])
            pgeyA = np.array([np.interp(max(pgeyS - 0.02, 0.0), pgeyGrilla, pgeyEje[:, pgeyI]) for pgeyI in range(3)])
            pgeyB = np.array([np.interp(min(pgeyS + 0.02, pgeyGrilla[-1]), pgeyGrilla, pgeyEje[:, pgeyI]) for pgeyI in range(3)])
            pgeyT = pgeyB - pgeyA
            return pgeyP, pgeyT / np.linalg.norm(pgeyT)
        return pgeyEn

    def pgeyBrazoRadio(self, pgeySigno, pgeyS, pgeyDireccion):
        pgeyC = self.pgeyBrazoPunto(pgeySigno, pgeyS)
        pgeyHit = self.pgeyRayo(pgeyC, pgeyDireccion, 0.2)
        return pgeyHit[2] if pgeyHit else 0.05

    def _pgeyTorsoEje(self):
        self._pgeyEjes = []
        for pgeyY in np.arange(self.pgeyEntrepierna + 0.005, self.pgeyBaseCuello + 0.05, 0.005):
            pgeyP = self.pgeyTorsoCorte(pgeyY)
            if len(pgeyP):
                self._pgeyEjes.append((float(pgeyY), (pgeyP[:, 1].max() + pgeyP[:, 1].min()) / 2))
        self._pgeyEjes = np.array(self._pgeyEjes)

    def pgeyTorsoZ(self, pgeyY):
        return float(np.interp(pgeyY, self._pgeyEjes[:, 0], self._pgeyEjes[:, 1]))

    def pgeyPiernaCentro(self, pgeyY, pgeySigno):
        pgeyP = self.pgeyPiernaCorte(pgeyY, pgeySigno)
        if len(pgeyP) == 0:
            return None
        return np.array(((pgeyP[:, 0].max() + pgeyP[:, 0].min()) / 2, (pgeyP[:, 1].max() + pgeyP[:, 1].min()) / 2))

    def pgeyResumen(self):
        return {
            "suelo": round(self.pgeySuelo, 4), "alto": round(self.pgeyAlto, 4),
            "entrepierna": round(self.pgeyEntrepierna, 4), "axila": round(self.pgeyAxila, 4),
            "base_cuello": round(self.pgeyBaseCuello, 4),
            "cuello_centro_z": round(float(self.pgeyCuelloCentro[1]), 4),
            "cuello_radio": [round(float(pgeyR), 4) for pgeyR in self.pgeyCuelloRadio],
            "hombro_izq": [round(float(pgeyR), 4) for pgeyR in self.pgeyHombro[1]],
            "hombro_der": [round(float(pgeyR), 4) for pgeyR in self.pgeyHombro[-1]],
            "brazo_izq_dir": [round(float(pgeyR), 3) for pgeyR in self.pgeyBrazo[1][1]],
            "brazo_der_dir": [round(float(pgeyR), 3) for pgeyR in self.pgeyBrazo[-1][1]],
        }


def pgeyHull2d(pgeyPts):
    pgeyP = sorted(set(map(tuple, np.round(pgeyPts, 6))))
    if len(pgeyP) <= 2:
        return np.array(pgeyP)

    def pgeyCross(pgeyO, pgeyA, pgeyB):
        return (pgeyA[0] - pgeyO[0]) * (pgeyB[1] - pgeyO[1]) - (pgeyA[1] - pgeyO[1]) * (pgeyB[0] - pgeyO[0])

    pgeyLower, pgeyUpper = [], []
    for pgeyQ in pgeyP:
        while len(pgeyLower) >= 2 and pgeyCross(pgeyLower[-2], pgeyLower[-1], pgeyQ) <= 0:
            pgeyLower.pop()
        pgeyLower.append(pgeyQ)
    for pgeyQ in reversed(pgeyP):
        while len(pgeyUpper) >= 2 and pgeyCross(pgeyUpper[-2], pgeyUpper[-1], pgeyQ) <= 0:
            pgeyUpper.pop()
        pgeyUpper.append(pgeyQ)
    return np.array(pgeyLower[:-1] + pgeyUpper[:-1])


def pgeyRadioPolar(pgeyHull, pgeyCentro, pgeyAngulos):
    pgeyA = np.asarray(pgeyAngulos, dtype=float)
    pgeyDx, pgeyDz = np.sin(pgeyA), np.cos(pgeyA)
    pgeyOut = np.zeros(len(pgeyA))
    pgeyN = len(pgeyHull)
    for pgeyK in range(pgeyN):
        pgeyP = pgeyHull[pgeyK] - pgeyCentro
        pgeyE = pgeyHull[(pgeyK + 1) % pgeyN] - pgeyHull[pgeyK]
        pgeyDen = pgeyDx * (-pgeyE[1]) - pgeyDz * (-pgeyE[0])
        pgeyOk = np.abs(pgeyDen) > 1e-12
        pgeyDen = np.where(pgeyOk, pgeyDen, 1.0)
        pgeyT = (pgeyP[0] * (-pgeyE[1]) - pgeyP[1] * (-pgeyE[0])) / pgeyDen
        pgeyU = (pgeyDx * pgeyP[1] - pgeyDz * pgeyP[0]) / pgeyDen
        pgeyM = pgeyOk & (pgeyT > 0) & (pgeyU >= -1e-9) & (pgeyU <= 1 + 1e-9)
        pgeyOut = np.where(pgeyM, np.maximum(pgeyOut, pgeyT), pgeyOut)
    return pgeyOut


class PgeyTablaPolar:

    def __init__(self, pgeyCuerpo, pgeyY0, pgeyY1, pgeyPaso=0.004, pgeyRecorte=None, pgeyNAng=720):
        self.pgeyY0, self.pgeyPaso = pgeyY0, pgeyPaso
        self.pgeyYs = np.arange(pgeyY0, pgeyY1 + pgeyPaso, pgeyPaso)
        self.pgeyAng = np.linspace(0.0, 2 * math.pi, pgeyNAng, endpoint=False)
        self.pgeyZc = np.array([pgeyCuerpo.pgeyTorsoZ(pgeyY) for pgeyY in self.pgeyYs])
        pgeyFilas = []
        for pgeyY, pgeyZc in zip(self.pgeyYs, self.pgeyZc):
            pgeyP = pgeyCuerpo.pgeyTorsoCorte(pgeyY)
            pgeyXmax = pgeyRecorte(pgeyY) if callable(pgeyRecorte) else pgeyRecorte
            if pgeyXmax is not None and len(pgeyP):
                pgeyP = pgeyP[np.abs(pgeyP[:, 0]) <= pgeyXmax]
            pgeyFilas.append(pgeyRadioPolar(pgeyHull2d(pgeyP), np.array((0.0, pgeyZc)), self.pgeyAng) if len(pgeyP) >= 3 else np.zeros(pgeyNAng))
        self.pgeyR = np.array(pgeyFilas)

    def _pgeyIy(self, pgeyY):
        pgeyF = np.clip((np.asarray(pgeyY) - self.pgeyY0) / self.pgeyPaso, 0, len(self.pgeyYs) - 1.001)
        pgeyI = np.floor(pgeyF).astype(int)
        return pgeyI, pgeyF - pgeyI

    def pgeyCentroZ(self, pgeyY):
        pgeyI, pgeyF = self._pgeyIy(pgeyY)
        return self.pgeyZc[pgeyI] * (1 - pgeyF) + self.pgeyZc[pgeyI + 1] * pgeyF

    def pgeyRadio(self, pgeyY, pgeyTh):
        pgeyI, pgeyF = self._pgeyIy(pgeyY)
        pgeyA = (np.asarray(pgeyTh) % (2 * math.pi)) / (2 * math.pi) * len(self.pgeyAng)
        pgeyJ = np.floor(pgeyA).astype(int) % len(self.pgeyAng)
        pgeyG = pgeyA - np.floor(pgeyA)
        pgeyJ2 = (pgeyJ + 1) % len(self.pgeyAng)
        pgeyR0 = self.pgeyR[pgeyI, pgeyJ] * (1 - pgeyG) + self.pgeyR[pgeyI, pgeyJ2] * pgeyG
        pgeyR1 = self.pgeyR[pgeyI + 1, pgeyJ] * (1 - pgeyG) + self.pgeyR[pgeyI + 1, pgeyJ2] * pgeyG
        return pgeyR0 * (1 - pgeyF) + pgeyR1 * pgeyF

    def pgeyDentro(self, pgeyQ):
        pgeyQ = np.atleast_2d(pgeyQ)
        pgeyZc = self.pgeyCentroZ(pgeyQ[:, 1])
        pgeyTh = np.arctan2(pgeyQ[:, 0], pgeyQ[:, 2] - pgeyZc)
        return np.hypot(pgeyQ[:, 0], pgeyQ[:, 2] - pgeyZc) < self.pgeyRadio(pgeyQ[:, 1], pgeyTh)

    def pgeySalida(self, pgeyOrigen, pgeyDireccion, pgeyLargo=0.5, pgeyPaso=0.001):
        pgeyT = np.arange(pgeyPaso, pgeyLargo, pgeyPaso)
        pgeyQ = pgeyOrigen[None, :] + pgeyDireccion[None, :] * pgeyT[:, None]
        pgeyFuera = ~self.pgeyDentro(pgeyQ)
        pgeyK = int(np.argmax(pgeyFuera))
        if not pgeyFuera[pgeyK]:
            return None
        return pgeyOrigen + pgeyDireccion * (pgeyT[pgeyK] - pgeyPaso / 2)
