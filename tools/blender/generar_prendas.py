import argparse
import json
import sys
import time
from pathlib import Path

PGEY_AQUI = Path(__file__).resolve().parent
sys.path.insert(0, str(PGEY_AQUI))

import bpy
from mathutils.bvhtree import BVHTree

import camiseta
import cuerpo
import hornear
import medias
import short
import tela
import vista

PGEY_RAIZ = PGEY_AQUI.parent.parent
PGEY_CORTES = {"camiseta": "camiseta_futbol_v", "short": "short_futbol", "medias": "medias_futbol"}

PGEY_CORTES_PERSONA = {
    "hombre": {},
    "mujer": {
        "camiseta": {"pgeyCaida": 0.30, "pgeyHolguraPecho": 0.007, "pgeyHolguraRuedo": 0.012,
                     "pgeyRuedoFrente": 0.040, "pgeyRuedoEspalda": 0.034},
        "short": {"pgeyLargoEntrepierna": 0.21, "pgeyHolguraCadera": 0.012, "pgeyHolguraPierna": 0.014,
                  "pgeyHolguraBasta": 0.020},
    },
    "nino": {
        "camiseta": {"pgeyHolguraPecho": 0.009, "pgeyHolguraRuedo": 0.018},
        "short": {"pgeyLargoEntrepierna": 0.20},
    },
    "nina": {
        "camiseta": {"pgeyCaida": 0.55, "pgeyHolguraPecho": 0.008, "pgeyHolguraRuedo": 0.015},
        "short": {"pgeyLargoEntrepierna": 0.22, "pgeyHolguraPierna": 0.016, "pgeyHolguraBasta": 0.024},
    },
}


def pgeyCorte(pgeyModulo, pgeyPersona, pgeyPrenda, **pgeyExtra):
    return type("PgeyCorte", (pgeyModulo.PgeyCorte,), {**PGEY_CORTES_PERSONA[pgeyPersona].get(pgeyPrenda, {}), **pgeyExtra})

PGEY_VARIANTES = {
    "camiseta": {"camiseta_futbol_v_larga": {"pgeyMangaLarga": True}},
    "short": {"short_futbol_corto": ("rodilla", 0.33), "short_futbol_largo": ("rodilla", 0.86)},
    "medias": {"medias_futbol_tobillera": 0.11, "medias_futbol_media": 0.18},
}


def pgeyArgumentos():
    pgeyArgv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    pgeyP = argparse.ArgumentParser(prog="generar_prendas.py")
    pgeyP.add_argument("--persona", default="hombre", choices=["hombre", "mujer", "nino", "nina"])
    pgeyP.add_argument("--salida", default=str(PGEY_RAIZ / "src/main/resources/models/prendas"))
    pgeyP.add_argument("--cuadros", type=int, default=tela.PgeyAjustes.pgeyCuadros)
    pgeyP.add_argument("--vistas", default=None)
    pgeyP.add_argument("--sin-hornear", action="store_true")
    pgeyP.add_argument("--exportar-medio", action="store_true",
                   help="con --variantes, exporta también el short medio (short_futbol) simulado de nuevo")
    pgeyP.add_argument("--variantes", action="store_true",
                   help="solo manga larga, short corto/largo y medias tobillera/media pierna (Etapa 10)")
    return pgeyP.parse_args(pgeyArgv)


class PgeySuperficies:

    def __init__(self, pgeyC):
        self.pgeyC = pgeyC
        self.pgeyOtros = []

    def pgeyAgregar(self, pgeyObj):
        self.pgeyOtros.append(BVHTree.FromObject(pgeyObj, bpy.context.evaluated_depsgraph_get()))

    def pgeyCercano(self, pgeyP):
        pgeyMejor = self.pgeyC.pgeyCercano(pgeyP)
        for pgeyBvh in self.pgeyOtros:
            pgeyLoc, pgeyNrm, _, pgeyD = pgeyBvh.find_nearest(cuerpo.pgeyABlender(pgeyP))
            if pgeyLoc is not None and pgeyD < pgeyMejor[2]:
                pgeyMejor = (cuerpo.pgeyAModelo(pgeyLoc), cuerpo.pgeyAModelo(pgeyNrm), pgeyD)
        return pgeyMejor


