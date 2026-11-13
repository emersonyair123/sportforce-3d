import bmesh
import bpy
from mathutils.bvhtree import BVHTree

from malla import isla


class Ajustes:
    cuadros = 45
    calidad = 10
    masa = 0.08
    tension = 18.0
    compresion = 18.0
    corte = 8.0
    flexion = 0.5
    amortiguacion = 4.0
    distancia_colision = 0.0035
    auto_colision = True
    distancia_auto = 0.002
    friccion = 8.0


def preparar_colision(obj, grosor=0.002):
    if not any(m.type == 'COLLISION' for m in obj.modifiers):
        obj.modifiers.new("Colision", 'COLLISION')
    obj.collision.thickness_outer = grosor
    obj.collision.thickness_inner = 0.02
    obj.collision.cloth_friction = Ajustes.friccion
    obj.collision.damping = 0.6


def simular(obj, ajustes=Ajustes, fijar="fijar"):
    sc = bpy.context.scene
    mod = obj.modifiers.new("Tela", 'CLOTH')
    st = mod.settings
    st.quality = ajustes.calidad
    st.mass = ajustes.masa
    st.air_damping = 1.0
    st.tension_stiffness = ajustes.tension
    st.compression_stiffness = ajustes.compresion
    st.shear_stiffness = ajustes.corte
    st.bending_stiffness = ajustes.flexion
    st.tension_damping = ajustes.amortiguacion
    st.compression_damping = ajustes.amortiguacion
    st.shear_damping = ajustes.amortiguacion
    st.bending_damping = 0.5
    st.vertex_group_mass = fijar
    st.pin_stiffness = 1.0
    cs = mod.collision_settings
    cs.collision_quality = 4
    cs.distance_min = ajustes.distancia_colision
    cs.use_self_collision = ajustes.auto_colision
    cs.self_distance_min = ajustes.distancia_auto
    cs.self_friction = 5.0
    mod.point_cache.frame_start = 1
    mod.point_cache.frame_end = ajustes.cuadros
    sc.frame_start, sc.frame_end = 1, ajustes.cuadros
    for f in range(1, ajustes.cuadros + 1):
        sc.frame_set(f)
    bpy.context.view_layer.objects.active = obj
    for o in bpy.context.selected_objects:
        o.select_set(False)
    obj.select_set(True)
    bpy.ops.object.modifier_apply(modifier="Tela")
    sc.frame_set(1)


def alisar(obj, centros, radio, piel, pasadas=12, holgura=0.004):
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bm.verts.ensure_lookup_table()
    origen = [bm.verts[i].co.copy() for i in centros]
    zona = [v for v in bm.verts if min((v.co - c).length for c in origen) < radio]
    for _ in range(pasadas):
        nuevo = {}
        for v in zona:
            vec = [e.other_vert(v).co for e in v.link_edges if not v.is_boundary or e.is_boundary]
            media = sum(vec, v.co * 0) / len(vec)
            peso = 1 - min((v.co - c).length for c in origen) / radio
            nuevo[v] = v.co + (media - v.co) * 0.5 * peso
        for v, q in nuevo.items():
            v.co = q
    bvh = BVHTree.FromObject(piel, bpy.context.evaluated_depsgraph_get())
    for v in zona:
        loc, nrm, _, _ = bvh.find_nearest(v.co)
        if loc is not None and (v.co - loc).dot(nrm) < holgura:
            v.co = loc + nrm * holgura
    bm.to_mesh(obj.data)
    bm.free()
    obj.data.update()
    return len(zona)


def penetraciones(obj, piel, margen=0.0):
    me = obj.data
    bvh = BVHTree.FromObject(piel, bpy.context.evaluated_depsgraph_get())
    dentro = 0
    for v in me.vertices:
        loc, nrm, _, _ = bvh.find_nearest(v.co)
        if loc is not None and (v.co - loc).dot(nrm) < margen:
            dentro += 1
    return dentro


