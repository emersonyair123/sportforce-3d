import bmesh
import bpy
from mathutils.bvhtree import BVHTree

from malla import pgeyIsla


class PgeyAjustes:
    pgeyCuadros = 45
    pgeyCalidad = 10
    pgeyMasa = 0.08
    pgeyTension = 18.0
    pgeyCompresion = 18.0
    pgeyCorte = 8.0
    pgeyFlexion = 0.5
    pgeyAmortiguacion = 4.0
    pgeyDistanciaColision = 0.0035
    pgeyAutoColision = True
    pgeyDistanciaAuto = 0.002
    pgeyFriccion = 8.0


def pgeyPrepararColision(pgeyObj, pgeyGrosor=0.002):
    if not any(pgeyM.type == 'COLLISION' for pgeyM in pgeyObj.modifiers):
        pgeyObj.modifiers.new("Colision", 'COLLISION')
    pgeyObj.collision.thickness_outer = pgeyGrosor
    pgeyObj.collision.thickness_inner = 0.02
    pgeyObj.collision.cloth_friction = PgeyAjustes.pgeyFriccion
    pgeyObj.collision.damping = 0.6


def pgeySimular(pgeyObj, pgeyAjustes=PgeyAjustes, pgeyFijar="fijar"):
    pgeySc = bpy.context.scene
    pgeyMod = pgeyObj.modifiers.new("Tela", 'CLOTH')
    pgeySt = pgeyMod.settings
    pgeySt.quality = pgeyAjustes.pgeyCalidad
    pgeySt.mass = pgeyAjustes.pgeyMasa
    pgeySt.air_damping = 1.0
    pgeySt.tension_stiffness = pgeyAjustes.pgeyTension
    pgeySt.compression_stiffness = pgeyAjustes.pgeyCompresion
    pgeySt.shear_stiffness = pgeyAjustes.pgeyCorte
    pgeySt.bending_stiffness = pgeyAjustes.pgeyFlexion
    pgeySt.tension_damping = pgeyAjustes.pgeyAmortiguacion
    pgeySt.compression_damping = pgeyAjustes.pgeyAmortiguacion
    pgeySt.shear_damping = pgeyAjustes.pgeyAmortiguacion
    pgeySt.bending_damping = 0.5
    pgeySt.vertex_group_mass = pgeyFijar
    pgeySt.pin_stiffness = 1.0
    pgeyCs = pgeyMod.collision_settings
    pgeyCs.collision_quality = 4
    pgeyCs.distance_min = pgeyAjustes.pgeyDistanciaColision
    pgeyCs.use_self_collision = pgeyAjustes.pgeyAutoColision
    pgeyCs.self_distance_min = pgeyAjustes.pgeyDistanciaAuto
    pgeyCs.self_friction = 5.0
    pgeyMod.point_cache.frame_start = 1
    pgeyMod.point_cache.frame_end = pgeyAjustes.pgeyCuadros
    pgeySc.frame_start, pgeySc.frame_end = 1, pgeyAjustes.pgeyCuadros
    for pgeyF in range(1, pgeyAjustes.pgeyCuadros + 1):
        pgeySc.frame_set(pgeyF)
    bpy.context.view_layer.objects.active = pgeyObj
    for pgeyO in bpy.context.selected_objects:
        pgeyO.select_set(False)
    pgeyObj.select_set(True)
    bpy.ops.object.modifier_apply(modifier="Tela")
    pgeySc.frame_set(1)


def pgeyAlisar(pgeyObj, pgeyCentros, pgeyRadio, pgeyPiel, pgeyPasadas=12, pgeyHolgura=0.004):
    pgeyBm = bmesh.new()
    pgeyBm.from_mesh(pgeyObj.data)
    pgeyBm.verts.ensure_lookup_table()
    pgeyOrigen = [pgeyBm.verts[pgeyI].co.copy() for pgeyI in pgeyCentros]
    pgeyZona = [pgeyV for pgeyV in pgeyBm.verts if min((pgeyV.co - pgeyC).length for pgeyC in pgeyOrigen) < pgeyRadio]
    for _ in range(pgeyPasadas):
        pgeyNuevo = {}
        for pgeyV in pgeyZona:
            pgeyVec = [pgeyE.other_vert(pgeyV).co for pgeyE in pgeyV.link_edges if not pgeyV.is_boundary or pgeyE.is_boundary]
            pgeyMedia = sum(pgeyVec, pgeyV.co * 0) / len(pgeyVec)
            pgeyPeso = 1 - min((pgeyV.co - pgeyC).length for pgeyC in pgeyOrigen) / pgeyRadio
            pgeyNuevo[pgeyV] = pgeyV.co + (pgeyMedia - pgeyV.co) * 0.5 * pgeyPeso
        for pgeyV, pgeyQ in pgeyNuevo.items():
            pgeyV.co = pgeyQ
    pgeyBvh = BVHTree.FromObject(pgeyPiel, bpy.context.evaluated_depsgraph_get())
    for pgeyV in pgeyZona:
        pgeyLoc, pgeyNrm, _, _ = pgeyBvh.find_nearest(pgeyV.co)
        if pgeyLoc is not None and (pgeyV.co - pgeyLoc).dot(pgeyNrm) < pgeyHolgura:
            pgeyV.co = pgeyLoc + pgeyNrm * pgeyHolgura
    pgeyBm.to_mesh(pgeyObj.data)
    pgeyBm.free()
    pgeyObj.data.update()
    return len(pgeyZona)


