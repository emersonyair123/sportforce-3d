import bmesh
import bpy
import numpy as np

from cuerpo import a_blender

ISLAS = {
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


def isla(nombre, u, v):
    r = ISLAS[nombre]
    u = min(max(u, 0.0), 1.0)
    v = min(max(v, 0.0), 1.0)
    return r[0] + (r[2] - r[0]) * u, r[1] + (r[3] - r[1]) * v


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


class Malla:
    def __init__(self, zonas):
        self.zonas = list(zonas)
        self.v = []
        self.caras = []
        self.grupos = {}

    def vert(self, p):
        self.v.append(np.asarray(p, dtype=float))
        return len(self.v) - 1

    def cara(self, idx, uvs, zona):
        self.caras.append((tuple(idx), tuple(uvs), zona))

    def peso(self, grupo, i, w):
        g = self.grupos.setdefault(grupo, {})
        g[i] = max(g.get(i, 0.0), float(w))

    def vecinos(self):
        n = len(self.v)
        ady = [set() for _ in range(n)]
        for idx, _, _ in self.caras:
            for k in range(len(idx)):
                a, b = idx[k], idx[(k + 1) % len(idx)]
                ady[a].add(b)
                ady[b].add(a)
        return ady

    def empujar_afuera(self, cuerpo, holgura, fijos=()):
        movidos = 0
        fijos = set(fijos)
        for i, p in enumerate(self.v):
            if i in fijos:
                continue
            q, n, _ = cuerpo.cercano(p)
            d = float(np.dot(p - q, n))
            if d < holgura:
                self.v[i] = p + n * (holgura - d)
                movidos += 1
        return movidos

    def orientar(self, cuerpo):
        vueltas = 0
        for k, (idx, uvs, zona) in enumerate(self.caras):
            pts = [self.v[i] for i in idx]
            nrm = np.zeros(3)
            for a in range(len(pts)):
                nrm += np.cross(pts[a], pts[(a + 1) % len(pts)])
            centro = np.mean(pts, axis=0)
            q, _, _ = cuerpo.cercano(centro)
            if np.dot(nrm, centro - q) < 0:
                self.caras[k] = (idx[::-1], uvs[::-1], zona)
                vueltas += 1
        return vueltas

    def relajar(self, solo, pasadas, factor=0.5, rectas=None):

        rectas = rectas or {}

        def sobre(pol, q):
            mejor, dmin = None, 1e9
            for a, b in zip(pol, pol[1:]):
                t = float(np.clip(np.dot(q - a, b - a) / max(np.dot(b - a, b - a), 1e-12), 0.0, 1.0))
                p = a + (b - a) * t
                d = float(np.linalg.norm(p - q))
                if d < dmin:
                    mejor, dmin = p, d
            return mejor
        from mathutils import Vector
        from mathutils.bvhtree import BVHTree
        tris = []
        for idx, _, _ in self.caras:
            for k in range(1, len(idx) - 1):
                tris.append((idx[0], idx[k], idx[k + 1]))
        bvh = BVHTree.FromPolygons([Vector(p) for p in self.v], tris)
        ady = self.vecinos()
        for _ in range(pasadas):
            nuevo = {}
            for i in solo:
                if i in rectas:
                    pol, ant, sig = rectas[i]
                    q = self.v[i] + ((self.v[ant] + self.v[sig]) / 2 - self.v[i]) * factor
                    nuevo[i] = sobre(pol, q)
                elif ady[i]:
                    m = np.mean([self.v[j] for j in ady[i]], axis=0)
                    q = self.v[i] + (m - self.v[i]) * factor
                    loc, _, _, _ = bvh.find_nearest(Vector(q))
                    nuevo[i] = np.array(loc) if loc is not None else q
            for i, q in nuevo.items():
                self.v[i] = q

    def suavizar(self, pasadas, factor=0.5, solo=None):
        ady = self.vecinos()
        for _ in range(pasadas):
            nuevo = list(self.v)
            for i, vs in enumerate(ady):
                if not vs or (solo is not None and i not in solo):
                    continue
                m = np.mean([self.v[j] for j in vs], axis=0)
                nuevo[i] = self.v[i] + (m - self.v[i]) * factor
            self.v = nuevo

    def crear(self, nombre, cuerpo=None):

        me = bpy.data.meshes.new(nombre)
        bm = bmesh.new()
        bv = [bm.verts.new(a_blender(p)) for p in self.v]
        bm.verts.ensure_lookup_table()
        capa = bm.loops.layers.uv.new("UVMap")
        omitidas = 0
        for idx, uvs, zona in self.caras:
            try:
                f = bm.faces.new([bv[k] for k in idx])
            except ValueError:
                omitidas += 1
                continue
            f.material_index = self.zonas.index(zona)
            for loop, uv in zip(f.loops, uvs):
                loop[capa].uv = (uv[0], 1.0 - uv[1])
            f.smooth = True
        if cuerpo is not None:
            bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
            voto = 0
            for f in list(bm.faces)[:: max(1, len(bm.faces) // 400)]:
                c = f.calc_center_median()
                p = np.array((c.x, c.z, -c.y))
                q, _, _ = cuerpo.cercano(p)
                nn = np.array((f.normal.x, f.normal.z, -f.normal.y))
                voto += 1 if np.dot(nn, p - q) > 0 else -1
            if voto < 0:
                bmesh.ops.reverse_faces(bm, faces=bm.faces, flip_multires=False)
        bm.to_mesh(me)
        bm.free()
        obj = bpy.data.objects.new(nombre, me)
        bpy.context.scene.collection.objects.link(obj)
        for z in self.zonas:
            mat = bpy.data.materials.get(z) or bpy.data.materials.new(z)
            me.materials.append(mat)
        for g, pesos in self.grupos.items():
            vg = obj.vertex_groups.new(name=g)
            por_peso = {}
            for i, w in pesos.items():
                por_peso.setdefault(round(w, 3), []).append(i)
            for w, ids in por_peso.items():
                vg.add(ids, w, 'REPLACE')
        if omitidas:
            print("AVISO: %d caras repetidas omitidas en %s" % (omitidas, nombre))
        return obj
