import argparse
import json
import sys
import time
from pathlib import Path

AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(AQUI))

import bpy
from mathutils.bvhtree import BVHTree

import camiseta
import cuerpo
import hornear
import medias
import short
import tela
import vista

RAIZ = AQUI.parent.parent
CORTES = {"camiseta": "camiseta_futbol_v", "short": "short_futbol", "medias": "medias_futbol"}

CORTES_PERSONA = {
    "hombre": {},
    "mujer": {
        "camiseta": {"caida": 0.30, "holgura_pecho": 0.007, "holgura_ruedo": 0.012,
                     "ruedo_frente": 0.040, "ruedo_espalda": 0.034},
        "short": {"largo_entrepierna": 0.21, "holgura_cadera": 0.012, "holgura_pierna": 0.014,
                  "holgura_basta": 0.020},
    },
    "nino": {
        "camiseta": {"holgura_pecho": 0.009, "holgura_ruedo": 0.018},
        "short": {"largo_entrepierna": 0.20},
    },
    "nina": {
        "camiseta": {"caida": 0.55, "holgura_pecho": 0.008, "holgura_ruedo": 0.015},
        "short": {"largo_entrepierna": 0.22, "holgura_pierna": 0.016, "holgura_basta": 0.024},
    },
}


def corte(modulo, persona, prenda, **extra):
    return type("Corte", (modulo.Corte,), {**CORTES_PERSONA[persona].get(prenda, {}), **extra})

VARIANTES = {
    "camiseta": {"camiseta_futbol_v_larga": {"manga_larga": True}},
    "short": {"short_futbol_corto": ("rodilla", 0.33), "short_futbol_largo": ("rodilla", 0.86)},
    "medias": {"medias_futbol_tobillera": 0.11, "medias_futbol_media": 0.18},
}


def argumentos():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    p = argparse.ArgumentParser(prog="generar_prendas.py")
    p.add_argument("--persona", default="hombre", choices=["hombre", "mujer", "nino", "nina"])
    p.add_argument("--salida", default=str(RAIZ / "src/main/resources/models/prendas"))
    p.add_argument("--cuadros", type=int, default=tela.Ajustes.cuadros)
    p.add_argument("--vistas", default=None)
    p.add_argument("--sin-hornear", action="store_true")
    p.add_argument("--exportar-medio", action="store_true",
                   help="con --variantes, exporta también el short medio (short_futbol) simulado de nuevo")
    p.add_argument("--variantes", action="store_true",
                   help="solo manga larga, short corto/largo y medias tobillera/media pierna (Etapa 10)")
    return p.parse_args(argv)


class Superficies:

    def __init__(self, c):
        self.c = c
        self.otros = []

    def agregar(self, obj):
        self.otros.append(BVHTree.FromObject(obj, bpy.context.evaluated_depsgraph_get()))

    def cercano(self, p):
        mejor = self.c.cercano(p)
        for bvh in self.otros:
            loc, nrm, _, d = bvh.find_nearest(cuerpo.a_blender(p))
            if loc is not None and d < mejor[2]:
                mejor = (cuerpo.a_modelo(loc), cuerpo.a_modelo(nrm), d)
        return mejor


