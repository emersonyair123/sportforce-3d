import bpy


def _pgeyImagen(pgeyNombre, pgeyLado):
    pgeyImg = bpy.data.images.get(pgeyNombre)
    if pgeyImg is not None:
        bpy.data.images.remove(pgeyImg)
    pgeyImg = bpy.data.images.new(pgeyNombre, pgeyLado, pgeyLado, alpha=False, float_buffer=False)
    pgeyImg.colorspace_settings.name = 'Non-Color'
    return pgeyImg


def _pgeyDestino(pgeyObj, pgeyImg):
    for pgeyMat in pgeyObj.data.materials:
        pgeyMat.use_nodes = True
        pgeyNodos = pgeyMat.node_tree.nodes
        pgeyNodo = pgeyNodos.get("Horneado") or pgeyNodos.new('ShaderNodeTexImage')
        pgeyNodo.name = "Horneado"
        pgeyNodo.image = pgeyImg
        pgeyNodos.active = pgeyNodo


def _pgeySeleccionar(pgeyActivo, pgeyOtros=()):
    for pgeyO in bpy.context.selected_objects:
        pgeyO.select_set(False)
    for pgeyO in pgeyOtros:
        pgeyO.select_set(True)
    pgeyActivo.select_set(True)
    bpy.context.view_layer.objects.active = pgeyActivo


def pgeyPreparar(pgeyMuestras=96, pgeyDistanciaAo=0.12):
    pgeySc = bpy.context.scene
    pgeySc.render.engine = 'CYCLES'
    pgeySc.cycles.device = 'CPU'
    pgeySc.cycles.samples = pgeyMuestras
    if pgeySc.world is None:
        pgeySc.world = bpy.data.worlds.new("Mundo")
    pgeySc.world.light_settings.distance = pgeyDistanciaAo
    pgeySc.render.bake.margin = 8


def pgeyOclusion(pgeyBaja, pgeyOcultar, pgeyRuta, pgeyLado=1024):
    pgeyImg = _pgeyImagen("ao_" + pgeyBaja.name, pgeyLado)
    _pgeyDestino(pgeyBaja, pgeyImg)
    pgeyPrevios = [(pgeyO, pgeyO.hide_render) for pgeyO in pgeyOcultar]
    for pgeyO in pgeyOcultar:
        pgeyO.hide_render = True
    _pgeySeleccionar(pgeyBaja)
    bpy.ops.object.bake(type='AO', margin=8, use_clear=True)
    for pgeyO, pgeyH in pgeyPrevios:
        pgeyO.hide_render = pgeyH
    pgeyImg.filepath_raw = str(pgeyRuta)
    pgeyImg.file_format = 'PNG'
    pgeyImg.save()
    return pgeyImg


def pgeyNormales(pgeyBaja, pgeyAlta, pgeyRuta, pgeyLado=1024):

    pgeyImg = _pgeyImagen("normal_" + pgeyBaja.name, pgeyLado)
    _pgeyDestino(pgeyBaja, pgeyImg)
    pgeyAlta.hide_render = False
    _pgeySeleccionar(pgeyBaja, [pgeyAlta])
    bpy.ops.object.bake(type='NORMAL', normal_space='TANGENT', normal_r='POS_X', normal_g='NEG_Y',
                        normal_b='POS_Z', use_selected_to_active=True, cage_extrusion=0.004,
                        max_ray_distance=0.012, margin=8, use_clear=True)
    pgeyImg.filepath_raw = str(pgeyRuta)
    pgeyImg.file_format = 'PNG'
    pgeyImg.save()
    return pgeyImg
