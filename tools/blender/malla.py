import bmesh
import bpy
import numpy as np

from cuerpo import pgeyABlender

PGEY_ISLAS = {
    "frente": (0.02, 0.02, 0.48, 0.54),
    "espalda": (0.52, 0.02, 0.98, 0.54),
    "manga_der": (0.02, 0.58, 0.30, 0.78),
    "manga_izq": (0.34, 0.58, 0.62, 0.78),
    "antebrazo_der": (0.02, 0.80, 0.30, 0.92),
    "antebrazo_izq": (0.34, 0.80, 0.62, 0.92),
    "puno_der": (0.66, 0.58, 0.81, 0.63),
    "puno_izq": (0.83, 0.58, 0.98, 0.63),
    "lado_der": (0.66, 0.66, 0.74, 0.90),
    "lado_izq": (0.78, 0.66, 0.86, 0.90),
    "cuello": (0.02, 0.93, 0.98, 0.97),
    "short_der_frente": (0.02, 0.02, 0.24, 0.78),
    "short_der_espalda": (0.26, 0.02, 0.48, 0.78),
    "short_izq_frente": (0.52, 0.02, 0.74, 0.78),
    "short_izq_espalda": (0.76, 0.02, 0.98, 0.78),
    "pretina": (0.02, 0.82, 0.98, 0.88),
    "franja_der": (0.02, 0.91, 0.48, 0.97),
    "franja_izq": (0.52, 0.91, 0.98, 0.97),
    "media_der": (0.02, 0.02, 0.48, 0.78),
    "media_izq": (0.52, 0.02, 0.98, 0.78),
    "banda_der": (0.02, 0.82, 0.48, 0.96),
    "banda_izq": (0.52, 0.82, 0.98, 0.96),
}


def pgeyIsla(pgeyNombre, pgeyU, pgeyV):
    pgeyR = PGEY_ISLAS[pgeyNombre]
    pgeyU = min(max(pgeyU, 0.0), 1.0)
    pgeyV = min(max(pgeyV, 0.0), 1.0)
    return pgeyR[0] + (pgeyR[2] - pgeyR[0]) * pgeyU, pgeyR[1] + (pgeyR[3] - pgeyR[1]) * pgeyV


def pgeySmoothstep(pgeyA, pgeyB, pgeyX):
    pgeyT = np.clip((pgeyX - pgeyA) / (pgeyB - pgeyA), 0.0, 1.0)
    return pgeyT * pgeyT * (3 - 2 * pgeyT)


