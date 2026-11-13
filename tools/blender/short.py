import math

import numpy as np

from cuerpo import TablaPolar, hull2d, radio_polar
from malla import Malla, isla, smoothstep

ZONAS = ["shorts_base", "shorts_stripe", "shorts_waist"]
AREA_H = 0.35


class Corte:
    arista = 0.012
    cintura = 0.056
    largo_entrepierna = 0.21
    holgura_cintura = 0.008
    holgura_cadera = 0.016
    holgura_pierna = 0.020
    holgura_basta = 0.032
    caida = 0.85
    pretina = 0.04
    franja_columnas = 2
    largo_ref = None
    v_tope = 0.12


def construir(c, corte=Corte):
    k = c.alto / 1.86
    e = corte.arista
    y_w = c.entrepierna + corte.cintura * c.alto
    y_cr = c.entrepierna + 0.004
    y_h = c.entrepierna - corte.largo_entrepierna * k
    pelvis = TablaPolar(c, y_cr - 0.004, y_w + 0.01)

    y_hip = (y_w + y_cr) / 2
    densos = np.radians(np.arange(0.0, 360.25, 0.25))
    rr = pelvis.radio(np.full(len(densos), y_hip), densos)
    pts = np.stack((rr * np.sin(densos), rr * np.cos(densos)), axis=1)
    arco = np.concatenate(([0.0], np.cumsum(np.linalg.norm(np.diff(pts, axis=0), axis=1))))

    def arco_de(t):
        return float(np.interp(t, densos, arco))

    def th_de(a):
        return float(np.interp(a, arco, densos))

    cuartos = [0.0, math.pi / 2, math.pi]
    th_izq = []
    for a, b in zip(cuartos, cuartos[1:]):
        nq = max(4, round((arco_de(b) - arco_de(a)) / e))
        th_izq += [th_de(arco_de(a) + (arco_de(b) - arco_de(a)) * i / nq) for i in range(nq)]
    th_izq.append(math.pi)
    n = 2 * (len(th_izq) - 1)
    th = np.zeros(n)
    th[: len(th_izq)] = th_izq
    for j in range(len(th_izq), n):
        th[j] = 2 * math.pi - th[n - j]
    j90 = th_izq.index(min(th_izq, key=lambda t: abs(t - math.pi / 2)))
    mitad = n // 2

    r_c = max(4, round((y_w - y_cr) / e))
    P = {}

    def radio_caida(y, t):
        ry = float(pelvis.radio(y, t))
        ys = np.arange(y, y_w, 0.006)
        ru = float(max(pelvis.radio(ys, np.full(len(ys), t)))) if len(ys) else ry
        return ry + corte.caida * max(0.0, ru - ry)

    for r in range(r_c + 1):
        y = y_w - (y_w - y_cr) * r / r_c
        hol = corte.holgura_cintura + (corte.holgura_cadera - corte.holgura_cintura) * smoothstep(y_w, y_w - 0.05, y)
        zc = float(pelvis.centro_z(y))
        for j in range(n):
            rad = radio_caida(y, th[j]) + hol
            P[(r, j)] = np.array((math.sin(th[j]) * rad, y, zc + math.cos(th[j]) * rad))

    m = Malla(ZONAS)
    ids = {key: m.vert(q) for key, q in P.items()}

    F, B = P[(r_c, 0)], P[(r_c, mitad)]
    n_s = max(3, round(np.linalg.norm(F - B) / e) - 1)
    costura = []
    for i in range(1, n_s + 1):
        t = i / (n_s + 1)
        q = B + (F - B) * t
        q[1] -= 0.012 * math.sin(math.pi * t)
        costura.append(m.vert(q))
    m.empujar_afuera(c, 0.006, fijos=set(range(len(m.v))) - set(costura))

    n_l = max(6, round((y_cr - y_h) / e))
    piernas = {}
    for lado in (1, -1):
        cols = list(range(0, mitad + 1)) if lado > 0 else [0] + list(range(n - 1, mitad - 1, -1))
        anillo0 = [ids[(r_c, j)] for j in cols] + costura
        D = [m.v[i].copy() for i in anillo0]
        largo_d = np.concatenate(([0.0], np.cumsum([np.linalg.norm(D[(i + 1) % len(D)] - D[i])
                                                     for i in range(len(D))])))
        q_m = largo_d[:-1] / largo_d[-1]
        filas = [anillo0]
        for r in range(1, n_l + 1):
            y = y_cr - (y_cr - y_h) * r / n_l
            w = float(smoothstep(0.0, 0.07, y_cr - y))
            anillo = _anillo_pierna(c, lado, y, y_cr, q_m, D[0], corte, smoothstep(y_cr - 0.03, y_h, y))
            fila = []
            for i, q in enumerate(D):
                d = np.array((q[0], y, q[2]))
                fila.append(m.vert(d * (1 - w) + anillo[i] * w))
            filas.append(fila)
        piernas[lado] = (cols, filas)

    m_lat = j90
    m_in = mitad + 1 + n_s // 2
    pelvis_pierna = {1: list(range(0, mitad + 1)), -1: [0] + list(range(n - 1, mitad - 1, -1))}

    def arcos(fila):
        pos = [m.v[i] for i in fila]
        L = len(pos)
        cerrado = L > mitad + 1

        def recorre(desde, paso, hasta):
            out, acc, i = {desde: 0.0}, 0.0, desde
            while i != hasta:
                i2 = (i + paso) % L
                acc += float(np.linalg.norm(pos[i2] - pos[i]))
                out[i2] = acc
                i = i2
            return out
        fr = recorre(0, +1, m_lat)
        es = recorre(mitad, -1, m_lat)
        if cerrado:
            fr.update({i: -a for i, a in recorre(0, -1, m_in).items() if i != 0})
            es.update({i: -a for i, a in recorre(mitad, +1, m_in).items() if i != mitad})
        return fr, es

    v_esc = (y_w - y_h) / (0.97 * AREA_H)
    if corte.largo_ref is not None:
        paso_v = (y_w - (c.entrepierna - corte.largo_ref * k)) / 0.97
        v_cint = 0.97 - (y_w - y_h) / paso_v

    def v_de(y):
        if corte.largo_ref is None:
            return (y_w - y) / (AREA_H * v_esc)
        v = 0.97 - (y - y_h) / paso_v
        if v_cint < 0 and v < corte.v_tope:
            v = corte.v_tope * (v - v_cint) / (corte.v_tope - v_cint)
        return v

    def uv_base(lado, fila_ids, i, delante, fr, es):
        y = m.v[fila_ids[i]][1]
        v = v_de(y)
        nom = ("short_izq_" if lado > 0 else "short_der_") + ("frente" if delante else "espalda")
        if delante:
            s = fr[i]
            lim = fr[m_lat] if s >= 0 else -min(fr.values())
            return isla(nom, 0.5 + lado * 0.5 * s / max(lim, 1e-6), v)
        s = es[i]
        lim = es[m_lat] if s >= 0 else -min(es.values())
        return isla(nom, 0.5 - lado * 0.5 * s / max(lim, 1e-6), v)

    r_w = max(2, round(corte.pretina / e))
    fc = corte.franja_columnas
    for lado in (1, -1):
        cols, filas = piernas[lado]
        todas = [[ids[(r, j)] for j in pelvis_pierna[lado]] for r in range(r_c)] + filas
        info = [arcos(f) for f in todas]
        for r in range(len(todas) - 1):
            a, b = todas[r], todas[r + 1]
            cerrado = len(a) == len(b) and len(a) > mitad + 1
            for i in range(len(a) if cerrado else mitad):
                i2 = (i + 1) % len(a)
                if lado > 0:
                    vs = [(r, i), (r + 1, i), (r + 1, i2), (r, i2)]
                else:
                    vs = [(r, i), (r, i2), (r + 1, i2), (r + 1, i)]
                ym = (m.v[a[i]][1] + m.v[a[i2]][1]) / 2
                im = i + 0.5
                if r < r_w:
                    zona = "shorts_waist"
                elif m_lat - fc <= i < m_lat + fc:
                    zona = "shorts_stripe"
                else:
                    zona = "shorts_base"
                delante = im < m_lat or im > m_in
                uv = []
                for rr_, ii in vs:
                    fila_ids = todas[rr_]
                    if zona == "shorts_waist":
                        j = pelvis_pierna[lado][ii]
                        u = 1.0 if (lado < 0 and j == 0) else j / n
                        uv.append(isla("pretina", u, (r_w - rr_) / r_w * 0.8))
                    elif zona == "shorts_stripe":
                        y = m.v[fila_ids[ii]][1]
                        uv.append(isla("franja_izq" if lado > 0 else "franja_der",
                                       (y_w - y) / (y_w - y_h), (ii - (m_lat - fc)) / (2 * fc)))
                    else:
                        uv.append(uv_base(lado, fila_ids, ii, delante, *info[rr_]))
                m.cara([todas[rr_][ii] for rr_, ii in vs], uv, zona)
        for r, fila in enumerate(todas):
            for i in fila:
                if r <= 2:
                    m.peso("fijar", i, 1.0)
                elif r <= 5:
                    m.peso("fijar", i, (5 - r) / 3)
                if r <= 1 or r >= len(todas) - 2:
                    m.peso("doble", i, 1.0)

    datos = {"cintura": [ids[(0, j)] for j in range(n)], "columnas": n, "filas_pelvis": r_c,
             "costura": costura, "bastas": {lado: piernas[lado][1][-1] for lado in (1, -1)}}
    return m, datos


