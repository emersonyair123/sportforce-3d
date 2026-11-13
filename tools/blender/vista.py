import math

import bpy
from mathutils import Vector


def pgeyCamara(pgeyNombre, pgeyObjetivo, pgeyDistancia, pgeyAzimut, pgeyElevacion=5.0, pgeyLente=50):
    pgeyCam = bpy.data.objects.get(pgeyNombre)
    if pgeyCam is None:
        pgeyCam = bpy.data.objects.new(pgeyNombre, bpy.data.cameras.new(pgeyNombre))
        bpy.context.scene.collection.objects.link(pgeyCam)
    pgeyCam.data.lens = pgeyLente
    pgeyA, pgeyE = math.radians(pgeyAzimut), math.radians(pgeyElevacion)
    pgeyPos = Vector(pgeyObjetivo) + Vector((math.sin(pgeyA) * math.cos(pgeyE), -math.cos(pgeyA) * math.cos(pgeyE), math.sin(pgeyE))) * pgeyDistancia
    pgeyCam.location = pgeyPos
    pgeyCam.rotation_euler = (Vector(pgeyObjetivo) - pgeyPos).to_track_quat('-Z', 'Y').to_euler()
    return pgeyCam


def pgeyRenderWorkbench(pgeyRuta, pgeyCam, pgeyAncho=900, pgeyAlto=1200):
    pgeySc = bpy.context.scene
    pgeySc.render.engine = 'BLENDER_WORKBENCH'
    pgeySc.display.shading.light = 'STUDIO'
    pgeySc.display.shading.color_type = 'MATERIAL'
    pgeySc.display.shading.show_cavity = False
    pgeySc.render.resolution_x, pgeySc.render.resolution_y = pgeyAncho, pgeyAlto
    pgeySc.render.film_transparent = False
    pgeySc.camera = pgeyCam
    pgeySc.render.filepath = str(pgeyRuta)
    bpy.ops.render.render(write_still=True)