def ribete_cuello(obj, bucle, ancho=0.008, zona="jersey_collar"):

    me = obj.data
    bm = bmesh.new()
    bm.from_mesh(me)
    bm.verts.ensure_lookup_table()
    bm.normal_update()
    bvh = BVHTree.FromBMesh(bm)
    pts = [bm.verts[i].co.copy() for i in bucle]
    nrm = [bm.verts[i].normal.copy() for i in bucle]
    n = len(pts)
    frente = [i for i in range(n) if pts[i].y < 0]
    k0 = min(frente, key=lambda i: pts[i].z)
    orden = list(range(k0, n)) + list(range(0, k0))
    if pts[orden[1]].x < pts[orden[-1]].x:
        orden = [orden[0]] + orden[1:][::-1]
    pts = [pts[i] for i in orden]
    nrm = [nrm[i] for i in orden]
    filas = []
    perfil = [(ancho, 0.0010), (ancho * 0.5, 0.0013), (0.0, 0.0014), (0.0, -0.0003), (ancho * 0.45, -0.0025)]
    dirs = [nrm[i].normalized().cross((pts[(i + 1) % n] - pts[i - 1]).normalized()).normalized() for i in range(n)]
    voto = 0
    for i in range(n):
        cerca = [bvh.find_nearest(pts[i] + dirs[i] * s * 0.012)[3] for s in (1, -1)]
        voto += 1 if cerca[0] < cerca[1] else -1
    if voto < 0:
        dirs = [-d for d in dirs]
    for _ in range(8):
        dirs = [dirs[i] if i in (0, 1, n - 1) else (dirs[i - 1] + dirs[i] * 2 + dirs[(i + 1) % n]).normalized()
                for i in range(n)]
    for i in range(n):
        p, nn = pts[i], nrm[i].normalized()
        b = (dirs[i] - nn * dirs[i].dot(nn)).normalized()
        fila = []
        for desp, sep in perfil:
            fila.append(p + b * desp + nn * sep)
        filas.append(fila)
    largo = [0.0]
    for i in range(1, n + 1):
        largo.append(largo[-1] + (pts[i % n] - pts[i - 1]).length)
    capa = bm.loops.layers.uv.active
    idx_mat = list(me.materials.keys()).index(zona)
    nuevos = [[bm.verts.new(q) for q in fila] for fila in filas]
    nuevas, exteriores = [], []
    for i in range(n):
        i2 = (i + 1) % n
        for r in range(len(perfil) - 1):
            vs = [nuevos[i][r], nuevos[i2][r], nuevos[i2][r + 1], nuevos[i][r + 1]]
            f = bm.faces.new(vs)
            f.material_index = idx_mat
            f.smooth = True
            nuevas.append(f)
            if r == 0:
                exteriores.append(f)
            us = [largo[i] / largo[-1], largo[i + 1] / largo[-1]]
            for loop, (u, v) in zip(f.loops, ((us[0], r), (us[1], r), (us[1], r + 1), (us[0], r + 1))):
                uu, vv = isla("cuello", u, v / (len(perfil) - 1))
                loop[capa].uv = (uu, 1.0 - vv)
    bm.normal_update()
    igual = 0
    for f in exteriores:
        loc, nq, _, _ = bvh.find_nearest(f.calc_center_median())
        igual += 1 if nq is not None and f.normal.dot(nq) > 0 else 0
    if igual < len(exteriores) / 2:
        bmesh.ops.reverse_faces(bm, faces=nuevas)
    bm.to_mesh(me)
    bm.free()


def subdividir(obj, niveles):
    mod = obj.modifiers.new("Subdivision", 'SUBSURF')
    mod.levels = niveles
    mod.render_levels = niveles
    mod.uv_smooth = 'PRESERVE_BOUNDARIES'
    mod.quality = 3
    aplicar(obj, mod.name)


def grosor(obj, espesor=0.0016, grupo="doble"):
    mod = obj.modifiers.new("Grosor", 'SOLIDIFY')
    mod.thickness = espesor * 2
    mod.offset = -1.0
    mod.use_rim = True
    mod.use_even_offset = False
    mod.thickness_clamp = 0.5
    mod.use_quality_normals = True
    if grupo in obj.vertex_groups:
        mod.vertex_group = grupo
        mod.thickness_vertex_group = 0.5
    aplicar(obj, mod.name)


def aplicar(obj, nombre_mod):
    bpy.context.view_layer.objects.active = obj
    for o in bpy.context.selected_objects:
        o.select_set(False)
    obj.select_set(True)
    bpy.ops.object.modifier_apply(modifier=nombre_mod)


def separar_zonas(obj):
    me = obj.data
    me.normals_split_custom_set_from_vertices([v.normal for v in me.vertices])
    bpy.context.view_layer.objects.active = obj
    for o in bpy.context.selected_objects:
        o.select_set(False)
    obj.select_set(True)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.mesh.separate(type='MATERIAL')
    bpy.ops.object.mode_set(mode='OBJECT')
    partes = []
    for o in bpy.context.selected_objects:
        usados = {p.material_index for p in o.data.polygons}
        if not usados:
            continue
        nombre = o.data.materials[usados.pop()].name
        o.name = nombre
        o.data.name = nombre
        partes.append(o)
    return partes
