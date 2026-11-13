import math

import numpy as np

from cuerpo import TablaPolar, hull2d, radio_polar
from malla import Malla, isla, smoothstep

ZONAS = ["jersey_body", "jersey_sleeves", "jersey_collar", "jersey_cuffs", "jersey_sides"]
AREA_W, AREA_H = 0.47, 0.58
MANGA_H = 0.17
ANTEBRAZO_H = 0.36


class Corte:
    arista = 0.012
    ruedo_frente = 0.034
    ruedo_espalda = 0.027
    caida = 0.80
    holgura_pecho = 0.010
    holgura_ruedo = 0.022
    holgura_hombro = 0.007
    holgura_escote = 0.006
    holgura_manga = 0.011
    escote = 0.010
    clavicula = 0.076
    v_profundidad = 0.06
    v_max_escote = 0.05
    v_angulo = 26.0
    largo_manga = 0.16
    puno_filas = 2
    anclaje_manga = (0.7, 0.3)
    lado_columnas = 3
    lado_angulo = 82.0
    suavizado = 10
    relajado_canesu = 60
    redondeo_escote = 10
    redondeo_vecinos = 4
    ribete = 0.008
    manga_larga = False
    largo_manga_larga = 0.49
    holgura_puno = 0.006
    anclaje_manga_larga = (0.8, 0.6)
    holgura_hombro_larga = 0.018


