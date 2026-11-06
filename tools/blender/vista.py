"""Renders rápidos (Workbench) para revisar la tela mientras se ajusta la simulación."""
import math

import bpy
from mathutils import Vector


def camara(nombre, objetivo, distancia, azimut, elevacion=5.0, lente=50):
    """Cámara mirando a ``objetivo`` (Blender) desde el azimut dado (0 = frente de la persona, que mira a -Y)."""
    cam = bpy.data.objects.get(nombre)
    if cam is None:
        cam = bpy.data.objects.new(nombre, bpy.data.cameras.new(nombre))
        bpy.context.scene.collection.objects.link(cam)
    cam.data.lens = lente
    a, e = math.radians(azimut), math.radians(elevacion)
    pos = Vector(objetivo) + Vector((math.sin(a) * math.cos(e), -math.cos(a) * math.cos(e), math.sin(e))) * distancia
    cam.location = pos
    cam.rotation_euler = (Vector(objetivo) - pos).to_track_quat('-Z', 'Y').to_euler()
    return cam


def render_workbench(ruta, cam, ancho=900, alto=1200):
    sc = bpy.context.scene
    sc.render.engine = 'BLENDER_WORKBENCH'
    sc.display.shading.light = 'STUDIO'
    sc.display.shading.color_type = 'MATERIAL'
    sc.display.shading.show_cavity = False
    sc.render.resolution_x, sc.render.resolution_y = ancho, alto
    sc.render.film_transparent = False
    sc.camera = cam
    sc.render.filepath = str(ruta)
    bpy.ops.render.render(write_still=True)