def main():
    args = argumentos()
    t0 = time.time()
    bpy.ops.wm.read_factory_settings(use_empty=True)
    ruta_obj = RAIZ / "src/main/resources/models/humanos" / args.persona / (args.persona + ".obj")
    if not ruta_obj.exists():
        raise SystemExit("No existe " + str(ruta_obj))
    piel, otros = cuerpo.importar(ruta_obj)
    c = cuerpo.Cuerpo(piel)
    print("MEDIDAS", json.dumps(c.resumen()))
    tela.preparar_colision(piel)
    sup = Superficies(c)
    tela.Ajustes.cuadros = args.cuadros
    if args.variantes:
        variantes(args, c, piel, otros, sup, t0)
        return

    ms, ds = short.construir(c, corte(short, args.persona, "short"))
    ms.orientar(c)
    ms.empujar_afuera(c, 0.006)
    obj_s = ms.crear("short", c)
    t = time.time()
    tela.simular(obj_s)
    print("SIMULADO short en %.1f s; vértices dentro de la piel: %d de %d"
          % (time.time() - t, tela.penetraciones(obj_s, piel), len(obj_s.data.vertices)))
    tela.preparar_colision(obj_s, 0.0015)
    sup.agregar(obj_s)

    corte_c = corte(camiseta, args.persona, "camiseta")
    mc, dc = camiseta.construir(c, corte_c)
    mc.orientar(c)
    mc.empujar_afuera(sup, 0.007)
    obj_c = mc.crear("camiseta", c)
    t = time.time()
    tela.simular(obj_c)
    print("SIMULADA camiseta en %.1f s; vértices dentro de la piel: %d de %d; escala UV %.3f"
          % (time.time() - t, tela.penetraciones(obj_c, piel), len(obj_c.data.vertices), dc["escala_uv"]))
    tela.alisar(obj_c, dc["esquinas_v"], 0.035 * c.alto / 1.86, piel)
    tela.ribete_cuello(obj_c, dc["escote"], corte_c.ribete * c.alto / 1.86)

    mm, _ = medias.construir(c, corte(medias, args.persona, "medias"))
    mm.orientar(c)
    mm.empujar_afuera(c, 0.0025)
    obj_m = mm.crear("medias", c)
    print("MEDIAS: vértices dentro de la piel: %d de %d" % (tela.penetraciones(obj_m, piel), len(obj_m.data.vertices)))

    prendas = [(obj_c, "camiseta"), (obj_s, "short"), (obj_m, "medias")]
    altas = {}
    for obj, _ in prendas:
        alta = obj.copy()
        alta.data = obj.data.copy()
        alta.name = obj.name + "_alta"
        bpy.context.scene.collection.objects.link(alta)
        tela.subdividir(alta, 3)
        alta.hide_render = True
        altas[obj.name] = alta
        tela.subdividir(obj, 1)

    if args.vistas:
        _vistas(Path(args.vistas), args.persona, [piel] + otros)

    salida = Path(args.salida) / args.persona
    if not args.sin_hornear:
        hornear.preparar()
        for obj, clave in prendas:
            carpeta = salida / CORTES[clave]
            carpeta.mkdir(parents=True, exist_ok=True)
            t = time.time()
            hornear.oclusion(obj, list(altas.values()), carpeta / "ao.png")
            hornear.normales(obj, altas[obj.name], carpeta / "normal.png")
            altas[obj.name].hide_render = True
            print("HORNEADO %s en %.1f s" % (clave, time.time() - t))

    for obj, clave in prendas:
        carpeta = salida / CORTES[clave]
        carpeta.mkdir(parents=True, exist_ok=True)
        tela.grosor(obj)
        partes = tela.separar_zonas(obj)
        for o in bpy.context.selected_objects:
            o.select_set(False)
        for o in partes:
            o.select_set(True)
        bpy.ops.wm.obj_export(filepath=str(carpeta / "prenda.obj"), export_selected_objects=True,
                              forward_axis='NEGATIVE_Z', up_axis='Y', export_uv=True, export_normals=True,
                              export_materials=False, export_triangulated_mesh=True, apply_modifiers=True,
                              export_object_groups=False, path_mode='STRIP')
        caras = sum(len(o.data.polygons) for o in partes)
        print("EXPORTADO %s: %s (%d caras, %s)" % (clave, carpeta / "prenda.obj", caras,
                                                    ", ".join(sorted(o.name for o in partes))))
    print("LISTO en %.1f s" % (time.time() - t0))


