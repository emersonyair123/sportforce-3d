import math

import numpy as np

from cuerpo import hull2d, radio_polar
from malla import Malla, isla

ZONAS = ["socks_leg", "socks_band"]


class Corte:
    arista = 0.012
    alto_media = 0.253
    alto_banda = 0.029
    tobillo = 0.075
    fondo = 0.03
    holgura = 0.0030
    holgura_pie = 0.0065
    holgura_banda = 0.0065
    alto_ref = None


def _alturas(c, corte, alto_media):
    e = corte.arista
    h, s0 = c.alto, c.suelo
    y_top = s0 + alto_media * h
    y_banda = y_top - corte.alto_banda * h
    y_tob = s0 + corte.tobillo * h
    y_fin = s0 + corte.fondo * h
    n_leg = max(8, round((y_banda - y_tob) / e))
    n_pie = max(4, round((y_tob - y_fin) / (e / 2)))
    return ([y_top - (y_top - y_banda) * i / 3 for i in range(3)]
            + [y_banda - (y_banda - y_tob) * i / n_leg for i in range(n_leg)]
            + [y_tob - (y_tob - y_fin) * i / n_pie for i in range(n_pie + 1)])


def construir(c, corte=Corte):
    e = corte.arista
    h, s0 = c.alto, c.suelo
    y_top = s0 + corte.alto_media * h
    y_banda = y_top - corte.alto_banda * h
    y_tob = s0 + corte.tobillo * h
    y_fin = s0 + corte.fondo * h
    m = Malla(ZONAS)
    datos = {"bordes": {}}
    for lado in (1, -1):
        secciones = {}

        def corte_en(y):
            clave = round(y / 0.003)
            if clave not in secciones:
                p = c.pierna_corte(clave * 0.003, lado)
                hull = hull2d(p)
                centro = np.array(((hull[:, 0].max() + hull[:, 0].min()) / 2,
                                   (hull[:, 1].max() + hull[:, 1].min()) / 2))
                secciones[clave] = (hull, centro)
            return secciones[clave]

        def seccion(y):
            if y >= y_tob:
                return corte_en(y)
            hull_t, centro_t = corte_en(y_tob)
            p = c.pierna_corte(y, lado)
            if len(p) < 3:
                return hull_t, centro_t
            return hull2d(np.concatenate((hull_t, p))), centro_t

        hull, centro = seccion(y_top)
        circ = float(np.sum(np.linalg.norm(np.diff(np.vstack((hull, hull[:1])), axis=0), axis=1)))
        cols = max(24, 2 * round(circ / e / 2))
        ys = _alturas(c, corte, corte.alto_media)
        if corte.alto_ref is None:
            v_cana = [(fi - 3) / (len(ys) - 4) for fi in range(len(ys))]
        else:
            ref = _alturas(c, corte, corte.alto_ref)[3:]
            v_ref = [i / (len(ref) - 1) for i in range(len(ref))]
            v_cana = [float(np.interp(y, ref[::-1], v_ref[::-1])) for y in ys]
        angs = [2 * math.pi * k / cols for k in range(cols)]
        filas = []
        for y in ys:
            hull, centro = seccion(y)
            rad = radio_polar(hull, centro, [a if lado > 0 else -a for a in angs])
            hol = corte.holgura_banda if y > y_banda + 1e-6 else corte.holgura if y >= y_tob else corte.holgura_pie
            fila = []
            for a, r in zip(angs, rad):
                x = centro[0] + lado * math.sin(a) * (r + hol)
                z = centro[1] + math.cos(a) * (r + hol)
                fila.append(m.vert((x, y, z)))
            filas.append(fila)
        isla_l = "media_izq" if lado > 0 else "media_der"
        isla_b = "banda_izq" if lado > 0 else "banda_der"
        for i in range(len(filas) - 1):
            banda = i < 3
            for k in range(cols):
                k2 = (k + 1) % cols
                us = (k / cols, 1.0 if k2 == 0 else k2 / cols)
                esquinas = [(i, k, us[0]), (i + 1, k, us[0]), (i + 1, k2, us[1]), (i, k2, us[1])]
                if lado < 0:
                    esquinas = esquinas[::-1]
                uv = []
                for fi, _, u in esquinas:
                    if banda:
                        uv.append(isla(isla_b, u, (3 - fi) / 3 * 0.8))
                    else:
                        uv.append(isla(isla_l, u, v_cana[fi]))
                m.cara([filas[fi][kk] for fi, kk, _ in esquinas], uv, "socks_band" if banda else "socks_leg")
        for fi in (0, 1):
            for v in filas[fi]:
                m.peso("doble", v, 1.0)
        datos["bordes"][lado] = filas[0]
    return m, datos