def main():
    pgeyArgs = pgeyArgumentos()
    pgeyT0 = time.time()
    bpy.ops.wm.read_factory_settings(use_empty=True)
    pgeyRutaObj = PGEY_RAIZ / "src/main/resources/models/humanos" / pgeyArgs.persona / (pgeyArgs.persona + ".obj")
    if not pgeyRutaObj.exists():
        raise SystemExit("No existe " + str(pgeyRutaObj))
    pgeyPiel, pgeyOtros = cuerpo.pgeyImportar(pgeyRutaObj)
    pgeyC = cuerpo.PgeyCuerpo(pgeyPiel)
    print("MEDIDAS", json.dumps(pgeyC.pgeyResumen()))
    tela.pgeyPrepararColision(pgeyPiel)
    pgeySup = PgeySuperficies(pgeyC)
    tela.PgeyAjustes.pgeyCuadros = pgeyArgs.cuadros
    if pgeyArgs.variantes:
        pgeyVariantes(pgeyArgs, pgeyC, pgeyPiel, pgeyOtros, pgeySup, pgeyT0)
        return

    pgeyMs, pgeyDs = short.pgeyConstruir(pgeyC, pgeyCorte(short, pgeyArgs.persona, "short"))
    pgeyMs.pgeyOrientar(pgeyC)
    pgeyMs.pgeyEmpujarAfuera(pgeyC, 0.006)
    pgeyObjS = pgeyMs.pgeyCrear("short", pgeyC)
    pgeyT = time.time()
    tela.pgeySimular(pgeyObjS)
    print("SIMULADO short en %.1f s; vértices dentro de la piel: %d de %d"
          % (time.time() - pgeyT, tela.pgeyPenetraciones(pgeyObjS, pgeyPiel), len(pgeyObjS.data.vertices)))
    tela.pgeyPrepararColision(pgeyObjS, 0.0015)
    pgeySup.pgeyAgregar(pgeyObjS)

    pgeyCorteC = pgeyCorte(camiseta, pgeyArgs.persona, "camiseta")
    pgeyMc, pgeyDc = camiseta.pgeyConstruir(pgeyC, pgeyCorteC)
    pgeyMc.pgeyOrientar(pgeyC)
    pgeyMc.pgeyEmpujarAfuera(pgeySup, 0.007)
    pgeyObjC = pgeyMc.pgeyCrear("camiseta", pgeyC)
    pgeyT = time.time()
    tela.pgeySimular(pgeyObjC)
    print("SIMULADA camiseta en %.1f s; vértices dentro de la piel: %d de %d; escala UV %.3f"
          % (time.time() - pgeyT, tela.pgeyPenetraciones(pgeyObjC, pgeyPiel), len(pgeyObjC.data.vertices), pgeyDc["escala_uv"]))
    tela.pgeyAlisar(pgeyObjC, pgeyDc["esquinas_v"], 0.035 * pgeyC.pgeyAlto / 1.86, pgeyPiel)
    tela.pgeyRibeteCuello(pgeyObjC, pgeyDc["escote"], pgeyCorteC.pgeyRibete * pgeyC.pgeyAlto / 1.86)

    pgeyMm, _ = medias.pgeyConstruir(pgeyC, pgeyCorte(medias, pgeyArgs.persona, "medias"))
    pgeyMm.pgeyOrientar(pgeyC)
    pgeyMm.pgeyEmpujarAfuera(pgeyC, 0.0025)
    pgeyObjM = pgeyMm.pgeyCrear("medias", pgeyC)
    print("MEDIAS: vértices dentro de la piel: %d de %d" % (tela.pgeyPenetraciones(pgeyObjM, pgeyPiel), len(pgeyObjM.data.vertices)))

    pgeyPrendas = [(pgeyObjC, "camiseta"), (pgeyObjS, "short"), (pgeyObjM, "medias")]
    pgeyAltas = {}
    for pgeyObj, _ in pgeyPrendas:
        pgeyAlta = pgeyObj.copy()
        pgeyAlta.data = pgeyObj.data.copy()
        pgeyAlta.name = pgeyObj.name + "_alta"
        bpy.context.scene.collection.objects.link(pgeyAlta)
        tela.pgeySubdividir(pgeyAlta, 3)
        pgeyAlta.hide_render = True
        pgeyAltas[pgeyObj.name] = pgeyAlta
        tela.pgeySubdividir(pgeyObj, 1)

    if pgeyArgs.vistas:
        _pgeyVistas(Path(pgeyArgs.vistas), pgeyArgs.persona, [pgeyPiel] + pgeyOtros)

    pgeySalida = Path(pgeyArgs.salida) / pgeyArgs.persona
    if not pgeyArgs.sin_hornear:
        hornear.pgeyPreparar()
        for pgeyObj, pgeyClave in pgeyPrendas:
            pgeyCarpeta = pgeySalida / PGEY_CORTES[pgeyClave]
            pgeyCarpeta.mkdir(parents=True, exist_ok=True)
            pgeyT = time.time()
            hornear.pgeyOclusion(pgeyObj, list(pgeyAltas.values()), pgeyCarpeta / "ao.png")
            hornear.pgeyNormales(pgeyObj, pgeyAltas[pgeyObj.name], pgeyCarpeta / "normal.png")
            pgeyAltas[pgeyObj.name].hide_render = True
            print("HORNEADO %s en %.1f s" % (pgeyClave, time.time() - pgeyT))

    for pgeyObj, pgeyClave in pgeyPrendas:
        pgeyCarpeta = pgeySalida / PGEY_CORTES[pgeyClave]
        pgeyCarpeta.mkdir(parents=True, exist_ok=True)
        tela.pgeyGrosor(pgeyObj)
        pgeyPartes = tela.pgeySepararZonas(pgeyObj)
        for pgeyO in bpy.context.selected_objects:
            pgeyO.select_set(False)
        for pgeyO in pgeyPartes:
            pgeyO.select_set(True)
        bpy.ops.wm.obj_export(filepath=str(pgeyCarpeta / "prenda.obj"), export_selected_objects=True,
                              forward_axis='NEGATIVE_Z', up_axis='Y', export_uv=True, export_normals=True,
                              export_materials=False, export_triangulated_mesh=True, apply_modifiers=True,
                              export_object_groups=False, path_mode='STRIP')
        pgeyCaras = sum(len(pgeyO.data.polygons) for pgeyO in pgeyPartes)
        print("EXPORTADO %s: %s (%d caras, %s)" % (pgeyClave, pgeyCarpeta / "prenda.obj", pgeyCaras,
                                                    ", ".join(sorted(pgeyO.name for pgeyO in pgeyPartes))))
    print("LISTO en %.1f s" % (time.time() - pgeyT0))


