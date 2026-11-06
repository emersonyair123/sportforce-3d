"""Medias de fútbol: un tubo por pierna desde debajo de la rodilla hasta dentro de la zapatilla.

La media es elástica y va pegada a la pierna: no se simula su caída; se arma sobre la envolvente convexa de cada
corte de la pierna con unos milímetros de holgura (sin hundirse entre los músculos) y por debajo del tobillo
envuelve el empeine y el talón (lo que queda dentro de la zapatilla lo recorta la app). La banda superior
(socks_band) dobla hacia adentro.

UV como el modelo procedural: u por ángulo alrededor de la pierna (0 = frente, 0,25 = costado exterior), v desde
el borde inferior de la banda (0) hasta el pie (1); la banda, de su borde inferior (0) hacia arriba.
"""
import math

import numpy as np

from cuerpo import hull2d, radio_polar
from malla import Malla, isla

ZONAS = ["socks_leg", "socks_band"]


class Corte:
    arista = 0.012
    alto_media = 0.253        # borde superior (fracción del alto): debajo de la rodilla
    alto_banda = 0.029        # alto de la banda (fracción del alto)
    tobillo = 0.075           # por debajo de esta altura el tubo baja recto
    fondo = 0.03              # hasta dónde baja (dentro de la zapatilla)
    holgura = 0.0030
    holgura_pie = 0.0065      # bajo el tobillo (dentro de la zapatilla): el empeine se curva entre filas
    holgura_banda = 0.0065    # la banda va doble (Solidify hacia adentro ×2): su cara interior queda a ~3 mm
    # Largos (Etapa 10): las variantes cambian alto_media; con alto_ref (el de la media larga) la v de la caña sigue
    # la escala de la larga (la parte de arriba de la isla queda sin usar): los patrones no se estiran.
    alto_ref = None


def _alturas(c, corte, alto_media):
    """Alturas de las filas: la banda (3 filas) y la caña hasta el fondo; bajo el tobillo, a la mitad de paso (entre
    filas la tela es recta y el empeine es curvo)."""
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
        # Contorno de referencia en el tobillo para la parte que baja recta.
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
            """Envolvente de la pierna a la altura y. Bajo el tobillo, la del tobillo unida a la del pie (empeine y
            talón crecen hacia abajo): la media envuelve el pie en lugar de atravesarlo; el centro queda el del
            tobillo para que los ángulos de las columnas no cambien."""
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
        # Filas: la banda (3 filas) y la caña hasta el fondo, a igual paso.
        # Bajo el tobillo, filas a la mitad de distancia: entre filas la tela es recta y el empeine es curvo.
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
            # Ángulo con el costado exterior en 90° (+x en la pierna izquierda, −x en la derecha).
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
            banda = i < 3  # filas 0..3: de la boca de la media al borde inferior de la banda
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