def pgeyPenetraciones(pgeyObj, pgeyPiel, pgeyMargen=0.0):
    pgeyMe = pgeyObj.data
    pgeyBvh = BVHTree.FromObject(pgeyPiel, bpy.context.evaluated_depsgraph_get())
    pgeyDentro = 0
    for pgeyV in pgeyMe.vertices:
        pgeyLoc, pgeyNrm, _, _ = pgeyBvh.find_nearest(pgeyV.co)
        if pgeyLoc is not None and (pgeyV.co - pgeyLoc).dot(pgeyNrm) < pgeyMargen:
            pgeyDentro += 1
    return pgeyDentro


def pgeyRibeteCuello(pgeyObj, pgeyBucle, pgeyAncho=0.008, pgeyZona="jersey_collar"):

    pgeyMe = pgeyObj.data
    pgeyBm = bmesh.new()
    pgeyBm.from_mesh(pgeyMe)
    pgeyBm.verts.ensure_lookup_table()
    pgeyBm.normal_update()
    pgeyBvh = BVHTree.FromBMesh(pgeyBm)
    pgeyPts = [pgeyBm.verts[pgeyI].co.copy() for pgeyI in pgeyBucle]
    pgeyNrm = [pgeyBm.verts[pgeyI].normal.copy() for pgeyI in pgeyBucle]
    pgeyN = len(pgeyPts)
    pgeyFrente = [pgeyI for pgeyI in range(pgeyN) if pgeyPts[pgeyI].y < 0]
    pgeyK0 = min(pgeyFrente, key=lambda pgeyI: pgeyPts[pgeyI].z)
    pgeyOrden = list(range(pgeyK0, pgeyN)) + list(range(0, pgeyK0))
    if pgeyPts[pgeyOrden[1]].x < pgeyPts[pgeyOrden[-1]].x:
        pgeyOrden = [pgeyOrden[0]] + pgeyOrden[1:][::-1]
    pgeyPts = [pgeyPts[pgeyI] for pgeyI in pgeyOrden]
    pgeyNrm = [pgeyNrm[pgeyI] for pgeyI in pgeyOrden]
    pgeyFilas = []
    pgeyPerfil = [(pgeyAncho, 0.0010), (pgeyAncho * 0.5, 0.0013), (0.0, 0.0014), (0.0, -0.0003), (pgeyAncho * 0.45, -0.0025)]
    pgeyDirs = [pgeyNrm[pgeyI].normalized().cross((pgeyPts[(pgeyI + 1) % pgeyN] - pgeyPts[pgeyI - 1]).normalized()).normalized() for pgeyI in range(pgeyN)]
    pgeyVoto = 0
    for pgeyI in range(pgeyN):
        pgeyCerca = [pgeyBvh.find_nearest(pgeyPts[pgeyI] + pgeyDirs[pgeyI] * pgeyS * 0.012)[3] for pgeyS in (1, -1)]
        pgeyVoto += 1 if pgeyCerca[0] < pgeyCerca[1] else -1
    if pgeyVoto < 0:
        pgeyDirs = [-pgeyD for pgeyD in pgeyDirs]
    for _ in range(8):
        pgeyDirs = [pgeyDirs[pgeyI] if pgeyI in (0, 1, pgeyN - 1) else (pgeyDirs[pgeyI - 1] + pgeyDirs[pgeyI] * 2 + pgeyDirs[(pgeyI + 1) % pgeyN]).normalized()
                for pgeyI in range(pgeyN)]
    for pgeyI in range(pgeyN):
        pgeyP, pgeyNn = pgeyPts[pgeyI], pgeyNrm[pgeyI].normalized()
        pgeyB = (pgeyDirs[pgeyI] - pgeyNn * pgeyDirs[pgeyI].dot(pgeyNn)).normalized()
        pgeyFila = []
        for pgeyDesp, pgeySep in pgeyPerfil:
            pgeyFila.append(pgeyP + pgeyB * pgeyDesp + pgeyNn * pgeySep)
        pgeyFilas.append(pgeyFila)
    pgeyLargo = [0.0]
    for pgeyI in range(1, pgeyN + 1):
        pgeyLargo.append(pgeyLargo[-1] + (pgeyPts[pgeyI % pgeyN] - pgeyPts[pgeyI - 1]).length)
    pgeyCapa = pgeyBm.loops.layers.uv.active
    pgeyIdxMat = list(pgeyMe.materials.keys()).index(pgeyZona)
    pgeyNuevos = [[pgeyBm.verts.new(pgeyQ) for pgeyQ in pgeyFila] for pgeyFila in pgeyFilas]
    pgeyNuevas, pgeyExteriores = [], []
    for pgeyI in range(pgeyN):
        pgeyI2 = (pgeyI + 1) % pgeyN
        for pgeyR in range(len(pgeyPerfil) - 1):
            pgeyVs = [pgeyNuevos[pgeyI][pgeyR], pgeyNuevos[pgeyI2][pgeyR], pgeyNuevos[pgeyI2][pgeyR + 1], pgeyNuevos[pgeyI][pgeyR + 1]]
            pgeyF = pgeyBm.faces.new(pgeyVs)
            pgeyF.material_index = pgeyIdxMat
            pgeyF.smooth = True
            pgeyNuevas.append(pgeyF)
            if pgeyR == 0:
                pgeyExteriores.append(pgeyF)
            pgeyUs = [pgeyLargo[pgeyI] / pgeyLargo[-1], pgeyLargo[pgeyI + 1] / pgeyLargo[-1]]
            for pgeyLoop, (pgeyU, pgeyV) in zip(pgeyF.loops, ((pgeyUs[0], pgeyR), (pgeyUs[1], pgeyR), (pgeyUs[1], pgeyR + 1), (pgeyUs[0], pgeyR + 1))):
                pgeyUu, pgeyVv = pgeyIsla("cuello", pgeyU, pgeyV / (len(pgeyPerfil) - 1))
                pgeyLoop[pgeyCapa].uv = (pgeyUu, 1.0 - pgeyVv)
    pgeyBm.normal_update()
    pgeyIgual = 0
    for pgeyF in pgeyExteriores:
        pgeyLoc, pgeyNq, _, _ = pgeyBvh.find_nearest(pgeyF.calc_center_median())
        pgeyIgual += 1 if pgeyNq is not None and pgeyF.normal.dot(pgeyNq) > 0 else 0
    if pgeyIgual < len(pgeyExteriores) / 2:
        bmesh.ops.reverse_faces(pgeyBm, faces=pgeyNuevas)
    pgeyBm.to_mesh(pgeyMe)
    pgeyBm.free()