def pgeyVariantes(pgeyArgs, pgeyC, pgeyPiel, pgeyOtros, pgeySup, pgeyT0):
    pgeyK = pgeyC.pgeyAlto / 1.86
    pgeyRodilla = pgeyC.pgeyRodilla()
    print("RODILLA %.4f (entrepierna %.4f)" % (pgeyRodilla, pgeyC.pgeyEntrepierna))
    pgeyBaseShort = pgeyCorte(short, pgeyArgs.persona, "short")

    def pgeySimularShort(pgeyNombre, pgeyExtra):
        pgeyMs, _ = short.pgeyConstruir(pgeyC, pgeyCorte(short, pgeyArgs.persona, "short", **pgeyExtra))
        pgeyMs.pgeyOrientar(pgeyC)
        pgeyMs.pgeyEmpujarAfuera(pgeyC, 0.006)
        pgeyObj = pgeyMs.pgeyCrear(pgeyNombre, pgeyC)
        pgeyT = time.time()
        tela.pgeySimular(pgeyObj)
        print("SIMULADO %s en %.1f s; vértices dentro de la piel: %d de %d"
              % (pgeyNombre, time.time() - pgeyT, tela.pgeyPenetraciones(pgeyObj, pgeyPiel), len(pgeyObj.data.vertices)))
        return pgeyObj

    pgeyExportar = {}
    for pgeyCarpeta, (pgeyBase, pgeyF) in PGEY_VARIANTES["short"].items():
        pgeyLargo = pgeyF * pgeyBaseShort.pgeyLargoEntrepierna if pgeyBase == "medio" else pgeyF * (pgeyC.pgeyEntrepierna - pgeyRodilla) / pgeyK
        pgeyExportar[pgeyCarpeta] = (pgeySimularShort(pgeyCarpeta, {"pgeyLargoEntrepierna": pgeyLargo,
                                                     "pgeyLargoRef": pgeyBaseShort.pgeyLargoEntrepierna}), "short")
    pgeyObjMedio = pgeySimularShort("short_medio", {})
    tela.pgeyPrepararColision(pgeyObjMedio, 0.0015)
    pgeySup.pgeyAgregar(pgeyObjMedio)

    for pgeyCarpeta, pgeyExtra in PGEY_VARIANTES["camiseta"].items():
        pgeyCorteC = pgeyCorte(camiseta, pgeyArgs.persona, "camiseta", **pgeyExtra)
        pgeyMc, pgeyDc = camiseta.pgeyConstruir(pgeyC, pgeyCorteC)
        pgeyMc.pgeyOrientar(pgeyC)
        pgeyMc.pgeyEmpujarAfuera(pgeySup, 0.007)
        pgeyObjC = pgeyMc.pgeyCrear(pgeyCarpeta, pgeyC)
        pgeyT = time.time()
        tela.pgeySimular(pgeyObjC)
        print("SIMULADA %s en %.1f s; vértices dentro de la piel: %d de %d; escala UV %.3f"
              % (pgeyCarpeta, time.time() - pgeyT, tela.pgeyPenetraciones(pgeyObjC, pgeyPiel), len(pgeyObjC.data.vertices),
                 pgeyDc["escala_uv"]))
        tela.pgeyAlisar(pgeyObjC, pgeyDc["esquinas_v"], 0.035 * pgeyC.pgeyAlto / 1.86, pgeyPiel)
        tela.pgeyAlisar(pgeyObjC, pgeyDc["sisa_tope"], 0.06 * pgeyK, pgeyPiel, pgeyPasadas=20)
        tela.pgeyRibeteCuello(pgeyObjC, pgeyDc["escote"], pgeyCorteC.pgeyRibete * pgeyC.pgeyAlto / 1.86)
        pgeyExportar[pgeyCarpeta] = (pgeyObjC, "camiseta")

    if pgeyArgs.exportar_medio:
        pgeyExportar["short_futbol"] = (pgeyObjMedio, "short_medio")

    for pgeyCarpeta, pgeyAlto in PGEY_VARIANTES["medias"].items():
        pgeyMm, _ = medias.pgeyConstruir(pgeyC, pgeyCorte(medias, pgeyArgs.persona, "medias", pgeyAltoMedia=pgeyAlto,
                                          pgeyAltoRef=medias.PgeyCorte.pgeyAltoMedia))
        pgeyMm.pgeyOrientar(pgeyC)
        pgeyMm.pgeyEmpujarAfuera(pgeyC, 0.0025)
        pgeyObjM = pgeyMm.pgeyCrear(pgeyCarpeta, pgeyC)
        print("MEDIAS %s: vértices dentro de la piel: %d de %d"
              % (pgeyCarpeta, tela.pgeyPenetraciones(pgeyObjM, pgeyPiel), len(pgeyObjM.data.vertices)))
        pgeyExportar[pgeyCarpeta] = (pgeyObjM, "medias")

    pgeyBases = {}
    for pgeyObj in {pgeyO for pgeyO, _ in pgeyExportar.values()} | {pgeyObjMedio}:
        pgeyBases[pgeyObj.name] = pgeyObj.data.copy()
        tela.pgeySubdividir(pgeyObj, 1)

    def pgeyAltaDe(pgeyObj):
        pgeyAlta = bpy.data.objects.new(pgeyObj.name + "_alta", pgeyBases[pgeyObj.name])
        pgeyAlta.matrix_world = pgeyObj.matrix_world.copy()
        bpy.context.scene.collection.objects.link(pgeyAlta)
        tela.pgeySubdividir(pgeyAlta, 3)
        return pgeyAlta

    def pgeySolo(pgeyVisibles):
        for pgeyO in {pgeyO for pgeyO, _ in pgeyExportar.values()} | {pgeyObjMedio}:
            pgeyO.hide_render = pgeyO not in pgeyVisibles

    pgeyCamisa = pgeyExportar["camiseta_futbol_v_larga"][0]
    if pgeyArgs.vistas:
        for pgeyNombre, (pgeyS_, pgeyM_) in {"corto-tobillera": ("short_futbol_corto", "medias_futbol_tobillera"),
                                 "largo-media": ("short_futbol_largo", "medias_futbol_media")}.items():
            pgeySolo({pgeyCamisa, pgeyExportar[pgeyS_][0], pgeyExportar[pgeyM_][0]})
            _pgeyVistas(Path(pgeyArgs.vistas) / pgeyNombre, pgeyArgs.persona, [pgeyPiel] + pgeyOtros)

    pgeySalida = Path(pgeyArgs.salida) / pgeyArgs.persona
    if not pgeyArgs.sin_hornear:
        hornear.pgeyPreparar()
        for pgeyCarpeta, (pgeyObj, pgeyClave) in pgeyExportar.items():
            if pgeyClave == "camiseta":
                pgeySolo({pgeyObj, pgeyObjMedio})
            elif pgeyClave in ("short", "short_medio"):
                pgeySolo({pgeyObj, pgeyCamisa})
            else:
                pgeySolo({pgeyObj, pgeyObjMedio, pgeyCamisa})
            pgeyDestino = pgeySalida / pgeyCarpeta
            pgeyDestino.mkdir(parents=True, exist_ok=True)
            pgeyT = time.time()
            hornear.pgeyOclusion(pgeyObj, [], pgeyDestino / "ao.png")
            pgeyAlta = pgeyAltaDe(pgeyObj)
            hornear.pgeyNormales(pgeyObj, pgeyAlta, pgeyDestino / "normal.png")
            pgeyMallaAlta = pgeyAlta.data
            bpy.data.objects.remove(pgeyAlta)
            bpy.data.meshes.remove(pgeyMallaAlta)
            print("HORNEADO %s en %.1f s" % (pgeyCarpeta, time.time() - pgeyT))

    for pgeyCarpeta, (pgeyObj, _) in pgeyExportar.items():
        pgeyDestino = pgeySalida / pgeyCarpeta
        pgeyDestino.mkdir(parents=True, exist_ok=True)
        tela.pgeyGrosor(pgeyObj)
        pgeyPartes = tela.pgeySepararZonas(pgeyObj)
        for pgeyO in bpy.context.selected_objects:
            pgeyO.select_set(False)
        for pgeyO in pgeyPartes:
            pgeyO.select_set(True)
        bpy.ops.wm.obj_export(filepath=str(pgeyDestino / "prenda.obj"), export_selected_objects=True,
                              forward_axis='NEGATIVE_Z', up_axis='Y', export_uv=True, export_normals=True,
                              export_materials=False, export_triangulated_mesh=True, apply_modifiers=True,
                              export_object_groups=False, path_mode='STRIP')
        pgeyCaras = sum(len(pgeyO.data.polygons) for pgeyO in pgeyPartes)
        print("EXPORTADO %s: %s (%d caras, %s)" % (pgeyCarpeta, pgeyDestino / "prenda.obj", pgeyCaras,
                                                    ", ".join(sorted(pgeyO.name for pgeyO in pgeyPartes))))
    print("LISTO en %.1f s" % (time.time() - pgeyT0))