def construir(c, corte=Corte):
    k = c.alto / 1.86
    e = corte.arista
    h, s0 = c.alto, c.suelo
    y_a = c.axila - 0.004
    y_s = float(c.hombro[1][1])
    y_ch = y_a - 0.03
    hem_f = c.entrepierna + corte.ruedo_frente * h
    hem_b = c.entrepierna + corte.ruedo_espalda * h
    xclip = float(np.abs(c.torso_corte(y_a)[:, 0]).max()) + 0.004
    medio = TablaPolar(c, y_a - 0.01, y_s + 0.01, recorte=xclip)
    alto = TablaPolar(c, y_s - 0.16, c.base_cuello + 0.05,
                      recorte=lambda y: None if y >= y_s + 0.012 else xclip + max(0.0, y - y_s) * 6)

    cache = {}

    def hull_en(y):
        clave = round(y / 0.004)
        if clave not in cache:
            cache[clave] = hull2d(c.torso_corte(clave * 0.004))
        return cache[clave]

    union = {}

    def radio_caida(y, th):
        clave = round(y / 0.004)
        if clave not in union:
            pts = [hull_en(yy) for yy in np.arange(clave * 0.004, y_ch + 1e-9, 0.008)] or [hull_en(y)]
            union[clave] = hull2d(np.concatenate(pts))
        centro = np.array((0.0, c.torso_z(y)))
        ry = radio_polar(hull_en(y), centro, [th])[0]
        ru = radio_polar(union[clave], centro, [th])[0]
        return ry + corte.caida * max(0.0, ru - ry)

    zc_ch = c.torso_z(y_ch)
    densos = np.radians(np.arange(0.0, 360.25, 0.25))
    rr = radio_polar(hull_en(y_ch), np.array((0.0, zc_ch)), densos)
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
    j270 = n - j90
    jl_izq = th_izq.index(min(th_izq, key=lambda t: abs(t - math.radians(corte.lado_angulo))))
    jl_der = n - jl_izq

    y_m = (y_a + y_s) / 2
    x_pared = max(math.sin(t) * float(medio.radio(y_m, t)) for t in th[: len(th_izq)])
    en_pared = [j for j in range(1, len(th_izq) - 1)
                if math.sin(th[j]) * float(medio.radio(y_m, th[j])) >= x_pared - 0.004]
    j0, j1 = min(en_pared) - 1, max(en_pared) + 1
    sisas = [(j0, j1), (n - j1, n - j0)]

    def en_sisa(j):
        return any(a < j < b for a, b in sisas)

    n1 = max(8, round((y_a - (hem_f + hem_b) / 2) / e))
    n2 = max(3, round((y_s - y_a) / e))
    r_a, r_s = n1, n1 + n2
    crudo = {}
    P = {}

    def punto(t, y, zc, rad):
        return np.array((math.sin(t) * rad, y, zc + math.cos(t) * rad))

    for j in range(n):
        t = th[j]
        hem = hem_b + (hem_f - hem_b) * (1 + math.cos(t)) / 2
        for r in range(n1 + 1):
            y = hem + (y_a - hem) * r / n1
            hol = corte.holgura_pecho + (corte.holgura_ruedo - corte.holgura_pecho) * smoothstep(y_a - 0.12, hem, y)
            P[(r, j)] = punto(t, y, c.torso_z(y), radio_caida(y, t) + hol)
        if en_sisa(j):
            continue
        for r in range(n1 + 1, r_s + 1):
            y = y_a + (y_s - y_a) * (r - n1) / n2
            zc = float(medio.centro_z(y))
            rad = float(medio.radio(y, t))
            crudo[j] = punto(t, y, zc, rad)
            P[(r, j)] = punto(t, y, zc, rad + corte.holgura_pecho)

    y_o = y_s - 0.10
    o2 = np.array((0.0, y_o, float(alto.centro_z(y_o))))
    pc = c.torso_corte(c.base_cuello + 0.008)
    zcn = (pc[:, 1].max() + pc[:, 1].min()) / 2
    rxn = (pc[:, 0].max() - pc[:, 0].min()) / 2 + corte.escote
    rzn = (pc[:, 1].max() - pc[:, 1].min()) / 2 + corte.escote

    def fuera_del_escote(q):
        rx, rz = q[0], q[2] - zcn
        a = math.atan2(rx, rz)
        return math.hypot(rx, rz) - 1.0 / math.sqrt((math.sin(a) / rxn) ** 2 + (math.cos(a) / rzn) ** 2)

    cadenas = {}
    for j in range(n):
        t = th[j]
        dh = np.array((math.sin(t), 0.0, math.cos(t)))

        def plano(q):
            return np.array((float(np.dot(q - o2, dh)), q[1]))

        puntos = []
        e0 = 0.0
        if not en_sisa(j):
            puntos.append(crudo[j])
            e0 = math.atan2(crudo[j][1] - o2[1], plano(crudo[j])[0])
        for el in np.radians(np.arange(math.degrees(e0) + 0.5, 89.0, 0.5)):
            q = alto.salida(o2, dh * math.cos(el) + np.array((0.0, math.sin(el), 0.0)), 0.45)
            if q is None:
                continue
            if not puntos and q[1] < y_s - 0.003:
                continue
            f = fuera_del_escote(q)
            if f <= 0 and puntos:
                fp = fuera_del_escote(puntos[-1])
                tt = fp / (fp - f) if fp > f else 1.0
                puntos.append(puntos[-1] + (q - puntos[-1]) * tt)
                break
            puntos.append(q)
        pila = []
        for q in (plano(p) for p in puntos):
            while len(pila) >= 2:
                a, b = pila[-2], pila[-1]
                if (b[0] - a[0]) * (q[1] - a[1]) - (b[1] - a[1]) * (q[0] - a[0]) < 0:
                    pila.pop()
                else:
                    break
            pila.append(q)
        cadenas[j] = np.array(pila)

    def largo(cad):
        return float(np.linalg.norm(np.diff(cad, axis=0), axis=1).sum())

    n3 = max(4, round(np.percentile([largo(cadenas[j]) for j in range(n)], 70) / e))
    filas = n1 + n2 + n3 + 1
    for j in range(n):
        t = th[j]
        cad = cadenas[j]
        seg = np.concatenate(([0.0], np.cumsum(np.linalg.norm(np.diff(cad, axis=0), axis=1))))
        for i in range(n3 + 1):
            a = seg[-1] * i / n3
            k2 = int(min(np.searchsorted(seg, a, side="right") - 1, len(cad) - 2))
            f = (a - seg[k2]) / max(seg[k2 + 1] - seg[k2], 1e-9)
            q = cad[k2] + (cad[k2 + 1] - cad[k2]) * f
            tan = (cad[k2 + 1] - cad[k2]) / max(np.linalg.norm(cad[k2 + 1] - cad[k2]), 1e-9)
            fr = i / n3
            hol = (corte.holgura_pecho + (corte.holgura_hombro - corte.holgura_pecho) * smoothstep(0.0, 0.4, fr)
                   if fr < 0.9 else corte.holgura_escote)
            q = q + np.array((tan[1], -tan[0])) * hol
            P[(r_s + i, j)] = np.array((math.sin(t) * q[0], q[1], o2[2] + math.cos(t) * q[0]))

    def arco_fila(r, desde, paso, hasta):
        out = {desde: 0.0}
        acc, j = 0.0, desde
        while j != hasta:
            jn = (j + paso) % n
            if (r, jn) not in P or (r, j) not in P:
                break
            acc += float(np.linalg.norm(P[(r, jn)] - P[(r, j)]))
            out[jn] = acc
            j = jn
        return out

    s_fr, s_es = {}, {}
    for r in range(filas):
        s_fr.update({(r, j): a for j, a in arco_fila(r, 0, +1, j90).items()})
        s_fr.update({(r, j): -a for j, a in arco_fila(r, 0, -1, j270).items()})
        s_es.update({(r, j): -a for j, a in arco_fila(r, n // 2, -1, j90).items()})
        s_es.update({(r, j): a for j, a in arco_fila(r, n // 2, +1, j270).items()})
    y_ref = c.base_cuello
    v_raw = {}
    for j in range(n):
        for r in range(r_s + 1):
            if (r, j) in P:
                v_raw[(r, j)] = y_ref - P[(r, j)][1]
        acc = y_ref - P[(r_s, j)][1]
        for r in range(r_s + 1, filas):
            acc -= float(np.linalg.norm(P[(r, j)] - P[(r - 1, j)]))
            v_raw[(r, j)] = acc
    s_max = max(abs(a) for key, a in list(s_fr.items()) + list(s_es.items()) if key[0] <= r_a)
    escala = max(s_max / (0.49 * AREA_W), (max(v_raw.values()) + 0.01) / (0.98 * AREA_H))
    for j in range(n):
        base = v_raw[(r_s, j)]
        bajo = min(v_raw[(r, j)] for r in range(r_s, filas))
        if bajo < 0.004 * AREA_H * escala:
            f = (base - 0.004 * AREA_H * escala) / max(base - bajo, 1e-9)
            for r in range(r_s + 1, filas):
                v_raw[(r, j)] = base - (base - v_raw[(r, j)]) * f
    tope = 0.49 * AREA_W * escala
    for s_dict in (s_fr, s_es):
        for r in range(r_a + 1, filas):
            fila = {key: a for key, a in s_dict.items() if key[0] == r}
            lado_max = {1: max([a for a in fila.values() if a > 0], default=0.0),
                        -1: -min([a for a in fila.values() if a < 0], default=0.0)}
            for key, a in fila.items():
                lim = lado_max[1 if a > 0 else -1]
                if lim > tope:
                    s_dict[key] = a * tope / lim

    y_clav = c.base_cuello - corte.clavicula * k
    y_frente = P[(filas - 1, 0)][1]
    y_punta = min(max(y_clav - corte.v_profundidad * k, y_frente - corte.v_max_escote * k), y_frente - 0.02 * k)
    j_max = int(np.argmin([abs(th[j] - math.radians(corte.v_angulo)) for j in range(n // 2)]))
    j_v = min(range(1, j_max + 1), key=lambda j: (P[(filas - 1, j)][1] - y_punta) / th[j])
    th_v = th[j_v]
    y_esq = P[(filas - 1, j_v)][1]

    def ang(j):
        return th[j] if th[j] <= math.pi else th[j] - 2 * math.pi

    def mezclar(destino, a, b, f):
        P[destino] = orig[a] + (orig[b] - orig[a]) * f
        s_fr[destino] = s_orig[a] + (s_orig[b] - s_orig[a]) * f
        v_raw[destino] = v_orig[a] + (v_orig[b] - v_orig[a]) * f

    orig, s_orig, v_orig = dict(P), dict(s_fr), dict(v_raw)
    quitado, ajustado = set(), set()
    for j in list(range(0, j_v)) + list(range(n - j_v + 1, n)):
        y_l = y_punta + (y_esq - y_punta) * abs(ang(j)) / th_v
        arriba = [r for r in range(r_a, filas) if (r, j) in P and P[(r, j)][1] > y_l + 1e-6]
        if not arriba:
            continue
        quitado.update((r, j) for r in arriba)
        r1 = min(arriba)
        r0 = r1 - 1
        f = (y_l - orig[(r0, j)][1]) / (orig[(r1, j)][1] - orig[(r0, j)][1])
        destino = (r0, j) if f < 0.3 else (r1, j)
        quitado.discard(destino)
        mezclar(destino, (r0, j), (r1, j), f)
        ajustado.add(destino)
    for r in range(r_a, filas):
        for paso in (+1, -1):
            j = 0
            while (r, j) in quitado:
                j = (j + paso) % n
            jin = (j - paso) % n
            if j == 0 or (r, jin) not in quitado or (r, j) in ajustado:
                continue
            a_obj = th_v * (orig[(r, j)][1] - y_punta) / (y_esq - y_punta)
            a_in, a_j = abs(ang(jin)), abs(ang(j))
            if a_in < a_obj < a_j:
                mezclar((r, j), (r, jin), (r, j), (a_obj - a_in) / (a_j - a_in))

    m = Malla(ZONAS)
    idx = {key: m.vert(q) for key, q in P.items() if key not in quitado}
    lc = corte.lado_columnas

    def uv_torso(r, j, zona, lado, delante):
        if zona == "jersey_sides":
            u = (j - (jl_izq - lc)) if lado > 0 else (jl_der + lc - j)
            return isla("lado_izq" if lado > 0 else "lado_der", u / (2 * lc), 1 - r / r_a)
        v = 0.01 + v_raw[(r, j)] / (AREA_H * escala)
        if delante:
            return isla("frente", 0.5 + s_fr[(r, j)] / (AREA_W * escala), v)
        return isla("espalda", 0.5 + s_es[(r, j)] / (AREA_W * escala), v)

    for r in range(filas - 1):
        for j in range(n):
            j2 = (j + 1) % n
            if r_a <= r < r_s and any(a <= j < b for a, b in sisas):
                continue
            esq = [(r, j), (r, j2), (r + 1, j2), (r + 1, j)]
            vivos = [q for q in esq if q in idx]
            if len(vivos) < 3:
                continue
            tm = (th[j] + (th[j2] if j2 else 2 * math.pi)) / 2
            delante = math.cos(tm) > 0
            lado = 1 if tm < math.pi else -1
            jl = jl_izq if lado > 0 else jl_der
            zona = "jersey_sides" if r < r_a and jl - lc <= j < jl + lc else "jersey_body"
            m.cara([idx[q] for q in vivos], [uv_torso(q[0], q[1], zona, lado, delante) for q in vivos], zona)
    for (r, j), i in idx.items():
        if r >= r_s:
            m.peso("fijar", i, float(smoothstep(0.25, 0.9, (r - r_s) / n3)))
        if r <= 1:
            m.peso("doble", i, 1.0)

    mangas = {}
    sisa_bordes = set()
    sisa_tope = []
    for lado, (a, b) in zip((1, -1), sisas):
        borde = [(r_a, j) for j in range(a, b)] + [(r, b) for r in range(r_a, r_s)] + \
                [(r_s, j) for j in range(b, a, -1)] + [(r, a) for r in range(r_s, r_a, -1)]
        sisa_bordes |= {idx[q] for q in borde}
        sisa_tope.append(max((idx[q] for q in borde), key=lambda i: m.v[i][1]))
        mangas[lado] = _manga(c, m, lado, [idx[q] for q in borde], corte, k)

    escote = _bucle_borde(m, idx[(filas - 1, j90)])
    nb = len(escote)
    pos = {v: i for i, v in enumerate(escote)}
    punta = min(range(nb), key=lambda i: m.v[escote[i]][1] if m.v[escote[i]][2] > 0 else 9.0)
    esquinas = [pos[idx[(filas - 1, j_v)]], pos[idx[(filas - 1, n - j_v)]]]
    for e_i in esquinas:
        paso = 1 if (e_i - punta) % nb > nb // 2 else -1
        tramo = [e_i]
        while tramo[-1] != punta:
            tramo.append((tramo[-1] + paso) % nb)
        largos = np.concatenate(([0.0], np.cumsum([np.linalg.norm(m.v[escote[b]] - m.v[escote[a]])
                                                   for a, b in zip(tramo, tramo[1:])])))
        c0, c1 = m.v[escote[tramo[0]]].copy(), m.v[escote[tramo[-1]]].copy()
        for i, f in zip(tramo[1:-1], largos[1:-1] / largos[-1]):
            antes = m.v[escote[i]]
            sa, na, _ = c.cercano(antes)
            dist = max(float(np.dot(antes - sa, na)), corte.holgura_escote)
            q = c0 + (c1 - c0) * f
            sq, nq, _ = c.cercano(q)
            m.v[escote[i]] = sq + nq * dist
    libres = [i for i in range(nb) if min(abs(i - punta), nb - abs(i - punta)) > 2
              and min(min(abs(i - e), nb - abs(i - e)) for e in esquinas) <= corte.redondeo_vecinos]
    for _ in range(corte.redondeo_escote):
        nuevo = {i: (m.v[escote[i - 1]] + m.v[escote[(i + 1) % nb]]) / 2 for i in libres}
        for i, q in nuevo.items():
            m.v[escote[i]] = m.v[escote[i]] * 0.5 + q * 0.5

    tramos = []
    for e_i in esquinas:
        paso = 1 if (e_i - punta) % nb > nb // 2 else -1
        t = [e_i]
        while t[-1] != punta:
            t.append((t[-1] + paso) % nb)
        tramos.append(t)
    resto = [esquinas[1]]
    paso = 1 if (esquinas[0] - esquinas[1]) % nb < (punta - esquinas[1]) % nb else -1
    while resto[-1] != esquinas[0]:
        resto.append((resto[-1] + paso) % nb)
    tramos.append(resto)
    rectas = {}
    for t in tramos:
        pol = [m.v[escote[i]].copy() for i in t]
        for i in t[1:-1]:
            rectas[escote[i]] = (pol, escote[(i - 1) % nb], escote[(i + 1) % nb])
    centro_v = {j for j in range(n) if abs(ang(j)) < 2 * th_v}
    canesu = {i for (r, j), i in idx.items() if r > r_s or (r >= r_a and j in centro_v)} \
        - set(escote) - sisa_bordes
    m.relajar(canesu | set(rectas), corte.relajado_canesu, rectas=rectas)

    ady = m.vecinos()
    anillo, vistos = set(escote), set(escote)
    for peso in (1.0, 1.0, 0.95, 0.85, 0.65, 0.4):
        for v in anillo:
            m.peso("fijar", v, peso)
        anillo = {w for v in anillo for w in ady[v]} - vistos
        vistos |= anillo

    bordes = set(escote) | {idx[(0, j)] for j in range(n)}
    bordes |= {v for fin in mangas.values() for v in fin}
    m.suavizar(corte.suavizado, 0.5, set(range(len(m.v))) - bordes)

    datos = {"filas": filas, "columnas": n, "escala_uv": escala, "sisa_tope": sisa_tope,
             "escote": _bucle_borde(m, idx[(filas - 1, j90)]),
             "esquinas_v": [escote[e] for e in esquinas],
             "ruedo": [idx[(0, j)] for j in range(n)], "mangas": mangas}
    return m, datos


def _manga(c, m, lado, borde, corte, k):
    e = corte.arista
    _, d = c.brazo[lado]
    a0 = c.brazo_punto(lado, 0.0)
    fa = np.array((0.0, 0.0, 1.0)) - d * d[2]
    fa /= np.linalg.norm(fa)
    sa = np.cross(d, fa)
    if sa[0] * lado < 0:
        sa = -sa
    L = [m.v[i] for i in borde]
    s_m = [float(np.dot(q - a0, d)) for q in L]
    centro = np.mean(L, axis=0)
    al = []
    for q in L:
        w = q - centro - d * np.dot(q - centro, d)
        al.append(math.atan2(float(np.dot(w, sa)), float(np.dot(w, fa))) % (2 * math.pi))
    larga = corte.manga_larga
    s_fin = (corte.largo_manga_larga if larga else corte.largo_manga) * k
    anillos = max(4, round((s_fin - float(np.mean(s_m))) / e))
    radios = {}
    eje = c.brazo_eje(lado, s_fin + 0.01) if larga else None

    def marco(s):
        if eje is None:
            return a0 + d * s, fa, sa
        p, t = eje(s)
        f = np.array((0.0, 0.0, 1.0)) - t * t[2]
        f /= np.linalg.norm(f)
        sv = np.cross(t, f)
        if sv[0] * lado < 0:
            sv = -sv
        return p, f, sv

    def tubo(s, a):
        centro_s, f, sv = marco(s)
        direc = f * math.cos(a) + sv * math.sin(a)
        clave = (round(max(s, 0.05), 3), round(a, 3))
        if clave not in radios:
            if eje is None:
                radios[clave] = c.brazo_radio(lado, max(s, 0.05), direc)
            else:
                cs, fs, svs = marco(max(s, 0.05))
                hit = c.rayo(cs, fs * math.cos(a) + svs * math.sin(a), 0.2)
                radios[clave] = hit[2] if hit else 0.05
        hol = corte.holgura_manga
        if larga:
            hol = corte.holgura_hombro_larga + (corte.holgura_manga - corte.holgura_hombro_larga) * float(
                smoothstep(0.08 * k, 0.20 * k, s))
            hol += (corte.holgura_puno - corte.holgura_manga) * float(smoothstep(s_fin - 0.14 * k, s_fin, s))
        return centro_s + direc * (radios[clave] + hol)

    s_min = min(s_m)
    escala = 1.0 if larga else max(1.0, (s_fin - s_min) / (0.97 * MANGA_H))
    t_mezcla = 0.7
    if larga:
        t_mezcla = 0.7 * (corte.largo_manga * k - float(np.mean(s_m))) / (s_fin - float(np.mean(s_m)))
    anclaje = corte.anclaje_manga_larga if larga else corte.anclaje_manga
    ids = [list(borde)]
    for i in range(1, anillos + 1):
        t = i / anillos
        w = float(smoothstep(0.0, t_mezcla, t))
        fila = []
        for q, s, a in zip(L, s_m, al):
            si = s + (s_fin - s) * t
            fin = tubo(s_fin, a)
            fila.append(m.vert((1 - w) * (q + (fin - q) * t) + w * tubo(si, a)))
            m.peso("fijar", fila[-1], anclaje[0] + (anclaje[1] - anclaje[0]) * t)
            if i >= anillos - 1:
                m.peso("doble", fila[-1], 1.0)
        ids.append(fila)
    nb = len(borde)
    isla_m = "manga_izq" if lado > 0 else "manga_der"
    isla_p = "puno_izq" if lado > 0 else "puno_der"
    isla_a = "antebrazo_izq" if lado > 0 else "antebrazo_der"

    def s_anillo(ring, bb):
        return s_m[bb] + (s_fin - s_m[bb]) * ring / anillos

    i_corte = anillos
    if larga:
        i_corte = max(i for i in range(anillos + 1)
                      if max(s_anillo(i, bb) for bb in range(nb)) <= s_min + 0.97 * MANGA_H)
    for i in range(anillos):
        puno = i >= anillos - corte.puno_filas
        for b in range(nb):
            b2 = (b + 1) % nb
            us = {b: al[b] / (2 * math.pi), b2: al[b2] / (2 * math.pi)}
            if abs(us[b] - us[b2]) > 0.5:
                us = {x: (u + 1 if u < 0.5 else u) for x, u in us.items()}
            esquinas = [(i, b), (i, b2), (i + 1, b2), (i + 1, b)]
            uv = []
            for ring, bb in esquinas:
                if puno:
                    v = (ring - (anillos - corte.puno_filas)) / corte.puno_filas * 0.85
                    uv.append(isla(isla_p, us[bb], v))
                elif i >= i_corte:
                    uv.append(isla(isla_a, us[bb], (s_anillo(ring, bb) - s_anillo(i_corte, bb)) / ANTEBRAZO_H))
                else:
                    s_vert = s_m[bb] + (s_fin - s_m[bb]) * ring / anillos
                    uv.append(isla(isla_m, us[bb], 0.01 + (s_vert - s_min) / (MANGA_H * escala)))
            m.cara([ids[ring][bb] for ring, bb in esquinas], uv, "jersey_cuffs" if puno else "jersey_sleeves")
    interior = {v for fila in ids[1:-1] for v in fila}
    m.suavizar(4, 0.5, interior)
    return ids[-1]


def _bucle_borde(m, inicio):
    usos = {}
    for idx, _, _ in m.caras:
        for a in range(len(idx)):
            e = tuple(sorted((idx[a], idx[(a + 1) % len(idx)])))
            usos[e] = usos.get(e, 0) + 1
    ady = {}
    for (a, b), u in usos.items():
        if u == 1:
            ady.setdefault(a, []).append(b)
            ady.setdefault(b, []).append(a)
    bucle, prev, cur = [inicio], None, inicio
    while True:
        sig = [x for x in ady[cur] if x != prev]
        if not sig or sig[0] == inicio:
            break
        prev, cur = cur, sig[0]
        bucle.append(cur)
        if len(bucle) > 10000:
            raise RuntimeError("borde sin cerrar")
    return bucle
