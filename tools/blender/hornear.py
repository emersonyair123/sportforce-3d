import bpy


def _imagen(nombre, lado):
    img = bpy.data.images.get(nombre)
    if img is not None:
        bpy.data.images.remove(img)
    img = bpy.data.images.new(nombre, lado, lado, alpha=False, float_buffer=False)
    img.colorspace_settings.name = 'Non-Color'
    return img


def _destino(obj, img):
    for mat in obj.data.materials:
        mat.use_nodes = True
        nodos = mat.node_tree.nodes
        nodo = nodos.get("Horneado") or nodos.new('ShaderNodeTexImage')
        nodo.name = "Horneado"
        nodo.image = img
        nodos.active = nodo


def _seleccionar(activo, otros=()):
    for o in bpy.context.selected_objects:
        o.select_set(False)
    for o in otros:
        o.select_set(True)
    activo.select_set(True)
    bpy.context.view_layer.objects.active = activo


def preparar(muestras=96, distancia_ao=0.12):
    sc = bpy.context.scene
    sc.render.engine = 'CYCLES'
    sc.cycles.device = 'CPU'
    sc.cycles.samples = muestras
    if sc.world is None:
        sc.world = bpy.data.worlds.new("Mundo")
    sc.world.light_settings.distance = distancia_ao
    sc.render.bake.margin = 8


def oclusion(baja, ocultar, ruta, lado=1024):
    img = _imagen("ao_" + baja.name, lado)
    _destino(baja, img)
    previos = [(o, o.hide_render) for o in ocultar]
    for o in ocultar:
        o.hide_render = True
    _seleccionar(baja)
    bpy.ops.object.bake(type='AO', margin=8, use_clear=True)
    for o, h in previos:
        o.hide_render = h
    img.filepath_raw = str(ruta)
    img.file_format = 'PNG'
    img.save()
    return img


def normales(baja, alta, ruta, lado=1024):

    img = _imagen("normal_" + baja.name, lado)
    _destino(baja, img)
    alta.hide_render = False
    _seleccionar(baja, [alta])
    bpy.ops.object.bake(type='NORMAL', normal_space='TANGENT', normal_r='POS_X', normal_g='NEG_Y',
                        normal_b='POS_Z', use_selected_to_active=True, cage_extrusion=0.004,
                        max_ray_distance=0.012, margin=8, use_clear=True)
    img.filepath_raw = str(ruta)
    img.file_format = 'PNG'
    img.save()
    return img