def _pgeyVistas(pgeyCarpeta, pgeyPersona, pgeyCuerpoObjs):
    pgeyCarpeta.mkdir(parents=True, exist_ok=True)
    pgeyColores = {"jersey_body": (0.80, 0.08, 0.10, 1), "jersey_sleeves": (0.80, 0.08, 0.10, 1),
               "jersey_collar": (0.05, 0.05, 0.05, 1), "jersey_cuffs": (0.05, 0.05, 0.05, 1),
               "jersey_sides": (0.95, 0.95, 0.95, 1), "shorts_base": (0.06, 0.06, 0.08, 1),
               "shorts_stripe": (0.80, 0.08, 0.10, 1), "shorts_waist": (0.06, 0.06, 0.08, 1),
               "socks_leg": (0.80, 0.08, 0.10, 1), "socks_band": (0.95, 0.95, 0.95, 1)}
    for pgeyMat in bpy.data.materials:
        if pgeyMat.name in pgeyColores:
            pgeyMat.diffuse_color = pgeyColores[pgeyMat.name]
    for pgeyO in pgeyCuerpoObjs:
        for pgeyMat in pgeyO.data.materials:
            pgeyMat.diffuse_color = (0.78, 0.62, 0.52, 1)
    for pgeyAz, pgeyNombre in ((0, "frente"), (180, "espalda"), (90, "costado"), (30, "tres-cuartos")):
        pgeyCam = vista.pgeyCamara("RevisionCam", (0, 0, 1.15), 2.7, pgeyAz, 6)
        vista.pgeyRenderWorkbench(pgeyCarpeta / ("blender-%s-%s.png" % (pgeyPersona, pgeyNombre)), pgeyCam, 800, 1000)


if __name__ == "__main__":
    main()
