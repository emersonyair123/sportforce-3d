"""Cuerpo de una persona de MakeHuman: importación, piel para colisiones y medidas.

Todo el cálculo de las prendas se hace en el espacio del modelo de la app (metros, Y arriba, +Z frente,
+X = izquierda de la persona). Blender trabaja con Z arriba: ``a_blender`` y ``a_modelo`` convierten.
"""
import math

import bpy
import numpy as np
from mathutils import Vector
from mathutils.bvhtree import BVHTree


def a_blender(p):
    """Modelo (x, y arriba, z frente) → Blender (x, -z, y)."""
    return Vector((p[0], -p[2], p[1]))


def a_modelo(v):
    return np.array((v[0], v[2], -v[1]))


def importar(ruta_obj):
    """Importa el OBJ de la persona y separa la piel (material con más caras). Devuelve (piel, otros)."""
    antes = set(bpy.data.objects)
    bpy.ops.wm.obj_import(filepath=str(ruta_obj), forward_axis='NEGATIVE_Z', up_axis='Y')
    nuevos = [o for o in bpy.data.objects if o not in antes]
    for o in nuevos:
        bpy.context.view_layer.objects.active = o
        o.select_set(True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    obj = nuevos[0]
    # Un objeto por material: piel, pelo, ojos.
    bpy.ops.object.select_all(action='DESELECT')
    obj.select_set(True)
    bpy.context.view_layer.objects.active = obj
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.separate(type='MATERIAL')
    bpy.ops.object.mode_set(mode='OBJECT')
    partes = [o for o in bpy.data.objects if o.type == 'MESH' and o not in antes]
    piel = max(partes, key=lambda o: len(o.data.polygons))
    piel.name = "persona_piel"
    otros = [o for o in partes if o is not piel]
    for i, o in enumerate(otros):
        o.name = "persona_" + (o.active_material.name if o.active_material else str(i))
    return piel, otros


class Cuerpo:
    """Malla de la piel en espacio del modelo, con un BVH para rayos y cortes horizontales."""

    def __init__(self, piel):
        me = piel.data
        self.v = np.array([a_modelo(x.co) for x in me.vertices])
        tris = []
        for p in me.polygons:
            vs = list(p.vertices)
            for k in range(1, len(vs) - 1):
                tris.append((vs[0], vs[k], vs[k + 1]))
        self.t = np.array(tris, dtype=np.int64)
        self.bvh = BVHTree.FromPolygons([Vector(x) for x in self.v], [tuple(t) for t in self.t])
        self.suelo = float(self.v[:, 1].min())
        self.alto = float(self.v[:, 1].max()) - self.suelo
        self._medir()

    # ------------------------------------------------------------ consultas

    def rayo(self, origen, direccion, dist=2.0):
        """Primer choque con la piel: (punto, normal, distancia) o None."""
        d = Vector(direccion).normalized()
        loc, nrm, _, dist_hit = self.bvh.ray_cast(Vector(origen), d, dist)
        if loc is None:
            return None
        return np.array(loc), np.array(nrm), dist_hit

    def cercano(self, p):
        loc, nrm, _, d = self.bvh.find_nearest(Vector(p))
        return np.array(loc), np.array(nrm), d

    def corte(self, y):
        """Puntos (x, z) donde las aristas de los triángulos cruzan el plano horizontal y."""
        v = self.v
        a, b, c = v[self.t[:, 0]], v[self.t[:, 1]], v[self.t[:, 2]]
        pts = []
        for p, q in ((a, b), (b, c), (c, a)):
            ya, yb = p[:, 1], q[:, 1]
            m = (ya - y) * (yb - y) < 0
            k = (y - ya[m]) / (yb[m] - ya[m])
            pts.append(p[m][:, [0, 2]] + (q[m][:, [0, 2]] - p[m][:, [0, 2]]) * k[:, None])
        return np.concatenate(pts) if pts else np.zeros((0, 2))

    def segmentos(self, y):
        """Segmentos del contorno en el plano y: array (n, 2, 2) de pares de puntos (x, z), uno por triángulo."""
        v = self.v
        a, b, c = v[self.t[:, 0]], v[self.t[:, 1]], v[self.t[:, 2]]
        lados = []
        for p, q in ((a, b), (b, c), (c, a)):
            ya, yb = p[:, 1], q[:, 1]
            m = (ya - y) * (yb - y) < 0
            k = np.where(m, (y - ya) / np.where(m, yb - ya, 1.0), 0.0)
            pt = p[:, [0, 2]] + (q[:, [0, 2]] - p[:, [0, 2]]) * k[:, None]
            lados.append((m, pt))
        segs = []
        for i in range(3):
            for j in range(i + 1, 3):
                m = lados[i][0] & lados[j][0]
                segs.append(np.stack((lados[i][1][m], lados[j][1][m]), axis=1))
        return np.concatenate(segs) if segs else np.zeros((0, 2, 2))

    def tramos_x(self, y, hueco=0.008):
        """Tramos de x ocupados por el contorno en el plano y (lista de [x0, x1] ordenados).

        Une los intervalos de x de cada segmento: en una malla de pocos polígonos los puntos sueltos dejan
        huecos falsos donde la superficie es casi horizontal (hombros).
        """
        segs = self.segmentos(y)
        if len(segs) == 0:
            return []
        iv = np.stack((segs[:, :, 0].min(axis=1), segs[:, :, 0].max(axis=1)), axis=1)
        iv = iv[np.argsort(iv[:, 0])]
        tramos = [[iv[0, 0], iv[0, 1]]]
        for x0, x1 in iv[1:]:
            if x0 - tramos[-1][1] > hueco:
                tramos.append([x0, x1])
            else:
                tramos[-1][1] = max(tramos[-1][1], x1)
        return tramos

    def torso_corte(self, y):
        """Puntos (x, z) del torso a la altura y: el tramo de x que contiene a x = 0 (sin brazos)."""
        pts = self.corte(y)
        for t in self.tramos_x(y):
            if t[0] <= 0.0 <= t[1]:
                m = (pts[:, 0] >= t[0] - 1e-6) & (pts[:, 0] <= t[1] + 1e-6)
                return pts[m]
        return np.zeros((0, 2))

    def pierna_corte(self, y, signo):
        """Puntos (x, z) de la pierna de un lado (signo +1 izquierda, -1 derecha) bajo la entrepierna."""
        pts = self.corte(y)
        if len(pts) == 0:
            return pts
        pts = pts[pts[:, 0] * signo > 0]
        # La mano (pose A) puede cruzar el plano: se queda el tramo más cercano al centro.
        tramos = [t for t in self.tramos_x(y) if (t[0] + t[1]) / 2 * signo > 0]
        if not tramos:
            return np.zeros((0, 2))
        t = min(tramos, key=lambda t: abs((t[0] + t[1]) / 2))
        m = (pts[:, 0] >= t[0] - 1e-6) & (pts[:, 0] <= t[1] + 1e-6)
        return pts[m]

    # ------------------------------------------------------------ medidas

    def _medir(self):
        s, h = self.suelo, self.alto
        ys = np.arange(s + 0.30 * h, s + 0.70 * h, 0.002)
        # Entrepierna: la altura más baja en que el plano corta la piel en x = 0.
        self.entrepierna = None
        for y in ys:
            if any(t[0] <= 0.0 <= t[1] for t in self.tramos_x(y)):
                self.entrepierna = float(y)
                break
        # Axila: la más alta en que, en ambos lados, el torso y el brazo están separados.
        self.axila = None
        for y in np.arange(s + 0.84 * h, s + 0.66 * h, -0.002):
            tr = self.tramos_x(y)
            centro = [t for t in tr if t[0] <= 0.0 <= t[1]]
            if centro and sum(1 for t in tr if t[0] > centro[0][1]) and sum(1 for t in tr if t[1] < centro[0][0]):
                self.axila = float(y)
                break
        # Cuello: ancho del torso por altura, de la axila hacia arriba; base del cuello donde el ancho se
        # acerca al del cuello (medido 4 % de la altura más arriba).
        def ancho(y):
            p = self.torso_corte(y)
            return (p[:, 0].max() - p[:, 0].min()) / 2 if len(p) else 0.0
        y_cuello = None
        anchos = [(float(y), ancho(y)) for y in np.arange(self.axila, s + 0.92 * h, 0.002)]
        minimo = min(a for y, a in anchos if y > self.axila + 0.08 * h)
        for y, a in anchos:
            if a < minimo * 1.35:
                y_cuello = y
                break
        self.base_cuello = y_cuello
        p = self.torso_corte(y_cuello + 0.02 * h)
        self.cuello_centro = np.array([0.0, (p[:, 1].max() + p[:, 1].min()) / 2])
        self.cuello_radio = np.array([(p[:, 0].max() - p[:, 0].min()) / 2, (p[:, 1].max() - p[:, 1].min()) / 2])
        # Perfil superior de los hombros: altura de la piel vista desde arriba en x, en el centro del torso.
        self.hombro = {}
        for signo in (1, -1):
            perfil = []
            for x in np.arange(0.0, 0.30, 0.004):
                hit = self.rayo((x * signo, s + h + 0.1, self.cuello_centro[1]), (0, -1, 0), 1.0)
                if hit is not None and hit[0][1] > self.axila:
                    perfil.append((float(x), float(hit[0][1])))
            # Acromion: donde la pendiente del hombro pasa a ser la del brazo (más de 35° hacia abajo); por debajo
            # de la base del cuello (la pendiente del cuello al trapecio también es empinada).
            perfil = [q for q in perfil if q[1] <= self.base_cuello + 0.005]
            acromion = perfil[-1]
            for (x0, y0), (x1, y1) in zip(perfil, perfil[1:]):
                if x0 > self.cuello_radio[0] * 1.6 and (y0 - y1) / (x1 - x0) > math.tan(math.radians(35)):
                    acromion = (x0, y0)
                    break
            self.hombro[signo] = np.array(acromion)
        self._brazos()
        self._torso_eje()

    def _brazos(self):
        """Eje de cada brazo (recta ajustada a los centros de sus cortes horizontales bajo la axila)."""
        s, h = self.suelo, self.alto
        self.brazo = {}
        for signo in (1, -1):
            centros = []
            for y in np.arange(self.axila - 0.005, self.axila - 0.14 * h, -0.004):
                tr = self.tramos_x(y)
                centro = [t for t in tr if t[0] <= 0.0 <= t[1]]
                if not centro:
                    continue
                lado = [t for t in tr if (t[0] > centro[0][1] if signo > 0 else t[1] < centro[0][0])]
                if not lado:
                    continue
                t = min(lado, key=lambda t: abs((t[0] + t[1]) / 2))
                pts = self.corte(y)
                m = (pts[:, 0] >= t[0]) & (pts[:, 0] <= t[1])
                if m.sum() < 6:
                    continue
                c = pts[m].mean(axis=0)
                centros.append((c[0], y, c[1]))
            c = np.array(centros)
            media = c.mean(axis=0)
            _, _, vt = np.linalg.svd(c - media)
            d = vt[0]
            if d[1] > 0:
                d = -d  # hacia la mano (abajo)
            self.brazo[signo] = (media, d / np.linalg.norm(d))

    def brazo_punto(self, signo, s):
        """Punto del eje del brazo a s metros del hombro (s = 0 en el acromion proyectado sobre el eje)."""
        media, d = self.brazo[signo]
        y = self.hombro[signo][1]
        a = np.array((self.hombro[signo][0] * signo, y, self.torso_z(y)))
        s0 = float(np.dot(a - media, d))
        return media + d * (s0 + s)

    def rodilla(self):
        """Altura de la rodilla: la sección más angosta de la pierna entre el 22 % y el 34 % del alto."""
        s, h = self.suelo, self.alto
        mejor = None
        for y in np.arange(s + 0.22 * h, s + 0.34 * h, 0.003):
            p = self.pierna_corte(y, 1)
            if len(p) < 3:
                continue
            a = float(p[:, 0].max() - p[:, 0].min())
            if mejor is None or a < mejor[1]:
                mejor = (float(y), a)
        return mejor[0]

    def brazo_eje(self, signo, s_max):
        """Eje del brazo hasta ``s_max`` (m desde el acromion) para la manga larga: la recta del brazo superior
        (``brazo_punto``) cerca del hombro y, desde ahí, los centros de los cortes horizontales suavizados, que
        siguen el codo y el antebrazo (en pose A el antebrazo se aparta unos 2–3 cm de la recta). Devuelve una
        función s → (punto, tangente)."""
        media, d = self.brazo[signo]
        a0 = self.brazo_punto(signo, 0.0)
        muestras = []
        for y in np.arange(self.axila - 0.005, self.suelo + 0.25 * self.alto, -0.003):
            tr = self.tramos_x(y)
            centro = [t for t in tr if t[0] <= 0.0 <= t[1]]
            if not centro:
                continue
            lado = [t for t in tr if (t[0] > centro[0][1] if signo > 0 else t[1] < centro[0][0])]
            if not lado:
                continue
            t = min(lado, key=lambda t: abs((t[0] + t[1]) / 2))
            pts = self.corte(y)
            m = (pts[:, 0] >= t[0]) & (pts[:, 0] <= t[1])
            if m.sum() < 6:
                continue
            c = pts[m].mean(axis=0)
            p = np.array((c[0], y, c[1]))
            s = float(np.dot(p - a0, d))
            if s > s_max + 0.03:
                break
            muestras.append((s, p))
        muestras.sort(key=lambda q: q[0])
        ss = np.array([q[0] for q in muestras])
        ps = np.array([q[1] for q in muestras])
        # Suavizado (media móvil de ~5 cm): los cortes del codo y la muñeca no son elipses perfectas.
        grilla = np.arange(0.0, s_max + 0.0101, 0.01)
        medidos = np.stack([np.interp(grilla, ss, ps[:, i]) for i in range(3)], axis=1)
        suave = np.array([medidos[max(0, i - 2):i + 3].mean(axis=0) for i in range(len(grilla))])
        recta = a0 + np.outer(grilla, d)
        w = np.clip((grilla - 0.10) / 0.08, 0.0, 1.0)
        w = (w * w * (3 - 2 * w))[:, None]
        eje = recta * (1 - w) + suave * w

        def en(s):
            s = min(max(s, 0.0), grilla[-1])
            p = np.array([np.interp(s, grilla, eje[:, i]) for i in range(3)])
            a = np.array([np.interp(max(s - 0.02, 0.0), grilla, eje[:, i]) for i in range(3)])
            b = np.array([np.interp(min(s + 0.02, grilla[-1]), grilla, eje[:, i]) for i in range(3)])
            t = b - a
            return p, t / np.linalg.norm(t)
        return en

    def brazo_radio(self, signo, s, direccion):
        """Distancia del eje del brazo a la piel en una dirección perpendicular."""
        c = self.brazo_punto(signo, s)
        hit = self.rayo(c, direccion, 0.2)
        return hit[2] if hit else 0.05

    def _torso_eje(self):
        """Centro (z) del torso por altura, de la entrepierna a la base del cuello."""
        self._ejes = []
        for y in np.arange(self.entrepierna + 0.005, self.base_cuello + 0.05, 0.005):
            p = self.torso_corte(y)
            if len(p):
                self._ejes.append((float(y), (p[:, 1].max() + p[:, 1].min()) / 2))
        self._ejes = np.array(self._ejes)

    def torso_z(self, y):
        return float(np.interp(y, self._ejes[:, 0], self._ejes[:, 1]))

    def pierna_centro(self, y, signo):
        p = self.pierna_corte(y, signo)
        if len(p) == 0:
            return None
        return np.array(((p[:, 0].max() + p[:, 0].min()) / 2, (p[:, 1].max() + p[:, 1].min()) / 2))

    def resumen(self):
        return {
            "suelo": round(self.suelo, 4), "alto": round(self.alto, 4),
            "entrepierna": round(self.entrepierna, 4), "axila": round(self.axila, 4),
            "base_cuello": round(self.base_cuello, 4),
            "cuello_centro_z": round(float(self.cuello_centro[1]), 4),
            "cuello_radio": [round(float(r), 4) for r in self.cuello_radio],
            "hombro_izq": [round(float(r), 4) for r in self.hombro[1]],
            "hombro_der": [round(float(r), 4) for r in self.hombro[-1]],
            "brazo_izq_dir": [round(float(r), 3) for r in self.brazo[1][1]],
            "brazo_der_dir": [round(float(r), 3) for r in self.brazo[-1][1]],
        }


def hull2d(pts):
    """Envolvente convexa (Andrew) de puntos 2D, en sentido antihorario."""
    p = sorted(set(map(tuple, np.round(pts, 6))))
    if len(p) <= 2:
        return np.array(p)

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    lower, upper = [], []
    for q in p:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], q) <= 0:
            lower.pop()
        lower.append(q)
    for q in reversed(p):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], q) <= 0:
            upper.pop()
        upper.append(q)
    return np.array(lower[:-1] + upper[:-1])


