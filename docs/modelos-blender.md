# Modelos 3D: procedural y Blender

El diseñador 3D usa por defecto un **modelo procedural** (generado por código en `com.sportforce3d.three.procedural`): maniquí sin rostro, camiseta con cuello en V, short, medias y chimpunes con tacos, en tres cuerpos (adulto hombre, adulto mujer, juvenil). Puedes reemplazarlo por un modelo hecho en **Blender** sin tocar el código, siempre que respetes los nombres de las zonas y las islas UV de este documento.

Capturas de referencia del modelo procedural: [`docs/capturas/`](capturas/) (frente, espalda, lateral y detalles). Para regenerarlas ejecuta la configuración **Capturas 3D** de IntelliJ, o:

```bash
mvnw.cmd compile
java -cp "target/classes;<classpath de Maven>" com.sportforce3d.three.CaptureTool docs/capturas ALL
```

## 1. Cómo elige el modelo la aplicación

`ModelProviders.forBody(tipo)` busca un archivo en `src/main/resources/models/`:

| Tipo de cuerpo | Archivo |
| --- | --- |
| Adulto hombre | `models/adulto-hombre.obj` |
| Adulto mujer | `models/adulto-mujer.obj` |
| Juvenil | `models/juvenil.obj` |

Si el archivo existe se usa `ObjModelProvider`; si no, el procedural. No hace falta cambiar nada más.

## 2. Escala, orientación y caras

- **Unidades:** metros. Un adulto mide alrededor de 1.80 m; los pies apoyan en **Y = 0**.
- **Ejes al exportar:** *Up* = **Y** y frente del maniquí hacia **+Z**. Si en el visor el maniquí aparece de espaldas, vuelve a exportar cambiando *Forward* (en Blender, el frente de un personaje suele ser −Y).
- **Centro:** el maniquí centrado en X = 0, Z = 0 (entre los pies).
- **Caras:** en sentido antihorario visto desde afuera (lo normal en Blender). Activa *Recalculate Outside* (Shift + N) antes de exportar.
- **Normales:** exporta normales (*Normals* activado) y usa *Shade Smooth*. Si el OBJ no las trae, el cargador las calcula, pero suavizadas por vértice.

## 3. Nombres de los objetos (obligatorio)

Cada zona personalizable es **un objeto** (o grupo) con este nombre exacto. Se aceptan sufijos de Blender como `.001` o `_L` / `_R`, por ejemplo `shoe_sole_L`.

| Prenda | Objetos |
| --- | --- |
| Camiseta | `jersey_body`, `jersey_sleeves`, `jersey_collar`, `jersey_cuffs`, `jersey_sides` |
| Short | `shorts_base`, `shorts_stripe`, `shorts_waist` |
| Medias | `socks_leg`, `socks_band` |
| Zapatillas | `shoe_upper`, `shoe_toe`, `shoe_heel`, `shoe_laces`, `shoe_sole` |
| Maniquí | cualquier nombre que empiece con `mannequin` o `body` |

Los objetos con otros nombres (luces, cámaras, ayudas) se ignoran y se anota un aviso en el log. El archivo `.mtl` no se usa: el color y la tela los pone el diseñador.

## 4. Desplegar UV (islas)

Cada prenda usa **una textura de 2048 × 2048** compartida por todas sus zonas. En la Etapa 4 el diseñador pintará colores, patrones, nombre, número y logos en estas islas, así que **frente y espalda van en islas separadas** y las islas de una misma prenda **no se superponen**. Coordenadas en 0..1, con V hacia abajo como en una imagen (Blender usa V hacia arriba; el cargador lo invierte solo).