def variantes(args, c, piel, otros, sup, t0):
    k = c.alto / 1.86
    rodilla = c.rodilla()
    print("RODILLA %.4f (entrepierna %.4f)" % (rodilla, c.entrepierna))
    base_short = corte(short, args.persona, "short")

    def simular_short(nombre, extra):
        ms, _ = short.construir(c, corte(short, args.persona, "short", **extra))
        ms.orientar(c)
        ms.empujar_afuera(c, 0.006)
        obj = ms.crear(nombre, c)
        t = time.time()
        tela.simular(obj)
        print("SIMULADO %s en %.1f s; vértices dentro de la piel: %d de %d"
              % (nombre, time.time() - t, tela.penetraciones(obj, piel), len(obj.data.vertices)))
        return obj

    exportar = {}
    for carpeta, (base, f) in VARIANTES["short"].items():
        largo = f * base_short.largo_entrepierna if base == "medio" else f * (c.entrepierna - rodilla) / k
        exportar[carpeta] = (simular_short(carpeta, {"largo_entrepierna": largo,
                                                     "largo_ref": base_short.largo_entrepierna}), "short")
    obj_medio = simular_short("short_medio", {})
    tela.preparar_colision(obj_medio, 0.0015)
    sup.agregar(obj_medio)

    for carpeta, extra in VARIANTES["camiseta"].items():
        corte_c = corte(camiseta, args.persona, "camiseta", **extra)
        mc, dc = camiseta.construir(c, corte_c)
        mc.orientar(c)
        mc.empujar_afuera(sup, 0.007)
        obj_c = mc.crear(carpeta, c)
        t = time.time()
        tela.simular(obj_c)
        print("SIMULADA %s en %.1f s; vértices dentro de la piel: %d de %d; escala UV %.3f"
              % (carpeta, time.time() - t, tela.penetraciones(obj_c, piel), len(obj_c.data.vertices),
                 dc["escala_uv"]))
        tela.alisar(obj_c, dc["esquinas_v"], 0.035 * c.alto / 1.86, piel)
        tela.alisar(obj_c, dc["sisa_tope"], 0.06 * k, piel, pasadas=20)
        tela.ribete_cuello(obj_c, dc["escote"], corte_c.ribete * c.alto / 1.86)
        exportar[carpeta] = (obj_c, "camiseta")

    if args.exportar_medio:
        exportar["short_futbol"] = (obj_medio, "short_medio")

    for carpeta, alto in VARIANTES["medias"].items():
        mm, _ = medias.construir(c, corte(medias, args.persona, "medias", alto_media=alto,
                                          alto_ref=medias.Corte.alto_media))
        mm.orientar(c)
        mm.empujar_afuera(c, 0.0025)
        obj_m = mm.crear(carpeta, c)
        print("MEDIAS %s: vértices dentro de la piel: %d de %d"
              % (carpeta, tela.penetraciones(obj_m, piel), len(obj_m.data.vertices)))
        exportar[carpeta] = (obj_m, "medias")

    bases = {}
    for obj in {o for o, _ in exportar.values()} | {obj_medio}:
        bases[obj.name] = obj.data.copy()
        tela.subdividir(obj, 1)

    def alta_de(obj):
        alta = bpy.data.objects.new(obj.name + "_alta", bases[obj.name])
        alta.matrix_world = obj.matrix_world.copy()
        bpy.context.scene.collection.objects.link(alta)
        tela.subdividir(alta, 3)
        return alta

    def solo(visibles):
        for o in {o for o, _ in exportar.values()} | {obj_medio}:
            o.hide_render = o not in visibles

    camisa = exportar["camiseta_futbol_v_larga"][0]
    if args.vistas:
        for nombre, (s_, m_) in {"corto-tobillera": ("short_futbol_corto", "medias_futbol_tobillera"),
                                 "largo-media": ("short_futbol_largo", "medias_futbol_media")}.items():
            solo({camisa, exportar[s_][0], exportar[m_][0]})
            _vistas(Path(args.vistas) / nombre, args.persona, [piel] + otros)

    salida = Path(args.salida) / args.persona
    if not args.sin_hornear:
        hornear.preparar()
        for carpeta, (obj, clave) in exportar.items():
            if clave == "camiseta":
                solo({obj, obj_medio})
            elif clave in ("short", "short_medio"):
                solo({obj, camisa})
            else:
                solo({obj, obj_medio, camisa})
            destino = salida / carpeta
            destino.mkdir(parents=True, exist_ok=True)
            t = time.time()
            hornear.oclusion(obj, [], destino / "ao.png")
            alta = alta_de(obj)
            hornear.normales(obj, alta, destino / "normal.png")
            malla_alta = alta.data
            bpy.data.objects.remove(alta)
            bpy.data.meshes.remove(malla_alta)
            print("HORNEADO %s en %.1f s" % (carpeta, time.time() - t))

    for carpeta, (obj, _) in exportar.items():
        destino = salida / carpeta
        destino.mkdir(parents=True, exist_ok=True)
        tela.grosor(obj)
        partes = tela.separar_zonas(obj)
        for o in bpy.context.selected_objects:
            o.select_set(False)
        for o in partes:
            o.select_set(True)
        bpy.ops.wm.obj_export(filepath=str(destino / "prenda.obj"), export_selected_objects=True,
                              forward_axis='NEGATIVE_Z', up_axis='Y', export_uv=True, export_normals=True,
                              export_materials=False, export_triangulated_mesh=True, apply_modifiers=True,
                              export_object_groups=False, path_mode='STRIP')
        caras = sum(len(o.data.polygons) for o in partes)
        print("EXPORTADO %s: %s (%d caras, %s)" % (carpeta, destino / "prenda.obj", caras,
                                                    ", ".join(sorted(o.name for o in partes))))
    print("LISTO en %.1f s" % (time.time() - t0))


def _vistas(carpeta, persona, cuerpo_objs):
    carpeta.mkdir(parents=True, exist_ok=True)
    colores = {"jersey_body": (0.80, 0.08, 0.10, 1), "jersey_sleeves": (0.80, 0.08, 0.10, 1),
               "jersey_collar": (0.05, 0.05, 0.05, 1), "jersey_cuffs": (0.05, 0.05, 0.05, 1),
               "jersey_sides": (0.95, 0.95, 0.95, 1), "shorts_base": (0.06, 0.06, 0.08, 1),
               "shorts_stripe": (0.80, 0.08, 0.10, 1), "shorts_waist": (0.06, 0.06, 0.08, 1),
               "socks_leg": (0.80, 0.08, 0.10, 1), "socks_band": (0.95, 0.95, 0.95, 1)}
    for mat in bpy.data.materials:
        if mat.name in colores:
            mat.diffuse_color = colores[mat.name]
    for o in cuerpo_objs:
        for mat in o.data.materials:
            mat.diffuse_color = (0.78, 0.62, 0.52, 1)
    for az, nombre in ((0, "frente"), (180, "espalda"), (90, "costado"), (30, "tres-cuartos")):
        cam = vista.camara("RevisionCam", (0, 0, 1.15), 2.7, az, 6)
        vista.render_workbench(carpeta / ("blender-%s-%s.png" % (persona, nombre)), cam, 800, 1000)


if __name__ == "__main__":
    main()