def _anillo_pierna(c, lado, y, y_cr, q_m, F, corte, t_basta):
    puntos = []
    for yy in np.arange(y, y_cr - 0.02, 0.01):
        p = c.pierna_corte(yy, lado)
        if len(p) >= 3:
            puntos.append(hull2d(p))
    p = c.pierna_corte(y, lado)
    actual = hull2d(p) if len(p) >= 3 else puntos[0]
    union = hull2d(np.concatenate(puntos)) if puntos else actual
    centro = np.array(((actual[:, 0].max() + actual[:, 0].min()) / 2, (actual[:, 1].max() + actual[:, 1].min()) / 2))
    t0 = math.atan2(F[0] - centro[0], F[2] - centro[1])
    paso = 1 if lado > 0 else -1
    angs = t0 + paso * np.radians(np.arange(0.0, 360.25, 0.5))
    ra = radio_polar(actual, centro, angs)
    ru = radio_polar(union, centro, angs)
    hol = corte.holgura_pierna + (corte.holgura_basta - corte.holgura_pierna) * t_basta
    rad = ra + corte.caida * np.maximum(0.0, ru - ra) + hol
    pts = np.stack((centro[0] + rad * np.sin(angs), centro[1] + rad * np.cos(angs)), axis=1)
    largo = np.concatenate(([0.0], np.cumsum(np.linalg.norm(np.diff(pts, axis=0), axis=1))))
    out = []
    for q in q_m:
        a = q * largo[-1]
        x = float(np.interp(a, largo, pts[:, 0]))
        z = float(np.interp(a, largo, pts[:, 1]))
        out.append(np.array((x, y, z)))
    return out