| Zona | Isla(s) u0, v0 → u1, v1 |
| --- | --- |
| `jersey_body` frente | 0.02, 0.02 → 0.48, 0.54 |
| `jersey_body` espalda | 0.52, 0.02 → 0.98, 0.54 |
| `jersey_sleeves` derecha / izquierda | 0.02, 0.58 → 0.30, 0.78 · 0.34, 0.58 → 0.62, 0.78 |
| `jersey_cuffs` derecha / izquierda | 0.66, 0.58 → 0.81, 0.63 · 0.83, 0.58 → 0.98, 0.63 |
| `jersey_sides` derecha / izquierda | 0.66, 0.66 → 0.74, 0.90 · 0.78, 0.66 → 0.86, 0.90 |
| `jersey_collar` | 0.02, 0.93 → 0.98, 0.97 |
| `shorts_base` pierna derecha frente / espalda | 0.02, 0.02 → 0.24, 0.78 · 0.26, 0.02 → 0.48, 0.78 |
| `shorts_base` pierna izquierda frente / espalda | 0.52, 0.02 → 0.74, 0.78 · 0.76, 0.02 → 0.98, 0.78 |
| `shorts_waist` | 0.02, 0.82 → 0.98, 0.88 |
| `shorts_stripe` derecha / izquierda | 0.02, 0.91 → 0.48, 0.97 · 0.52, 0.91 → 0.98, 0.97 |
| `socks_leg` derecha / izquierda | 0.02, 0.02 → 0.48, 0.78 · 0.52, 0.02 → 0.98, 0.78 |
| `socks_band` derecha / izquierda | 0.02, 0.82 → 0.48, 0.96 · 0.52, 0.82 → 0.98, 0.96 |
| `shoe_upper` | 0.02, 0.02 → 0.98, 0.46 |
| `shoe_toe` | 0.02, 0.50 → 0.48, 0.66 |
| `shoe_heel` | 0.52, 0.50 → 0.98, 0.66 |
| `shoe_laces` | 0.02, 0.70 → 0.48, 0.78 |
| `shoe_sole` | 0.02, 0.82 → 0.98, 0.98 |

La fuente de verdad es `three/model/UvLayout.java`.

**Orientación dentro de las islas (para que los textos no salgan al revés):**
- **Frente:** la izquierda de la isla es lo que se ve a la izquierda de la pantalla en la vista frontal.
- **Espalda:** igual, pero vista desde atrás.
- **V:** crece de arriba (cuello o pretina) hacia abajo (ruedo).

"Derecha" e "izquierda" son las del jugador: su izquierda es +X, a la derecha de la pantalla en vista frontal.

Pasos en Blender:
1. Marca costuras (*Mark Seam*) en los costados de la camiseta para separar frente y espalda, y en la cara interior de cada pierna del short.
2. *UV → Unwrap*.
3. Escala y mueve cada isla dentro de su rectángulo de la tabla, dejando un margen de unos 8 px a 2048.

## 5. Exportar

1. Selecciona el maniquí y todas las prendas.
2. *File → Export → Wavefront (.obj)* con: *Selection Only*, *Apply Modifiers*, *UV Coordinates*, *Normals*, *Triangulated Mesh* (opcional; el cargador triangula polígonos), *Object Groups* u *Objects as OBJ Objects*. *Forward* y *Up* según la sección 2. Escala 1.0.
3. Copia el `.obj` a `src/main/resources/models/` con el nombre del tipo de cuerpo.
4. Ejecuta la aplicación y abre **Diseñador 3D**. Si algo no se ve, revisa los avisos del log ("Objeto OBJ ignorado…").

Consejo: mantén las mallas por debajo de ~150 000 triángulos por cuerpo para que el visor se mueva fluido en equipos modestos.

## 6. Cómo está hecho el modelo procedural

Sirve de guía para quien quiera mejorarlo o modelarlo en Blender:

- **Torso:** en cada altura, una superelipse polar con semiejes distintos adelante y atrás. A eso se suman "lóbulos" gaussianos: pectorales o busto, omóplatos, glúteos y el surco de la columna. Los perfiles se interpolan con curvas monótonas (PCHIP), que no forman bultos falsos. Son 170 × 128 segmentos.
- **Extremidades y cuello:** tubos de sección elíptica variable sobre una espina Catmull-Rom (`Tube`): deltoides, bíceps, codo, antebrazo, mano tipo maniquí, cuádriceps, rodilla y pantorrilla desplazada hacia atrás.
- **Prendas:** cada una parte de una superficie base. La camiseta cae recta desde el pecho y el short forma una "D" de pelvis que se transforma en el óvalo del muslo. Esa base se desplaza sobre su **normal real** con holgura y pliegues sutiles. Cuello en V, puños, pretina y banda de media llevan **borde enrollado**, con espesor visible.
- **Zapatillas:** capellada lofteada a lo largo del pie con la punta levantada (*toe spring*), puntera y contrafuerte superpuestos, collarín acolchado, lengüeta, cinco cordones planos cruzados, suela redondeada (superelipse n = 6) y 10 tacos cónicos FG.
- **Normales y caras:** `GridSurface` calcula normales suaves por diferencias centrales y orienta cada triángulo según una pista "hacia afuera". Las pruebas verifican que ninguna cara quede invertida y que las prendas nunca atraviesen el cuerpo.
- **Escena:**
  - Tres luces direccionales que giran con la cámara: clave cálida, relleno frío y contraluz, más luz ambiente.
  - Suelo con sombra de contacto horneada en su textura, calculada desde los vértices que tocan el piso.
  - Mapa de relieve de tejido Dry-Fit generado por código.