def pgeySubdividir(pgeyObj, pgeyNiveles):
    pgeyMod = pgeyObj.modifiers.new("Subdivision", 'SUBSURF')
    pgeyMod.levels = pgeyNiveles
    pgeyMod.render_levels = pgeyNiveles
    pgeyMod.uv_smooth = 'PRESERVE_BOUNDARIES'
    pgeyMod.quality = 3
    pgeyAplicar(pgeyObj, pgeyMod.name)


def pgeyGrosor(pgeyObj, pgeyEspesor=0.0016, pgeyGrupo="doble"):
    pgeyMod = pgeyObj.modifiers.new("Grosor", 'SOLIDIFY')
    pgeyMod.thickness = pgeyEspesor * 2
    pgeyMod.offset = -1.0
    pgeyMod.use_rim = True
    pgeyMod.use_even_offset = False
    pgeyMod.thickness_clamp = 0.5
    pgeyMod.use_quality_normals = True
    if pgeyGrupo in pgeyObj.vertex_groups:
        pgeyMod.vertex_group = pgeyGrupo
        pgeyMod.thickness_vertex_group = 0.5
    pgeyAplicar(pgeyObj, pgeyMod.name)


def pgeyAplicar(pgeyObj, pgeyNombreMod):
    bpy.context.view_layer.objects.active = pgeyObj
    for pgeyO in bpy.context.selected_objects:
        pgeyO.select_set(False)
    pgeyObj.select_set(True)
    bpy.ops.object.modifier_apply(modifier=pgeyNombreMod)


def pgeySepararZonas(pgeyObj):
    pgeyMe = pgeyObj.data
    pgeyMe.normals_split_custom_set_from_vertices([pgeyV.normal for pgeyV in pgeyMe.vertices])
    bpy.context.view_layer.objects.active = pgeyObj
    for pgeyO in bpy.context.selected_objects:
        pgeyO.select_set(False)
    pgeyObj.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.mesh.separate(type='MATERIAL')
    bpy.ops.object.mode_set(mode='OBJECT')
    pgeyPartes = []
    for pgeyO in bpy.context.selected_objects:
        pgeyUsados = {pgeyP.material_index for pgeyP in pgeyO.data.polygons}
        if not pgeyUsados:
            continue
        pgeyNombre = pgeyO.data.materials[pgeyUsados.pop()].name
        pgeyO.name = pgeyNombre
        pgeyO.data.name = pgeyNombre
        pgeyPartes.append(pgeyO)
    return pgeyPartes