def radio_polar(hull, centro, angulos):
    """Radio de un polígono convexo (x, z) desde ``centro`` en cada ángulo (0 = +z frente, 90° = +x)."""
    a = np.asarray(angulos, dtype=float)
    dx, dz = np.sin(a), np.cos(a)
    out = np.zeros(len(a))
    n = len(hull)
    for k in range(n):
        p = hull[k] - centro
        e = hull[(k + 1) % n] - hull[k]
        den = dx * (-e[1]) - dz * (-e[0])
        ok = np.abs(den) > 1e-12
        den = np.where(ok, den, 1.0)
        t = (p[0] * (-e[1]) - p[1] * (-e[0])) / den
        u = (dx * p[1] - dz * p[0]) / den
        m = ok & (t > 0) & (u >= -1e-9) & (u <= 1 + 1e-9)
        out = np.where(m, np.maximum(out, t), out)
    return out


class TablaPolar:
    """Radio de la envolvente convexa del torso por altura y ángulo, alrededor del centro (0, z) de cada altura.

    ``recorte``: |x| máximo de los puntos del corte (sin los brazos sobre la axila); un número o una función de y
    (None = sin recorte a esa altura).
    """

    def __init__(self, cuerpo, y0, y1, paso=0.004, recorte=None, n_ang=720):
        self.y0, self.paso = y0, paso
        self.ys = np.arange(y0, y1 + paso, paso)
        self.ang = np.linspace(0.0, 2 * math.pi, n_ang, endpoint=False)
        self.zc = np.array([cuerpo.torso_z(y) for y in self.ys])
        filas = []
        for y, zc in zip(self.ys, self.zc):
            p = cuerpo.torso_corte(y)
            xmax = recorte(y) if callable(recorte) else recorte
            if xmax is not None and len(p):
                # Se descartan (no se aplastan contra el límite): así la envolvente no toma la profundidad del
                # brazo y la espalda no hace escalón sobre la axila.
                p = p[np.abs(p[:, 0]) <= xmax]
            filas.append(radio_polar(hull2d(p), np.array((0.0, zc)), self.ang) if len(p) >= 3 else np.zeros(n_ang))
        self.R = np.array(filas)

    def _iy(self, y):
        f = np.clip((np.asarray(y) - self.y0) / self.paso, 0, len(self.ys) - 1.001)
        i = np.floor(f).astype(int)
        return i, f - i

    def centro_z(self, y):
        i, f = self._iy(y)
        return self.zc[i] * (1 - f) + self.zc[i + 1] * f

    def radio(self, y, th):
        i, f = self._iy(y)
        a = (np.asarray(th) % (2 * math.pi)) / (2 * math.pi) * len(self.ang)
        j = np.floor(a).astype(int) % len(self.ang)
        g = a - np.floor(a)
        j2 = (j + 1) % len(self.ang)
        r0 = self.R[i, j] * (1 - g) + self.R[i, j2] * g
        r1 = self.R[i + 1, j] * (1 - g) + self.R[i + 1, j2] * g
        return r0 * (1 - f) + r1 * f

    def dentro(self, q):
        """True para los puntos (n, 3) que quedan dentro de la envolvente de su altura."""
        q = np.atleast_2d(q)
        zc = self.centro_z(q[:, 1])
        th = np.arctan2(q[:, 0], q[:, 2] - zc)
        return np.hypot(q[:, 0], q[:, 2] - zc) < self.radio(q[:, 1], th)

    def salida(self, origen, direccion, largo=0.5, paso=0.001):
        """Primer punto (±0,5 mm) de un rayo que parte de dentro y sale de la envolvente; None si no sale."""
        t = np.arange(paso, largo, paso)
        q = origen[None, :] + direccion[None, :] * t[:, None]
        fuera = ~self.dentro(q)
        k = int(np.argmax(fuera))
        if not fuera[k]:
            return None
        return origen + direccion * (t[k] - paso / 2)