class PgeyMalla:
    def __init__(self, pgeyZonas):
        self.pgeyZonas = list(pgeyZonas)
        self.pgeyV = []
        self.pgeyCaras = []
        self.pgeyGrupos = {}

    def pgeyVert(self, pgeyP):
        self.pgeyV.append(np.asarray(pgeyP, dtype=float))
        return len(self.pgeyV) - 1

    def pgeyCara(self, pgeyIdx, pgeyUvs, pgeyZona):
        self.pgeyCaras.append((tuple(pgeyIdx), tuple(pgeyUvs), pgeyZona))

    def pgeyPeso(self, pgeyGrupo, pgeyI, pgeyW):
        pgeyG = self.pgeyGrupos.setdefault(pgeyGrupo, {})
        pgeyG[pgeyI] = max(pgeyG.get(pgeyI, 0.0), float(pgeyW))

    def pgeyVecinos(self):
        pgeyN = len(self.pgeyV)
        pgeyAdy = [set() for _ in range(pgeyN)]
        for pgeyIdx, _, _ in self.pgeyCaras:
            for pgeyK in range(len(pgeyIdx)):
                pgeyA, pgeyB = pgeyIdx[pgeyK], pgeyIdx[(pgeyK + 1) % len(pgeyIdx)]
                pgeyAdy[pgeyA].add(pgeyB)
                pgeyAdy[pgeyB].add(pgeyA)
        return pgeyAdy

    def pgeyEmpujarAfuera(self, pgeyCuerpo, pgeyHolgura, pgeyFijos=()):
        pgeyMovidos = 0
        pgeyFijos = set(pgeyFijos)
        for pgeyI, pgeyP in enumerate(self.pgeyV):
            if pgeyI in pgeyFijos:
                continue
            pgeyQ, pgeyN, _ = pgeyCuerpo.pgeyCercano(pgeyP)
            pgeyD = float(np.dot(pgeyP - pgeyQ, pgeyN))
            if pgeyD < pgeyHolgura:
                self.pgeyV[pgeyI] = pgeyP + pgeyN * (pgeyHolgura - pgeyD)
                pgeyMovidos += 1
        return pgeyMovidos

    def pgeyOrientar(self, pgeyCuerpo):
        pgeyVueltas = 0
        for pgeyK, (pgeyIdx, pgeyUvs, pgeyZona) in enumerate(self.pgeyCaras):
            pgeyPts = [self.pgeyV[pgeyI] for pgeyI in pgeyIdx]
            pgeyNrm = np.zeros(3)
            for pgeyA in range(len(pgeyPts)):
                pgeyNrm += np.cross(pgeyPts[pgeyA], pgeyPts[(pgeyA + 1) % len(pgeyPts)])
            pgeyCentro = np.mean(pgeyPts, axis=0)
            pgeyQ, _, _ = pgeyCuerpo.pgeyCercano(pgeyCentro)
            if np.dot(pgeyNrm, pgeyCentro - pgeyQ) < 0:
                self.pgeyCaras[pgeyK] = (pgeyIdx[::-1], pgeyUvs[::-1], pgeyZona)
                pgeyVueltas += 1
        return pgeyVueltas

    def pgeyRelajar(self, pgeySolo, pgeyPasadas, pgeyFactor=0.5, pgeyRectas=None):

        pgeyRectas = pgeyRectas or {}

        def pgeySobre(pgeyPol, pgeyQ):
            pgeyMejor, pgeyDmin = None, 1e9
            for pgeyA, pgeyB in zip(pgeyPol, pgeyPol[1:]):
                pgeyT = float(np.clip(np.dot(pgeyQ - pgeyA, pgeyB - pgeyA) / max(np.dot(pgeyB - pgeyA, pgeyB - pgeyA), 1e-12), 0.0, 1.0))
                pgeyP = pgeyA + (pgeyB - pgeyA) * pgeyT
                pgeyD = float(np.linalg.norm(pgeyP - pgeyQ))
                if pgeyD < pgeyDmin:
                    pgeyMejor, pgeyDmin = pgeyP, pgeyD
            return pgeyMejor
        from mathutils import Vector
        from mathutils.bvhtree import BVHTree
        pgeyTris = []
        for pgeyIdx, _, _ in self.pgeyCaras:
            for pgeyK in range(1, len(pgeyIdx) - 1):
                pgeyTris.append((pgeyIdx[0], pgeyIdx[pgeyK], pgeyIdx[pgeyK + 1]))
        pgeyBvh = BVHTree.FromPolygons([Vector(pgeyP) for pgeyP in self.pgeyV], pgeyTris)
        pgeyAdy = self.pgeyVecinos()
        for _ in range(pgeyPasadas):
            pgeyNuevo = {}
            for pgeyI in pgeySolo:
                if pgeyI in pgeyRectas:
                    pgeyPol, pgeyAnt, pgeySig = pgeyRectas[pgeyI]
                    pgeyQ = self.pgeyV[pgeyI] + ((self.pgeyV[pgeyAnt] + self.pgeyV[pgeySig]) / 2 - self.pgeyV[pgeyI]) * pgeyFactor
                    pgeyNuevo[pgeyI] = pgeySobre(pgeyPol, pgeyQ)
                elif pgeyAdy[pgeyI]:
                    pgeyM = np.mean([self.pgeyV[pgeyJ] for pgeyJ in pgeyAdy[pgeyI]], axis=0)
                    pgeyQ = self.pgeyV[pgeyI] + (pgeyM - self.pgeyV[pgeyI]) * pgeyFactor
                    pgeyLoc, _, _, _ = pgeyBvh.find_nearest(Vector(pgeyQ))
                    pgeyNuevo[pgeyI] = np.array(pgeyLoc) if pgeyLoc is not None else pgeyQ
            for pgeyI, pgeyQ in pgeyNuevo.items():
                self.pgeyV[pgeyI] = pgeyQ

    def pgeySuavizar(self, pgeyPasadas, pgeyFactor=0.5, pgeySolo=None):
        pgeyAdy = self.pgeyVecinos()
        for _ in range(pgeyPasadas):
            pgeyNuevo = list(self.pgeyV)
            for pgeyI, pgeyVs in enumerate(pgeyAdy):
                if not pgeyVs or (pgeySolo is not None and pgeyI not in pgeySolo):
                    continue
                pgeyM = np.mean([self.pgeyV[pgeyJ] for pgeyJ in pgeyVs], axis=0)
                pgeyNuevo[pgeyI] = self.pgeyV[pgeyI] + (pgeyM - self.pgeyV[pgeyI]) * pgeyFactor
            self.pgeyV = pgeyNuevo

    def pgeyCrear(self, pgeyNombre, pgeyCuerpo=None):

        pgeyMe = bpy.data.meshes.new(pgeyNombre)
        pgeyBm = bmesh.new()
        pgeyBv = [pgeyBm.verts.new(pgeyABlender(pgeyP)) for pgeyP in self.pgeyV]
        pgeyBm.verts.ensure_lookup_table()
        pgeyCapa = pgeyBm.loops.layers.uv.new("UVMap")
        pgeyOmitidas = 0
        for pgeyIdx, pgeyUvs, pgeyZona in self.pgeyCaras:
            try:
                pgeyF = pgeyBm.faces.new([pgeyBv[pgeyK] for pgeyK in pgeyIdx])
            except ValueError:
                pgeyOmitidas += 1
                continue
            pgeyF.material_index = self.pgeyZonas.index(pgeyZona)
            for pgeyLoop, pgeyUv in zip(pgeyF.loops, pgeyUvs):
                pgeyLoop[pgeyCapa].uv = (pgeyUv[0], 1.0 - pgeyUv[1])
            pgeyF.smooth = True
        if pgeyCuerpo is not None:
            bmesh.ops.recalc_face_normals(pgeyBm, faces=pgeyBm.faces)
            pgeyVoto = 0
            for pgeyF in list(pgeyBm.faces)[:: max(1, len(pgeyBm.faces) // 400)]:
                pgeyC = pgeyF.calc_center_median()
                pgeyP = np.array((pgeyC.x, pgeyC.z, -pgeyC.y))
                pgeyQ, _, _ = pgeyCuerpo.pgeyCercano(pgeyP)
                pgeyNn = np.array((pgeyF.normal.x, pgeyF.normal.z, -pgeyF.normal.y))
                pgeyVoto += 1 if np.dot(pgeyNn, pgeyP - pgeyQ) > 0 else -1
            if pgeyVoto < 0:
                bmesh.ops.reverse_faces(pgeyBm, faces=pgeyBm.faces, flip_multires=False)
        pgeyBm.to_mesh(pgeyMe)
        pgeyBm.free()
        pgeyObj = bpy.data.objects.new(pgeyNombre, pgeyMe)
        bpy.context.scene.collection.objects.link(pgeyObj)
        for pgeyZ in self.pgeyZonas:
            pgeyMat = bpy.data.materials.get(pgeyZ) or bpy.data.materials.new(pgeyZ)
            pgeyMe.materials.append(pgeyMat)
        for pgeyG, pgeyPesos in self.pgeyGrupos.items():
            pgeyVg = pgeyObj.vertex_groups.new(name=pgeyG)
            pgeyPorPeso = {}
            for pgeyI, pgeyW in pgeyPesos.items():
                pgeyPorPeso.setdefault(round(pgeyW, 3), []).append(pgeyI)
            for pgeyW, pgeyIds in pgeyPorPeso.items():
                pgeyVg.add(pgeyIds, pgeyW, 'REPLACE')
        if pgeyOmitidas:
            print("AVISO: %d caras repetidas omitidas en %s" % (pgeyOmitidas, pgeyNombre))
        return pgeyObj
