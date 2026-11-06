package com.sportforce3d.three;

import com.sportforce3d.component.Motion;
import com.sportforce3d.model.Garment;
import com.sportforce3d.three.geom.MeshData;
import com.sportforce3d.three.geom.Vec3;
import com.sportforce3d.three.human.HumanModel;
import com.sportforce3d.three.human.OutfitFitter;
import com.sportforce3d.three.model.ModelData;
import com.sportforce3d.three.model.ModelData.Focus;
import com.sportforce3d.three.model.ModelData.Part;
import com.sportforce3d.three.model.Zone;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Visor 3D del diseñador: SubScene con antialiasing, cámara orbital suave, iluminación de estudio de tres
 * luces (clave cálida, relleno frío y contraluz) más ambiente, suelo con sombra de contacto y el modelo.
 * Las luces giran con la cámara para que el maniquí se vea igual de bien desde cualquier lado. Muestra el maniquí
 * procedural con sus prendas ({@link #setModel}) o una persona de MakeHuman ({@link #setPerson}).
 *
 * <p>Controles: arrastrar = girar; rueda o pellizco = zoom; botón derecho/central = subir o bajar el encuadre;
 * doble clic = restablecer; flechas, + y −, 1–4 (vistas) y 0 (restablecer) con el teclado.
 */
public class Viewer3D extends Region {

    /** bg-000 del tema oscuro (design/tokens.json). El escenario 3D no se puede estilizar con CSS. */
    public static final Color STAGE_BACKGROUND = Color.web("#07080a");
    /** Centro del suelo: entre bg-100 y bg-200, para que la figura "pise" un círculo sutil. */
    public static final Color FLOOR_CENTER = Color.web("#14171c");

    private static final double ROTATE_RATE = 11;  // 1/s: ~95 % del camino en ~0.27 s (≈ 320 ms)
    private static final double IDLE_BEFORE_SPIN_MS = 4000;
    private static final double AUTO_SPIN_DEG_PER_S = 9;

    private final Group world = new Group();
    private final Group modelGroup = new Group();
    private final Group floorGroup = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final SubScene subScene;

    private final Translate targetT = new Translate();
    private final Rotate yawR = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitchR = new Rotate(0, Rotate.X_AXIS);
    private final Translate distanceT = new Translate();
    private final Rotate lightYawR = new Rotate(0, Rotate.Y_AXIS);

    private final OrbitState current = new OrbitState(0, 5, 4, new Vec3(0, 0.93, 0));
    private final OrbitState goal = current.copy();
    private OrbitState home = current.copy();

    private final BooleanProperty autoRotate = new SimpleBooleanProperty(this, "autoRotate", true);
    private final Map<Zone, List<MeshView>> zoneViews = new EnumMap<>(Zone.class);
    private final Map<Zone, PhongMaterial> zoneMaterials = new EnumMap<>(Zone.class);
    private final Map<javafx.scene.Node, Zone> viewZones = new java.util.HashMap<>();
    private ModelData model;
    private HumanModel person;
    /** Sombreado y pliegues de la ropa sobre una persona; null con el maniquí. */
    private ClothLook clothLook;
    /** Textura del compositor por prenda (sin agujeros ni sombreado) y zonas con malla de agujeros. */
    private final Map<Garment, Image> rawTextures = new EnumMap<>(Garment.class);
    private final Map<Zone, Boolean> perforatedZones = new EnumMap<>(Zone.class);
    private final Perforation perforation = new Perforation();
    /** Encuadre por prenda del modelo visible (vacío mientras no hay modelo). */
    private Map<Garment, Focus> focusMap = Map.of();
    /** Altura del modelo que encuadró la cámara (para conservar la vista al cambiar de modelo). */
    private double frameHeight;
    private PickDrag pickDrag;
    private boolean draggingElement;

    /**
     * Arrastre de elementos sobre la prenda. {@code press} recibe la zona y la coordenada de textura bajo el
     * cursor ({@code PickResult.getIntersectedTexCoord()}); si devuelve true, el gesto mueve el elemento en lugar
     * de girar la cámara. {@code drag} recibe null en zona/UV cuando el cursor sale de la prenda.
     */
    public interface PickDrag {
        boolean press(Zone zone, double u, double v);

        void drag(Zone zone, double u, double v);

        void release();
    }

    private double lastX;
    private double lastY;
    private double yawVelocity;
    private long lastInteraction = System.nanoTime();
    private long lastFrame;
    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            tick(now);
        }
    };

    public Viewer3D() {
        getStyleClass().add("sf-viewer3d");
        camera.setFieldOfView(30);
        camera.setNearClip(0.05);
        camera.setFarClip(60);
        camera.getTransforms().add(distanceT);

        Group rig = new Group(camera);
        rig.getTransforms().addAll(targetT, yawR, pitchR);

        Group lights = createLights();
        lights.getTransforms().add(lightYawR);

        world.getChildren().addAll(floorGroup, modelGroup, lights, rig);
        subScene = new SubScene(world, 400, 400, true, SceneAntialiasing.BALANCED);
        subScene.setFill(STAGE_BACKGROUND);
        subScene.setCamera(camera);
        subScene.setFocusTraversable(false);
        getChildren().add(subScene);

        setFocusTraversable(true);
        setAccessibleText("Visor 3D del uniforme. Arrastra para girar, rueda para acercar, doble clic para restablecer.");
        installInput();
        apply(current);

        sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) {
                timer.stop();
            } else {
                lastFrame = 0;
                timer.start();
            }
        });
    }

    // ---------------------------------------------------------------- Luces

    private Group createLights() {
        AmbientLight ambient = new AmbientLight(Color.rgb(52, 56, 66));

        // Direcciones en coordenadas de JavaFX (Y abajo, +Z se aleja de la cámara en la vista frontal).
        DirectionalLight key = new DirectionalLight(Color.rgb(255, 242, 226).deriveColor(0, 1, 0.92, 1));
        key.setDirection(new Point3D(0.55, 0.62, 0.56));

        DirectionalLight fill = new DirectionalLight(Color.rgb(150, 176, 226).deriveColor(0, 1, 0.42, 1));
        fill.setDirection(new Point3D(-0.88, 0.12, 0.46));

        DirectionalLight rim = new DirectionalLight(Color.rgb(235, 240, 255).deriveColor(0, 1, 0.62, 1));
        rim.setDirection(new Point3D(-0.30, 0.45, -0.84));

        return new Group(ambient, key, fill, rim);
    }

    // ---------------------------------------------------------------- Modelo

    /** Reemplaza el modelo (hilo de JavaFX). Encuadra el cuerpo completo de frente. */
    public void setModel(ModelData data) {
        clothLook = null;
        show(data, null);
    }

    /**
     * Persona con el uniforme ajustado a su cuerpo ({@code OutfitFitter}): las prendas son zonas como las del
     * maniquí, así que colores, texturas, relieve y arrastre funcionan igual. Orden de dibujo: piel y ojos opacos,
     * prendas y al final lo transparente (pelo), para que una coleta sobre la espalda se mezcle con la camiseta.
     */
    public void setDressedPerson(OutfitFitter.DressedPerson dressed) {
        clothLook = new ClothLook(dressed.shading());
        show(dressed.outfit(), dressed.body());
        // Tela de poliéster con pliegues hasta que llegue el material del diseño (setLook).
        zoneMaterials.forEach((zone, m) -> {
            if (clothLook.covers(zone.garment())) {
                m.setBumpMap(clothLook.bump(zone.garment(), m.getBumpMap()));
                m.setSpecularColor(ClothLook.POLYESTER_SPECULAR);
                m.setSpecularPower(ClothLook.POLYESTER_POWER);
            }
        });
    }

    private void show(ModelData data, HumanModel human) {
        this.model = data;
        this.person = human;
        rawTextures.clear();
        perforatedZones.clear();
        modelGroup.getChildren().clear();
        zoneViews.clear();
        zoneMaterials.clear();
        viewZones.clear();

        List<MeshView> transparent = new ArrayList<>();
        List<MeshData> meshes = new ArrayList<>();
        if (human != null) {
            for (HumanModel.Part part : human.parts()) {
                MeshView view = personView(part);
                if (part.transparent()) {
                    transparent.add(view);
                } else {
                    modelGroup.getChildren().add(view);
                }
                meshes.add(part.mesh());
            }
        }
        PhongMaterial skin = Materials.mannequin();
        for (Part part : data.mannequin()) {
            MeshView view = new MeshView(part.mesh().toTriangleMesh());
            view.setMaterial(skin);
            view.setCullFace(CullFace.BACK);
            view.setId(part.name());
            modelGroup.getChildren().add(view);
        }
        Image fabric = FabricMaps.dryFit();
        data.zones().forEach((zone, parts) -> {
            PhongMaterial material = Materials.forZone(zone, Materials.DEFAULT_COLORS.get(zone), fabric);
            zoneMaterials.put(zone, material);
            List<MeshView> views = new ArrayList<>();
            for (Part part : parts) {
                MeshView view = new MeshView(part.mesh().toTriangleMesh());
                view.setMaterial(material);
                // Las prendas tienen aberturas (cuello, mangas, ruedo): se ven ambas caras.
                view.setCullFace(zone.garment() == Garment.SHOES ? CullFace.BACK : CullFace.NONE);
                view.setId(part.name());
                viewZones.put(view, zone);
                views.add(view);
            }
            zoneViews.put(zone, views);
            modelGroup.getChildren().addAll(views);
        });
        modelGroup.getChildren().addAll(transparent);
        data.zones().values().forEach(parts -> parts.forEach(p -> meshes.add(p.mesh())));
        data.mannequin().forEach(p -> meshes.add(p.mesh()));
        rebuildFloor(meshes, human == null ? 0 : human.minY());
        frame(data.focus(), data.height());
    }

    private static MeshView personView(HumanModel.Part part) {
        PhongMaterial material = new PhongMaterial(part.diffuse());
        material.setDiffuseMap(part.texture());
        material.setSpecularColor(part.specular());
        material.setSpecularPower(part.specularPower());
        MeshView view = new MeshView(part.mesh().toTriangleMesh());
        view.setMaterial(material);
        view.setCullFace(CullFace.BACK);
        view.setId(part.name());
        return view;
    }

    /**
     * Muestra una persona (hilo de JavaFX): un {@code MeshView} por material con su textura. Lo opaco va primero y
     * lo transparente (pelo, ojos) después, porque JavaFX no ordena por profundidad. Solo caras delanteras: con
     * ambas caras el interior del pelo se ve oscuro (JavaFX no invierte la normal de la cara trasera). Sin prendas:
     * {@link #getModel()} queda en null.
     */
    public void setPerson(HumanModel human) {
        this.person = human;
        this.model = null;
        modelGroup.getChildren().clear();
        zoneViews.clear();
        zoneMaterials.clear();
        viewZones.clear();

        List<MeshData> meshes = new ArrayList<>();
        for (HumanModel.Part part : human.parts()) {
            modelGroup.getChildren().add(personView(part));
            meshes.add(part.mesh());
        }
        rebuildFloor(meshes, human.minY());

        double h = human.height();
        Focus full = com.sportforce3d.three.human.OutfitFitter.fullBody(human.minY(), h);
        Map<Garment, Focus> all = new EnumMap<>(Garment.class);
        for (Garment g : Garment.values()) {
            all.put(g, full);
        }
        frame(all, h);
    }

    /**
     * Límites de zoom según la altura y "Reiniciar" = cuerpo completo de frente. La primera vez la cámara va a ese
     * encuadre; si ya había algo a la vista (se vuelve a vestir la persona: manga, largo, otra persona) la cámara
     * conserva su giro, inclinación y encuadre, escalados a la nueva altura: editar nunca mueve la vista.
     */
    private void frame(Map<Garment, Focus> focus, double h) {
        boolean keep = !focusMap.isEmpty() && frameHeight > 0;
        double k = keep ? h / frameHeight : 1;
        focusMap = focus;
        frameHeight = h;
        Focus full = focus.get(Garment.KIT);
        current.setDistanceLimits(0.55 * h / 1.812, 6.5 * h / 1.812);
        goal.setDistanceLimits(0.55 * h / 1.812, 6.5 * h / 1.812);
        home = new OrbitState(0, full.pitch(), full.distance(), full.target());
        home.setDistanceLimits(current.minDistance(), current.maxDistance());
        if (keep) {
            goal.aimAt(goal.yaw(), goal.pitch(), goal.distance() * k, scaled(goal.target(), k));
            current.set(goal);
        } else {
            goal.set(home);
            current.set(home);
        }
        yawVelocity = 0;
        apply(current);
    }

    private static Vec3 scaled(Vec3 v, double k) {
        return new Vec3(v.x() * k, v.y() * k, v.z() * k);
    }

    /** Copia del estado al que va la cámara (pruebas: editar no debe cambiarlo). */
    public OrbitState cameraGoal() {
        return goal.copy();
    }

    /** Modelo con prendas (maniquí o persona vestida), o null si se ve una persona sin uniforme (o nada todavía). */
    public ModelData getModel() {
        return model;
    }

    /** Persona visible o null. */
    public HumanModel getPerson() {
        return person;
    }

    /** true cuando hay algo que mostrar (maniquí o persona). */
    public boolean hasContent() {
        return !focusMap.isEmpty();
    }

    /** Material de una zona (la Etapa 4 cambiará color, mapas y brillo desde aquí). */
    public PhongMaterial material(Zone zone) {
        return zoneMaterials.get(zone);
    }

    /** Textura compuesta de una prenda: se aplica a todas sus zonas (el color sale de la textura). */
    public void setGarmentTexture(Garment garment, Image texture) {
        rawTextures.put(garment, texture);
        showTexture(garment);
    }

    /** Textura visible: la del compositor, con los agujeros de la malla y el sombreado de la persona si corresponde. */
    private void showTexture(Garment garment) {
        Image texture = rawTextures.get(garment);
        if (texture == null) {
            return;
        }
        List<com.sportforce3d.three.geom.UvRect> holes = new ArrayList<>();
        perforatedZones.forEach((zone, on) -> {
            if (on && zone.garment() == garment) {
                holes.addAll(com.sportforce3d.three.model.UvLayout.BY_ZONE.getOrDefault(zone, List.of()));
            }
        });
        if (!holes.isEmpty()) {
            texture = perforation.apply(garment, texture, holes);
        }
        Image shown = clothLook != null ? clothLook.shade(garment, texture) : texture;
        zoneMaterials.forEach((zone, m) -> {
            if (zone.garment() == garment) {
                m.setDiffuseColor(Color.WHITE);
                m.setDiffuseMap(shown);
            }
        });
    }

    /**
     * Relieve, brillo y agujeros (malla) de un material en las zonas indicadas. Cada tela conserva su brillo también
     * sobre la persona: el algodón queda mate y el poliéster liso brilla más que el dry-fit.
     */
    public void setLook(java.util.Collection<Zone> zones, MaterialLook look) {
        java.util.Set<Garment> retexture = java.util.EnumSet.noneOf(Garment.class);
        for (Zone z : zones) {
            if (perforatedZones.getOrDefault(z, false) != look.perforated()) {
                perforatedZones.put(z, look.perforated());
                retexture.add(z.garment());
            }
            PhongMaterial m = zoneMaterials.get(z);
            if (m == null) {
                continue;
            }
            if (clothLook != null && clothLook.covers(z.garment())) {
                // Sobre una persona: pliegues en el relieve; el brillo es el del material, sin brillos de punta.
                m.setBumpMap(clothLook.bump(z.garment(), look.bump()));
                m.setSpecularColor(look.specular());
                m.setSpecularPower(Math.max(6, Math.min(40, look.power())));
            } else {
                m.setBumpMap(look.bump());
                m.setSpecularColor(look.specular());
                m.setSpecularPower(look.power());
            }
        }
        retexture.forEach(this::showTexture);
    }

    public void setPickDrag(PickDrag handler) {
        this.pickDrag = handler;
    }

    /** Captura con la cámara en una vista fija (cuerpo completo) y luego vuelve a donde estaba. */
    public WritableImage captureView(ViewPreset preset, int width) {
        OrbitState savedCurrent = current.copy();
        OrbitState savedGoal = goal.copy();
        jumpTo(preset, Garment.KIT);
        WritableImage img = capture(width);
        current.set(savedCurrent);
        goal.set(savedGoal);
        apply(current);
        return img;
    }

    public List<MeshView> views(Zone zone) {
        return Collections.unmodifiableList(zoneViews.getOrDefault(zone, List.of()));
    }

    /** Suelo con la sombra de contacto de los vértices a menos de 3 cm del piso ({@code floorY}, Y arriba). */
    private void rebuildFloor(List<MeshData> meshes, double floorY) {
        List<float[]> near = new ArrayList<>();
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double sumZ = 0;
        int count = 0;
        for (MeshData m : meshes) {
            float[] pts = m.points();
            for (int i = 0; i < pts.length; i += 3) {
                if (-pts[i + 1] - floorY < 0.03) {
                    near.add(new float[]{pts[i], pts[i + 1], pts[i + 2]});
                    minX = Math.min(minX, pts[i]);
                    maxX = Math.max(maxX, pts[i]);
                    sumZ += pts[i + 2];
                    count++;
                }
            }
        }
        double cx = count == 0 ? 0 : (minX + maxX) / 2;
        double cz = count == 0 ? 0 : sumZ / count;
        floorGroup.getChildren().setAll(StudioFloor.create(near, cx, cz, STAGE_BACKGROUND, FLOOR_CENTER));
    }

    // ---------------------------------------------------------------- Cámara

    /** Gira a una vista (frente, espalda, lados) manteniendo el encuadre actual. */
    public void showView(ViewPreset preset) {
        goal.aimAt(preset.yaw(), goal.pitch(), goal.distance(), goal.target());
        interacted();
        snapIfReduced();
    }

    /** Encuadra una prenda (la cámara se acerca en ~320 ms). */
    public void focus(Garment garment) {
        if (focusMap.isEmpty()) {
            return;
        }
        Focus f = focusMap.getOrDefault(garment, focusMap.get(Garment.KIT));
        goal.aimAt(goal.yaw(), f.pitch(), f.distance(), f.target());
        interacted();
        snapIfReduced();
    }

    /** Vuelve al cuerpo completo de frente. */
    public void resetView() {
        goal.aimAt(home.yaw(), home.pitch(), home.distance(), home.target());
        interacted();
        snapIfReduced();
    }

    public void zoomBy(double factor) {
        goal.zoom(factor);
        interacted();
        snapIfReduced();
    }

    /** Coloca la cámara al instante (capturas y pruebas). */
    public void jumpTo(ViewPreset preset, Garment garment) {
        if (focusMap.isEmpty()) {
            return;
        }
        Focus f = focusMap.getOrDefault(garment, focusMap.get(Garment.KIT));
        goal.aimAt(preset.yaw(), f.pitch(), f.distance(), f.target());
        current.set(goal);
        yawVelocity = 0;
        apply(current);
    }

    /** Encuadra la cabeza de la persona al instante, sin cambiar el giro (capturas). */
    public void jumpToFace() {
        if (person == null) {
            return;
        }
        double h = person.height();
        goal.aimAt(goal.yaw(), 0, current.minDistance() * 1.25, new Vec3(0, person.minY() + 0.93 * h, 0.03 * h));
        current.set(goal);
        yawVelocity = 0;
        apply(current);
    }

    public BooleanProperty autoRotateProperty() {
        return autoRotate;
    }

    private void snapIfReduced() {
        if (Motion.isReduced()) {
            current.set(goal);
            apply(current);
        }
    }

    private void interacted() {
        lastInteraction = System.nanoTime();
    }

    private void tick(long now) {
        double dt = lastFrame == 0 ? 1 / 60.0 : Math.min(0.05, (now - lastFrame) / 1e9);
        lastFrame = now;

        if (Math.abs(yawVelocity) > 0.01) {
            goal.rotate(yawVelocity, 0);
            yawVelocity *= Math.pow(0.0025, dt); // la inercia se apaga en ~1 s
        } else {
            yawVelocity = 0;
        }
        boolean idle = (now - lastInteraction) / 1e6 > IDLE_BEFORE_SPIN_MS;
        if (autoRotate.get() && idle && !Motion.isReduced() && hasContent()) {
            goal.rotate(AUTO_SPIN_DEG_PER_S * dt, 0);
        }
        if (Motion.isReduced()) {
            current.set(goal);
        } else {
            current.approach(goal, OrbitState.alpha(dt, ROTATE_RATE));
        }
        apply(current);
    }

    private void apply(OrbitState s) {
        Vec3 t = s.target();
        targetT.setX(t.x());
        targetT.setY(-t.y());
        targetT.setZ(-t.z());
        yawR.setAngle(s.yaw());
        lightYawR.setAngle(s.yaw());
        pitchR.setAngle(-s.pitch());
        distanceT.setZ(-s.distance());
    }

    // ---------------------------------------------------------------- Entrada

    private void installInput() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            requestFocus();
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            yawVelocity = 0;
            interacted();
            draggingElement = false;
            if (pickDrag != null && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
                double[] hit = pickUv(e);
                Zone zone = pickZone(e);
                draggingElement = hit != null && zone != null && pickDrag.press(zone, hit[0], hit[1]);
            }
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (draggingElement) {
                double[] hit = pickUv(e);
                Zone zone = pickZone(e);
                pickDrag.drag(hit == null ? null : zone, hit == null ? 0 : hit[0], hit == null ? 0 : hit[1]);
                interacted();
                return;
            }
            double dx = e.getSceneX() - lastX;
            double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            if (e.getButton() == MouseButton.PRIMARY && !e.isSynthesized()) {
                goal.rotate(dx * 0.4, dy * 0.25);
                yawVelocity = dx * 0.4 * 0.5;
            } else if (e.getButton() == MouseButton.SECONDARY || e.getButton() == MouseButton.MIDDLE) {
                goal.pan(dy * goal.distance() * 0.0012);
            }
            interacted();
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (draggingElement) {
                draggingElement = false;
                pickDrag.release();
            }
            interacted();
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                yawVelocity = 0;
                resetView();
            }
        });
        setOnScroll(e -> {
            if (e.getTouchCount() >= 2) {
                return; // el pellizco llega como ZoomEvent
            }
            if (e.getTouchCount() == 1) {
                goal.rotate(e.getDeltaX() * 0.3, e.getDeltaY() * 0.2);
            } else if (e.getDeltaY() != 0) {
                goal.zoom(Math.exp(-e.getDeltaY() * 0.0018));
            }
            interacted();
            e.consume();
        });
        setOnZoom(e -> {
            goal.zoom(1 / e.getZoomFactor());
            interacted();
            e.consume();
        });
        setOnRotate(e -> {
            goal.rotate(-e.getAngle(), 0);
            interacted();
            e.consume();
        });
        setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            switch (k) {
                case LEFT -> goal.rotate(15, 0);
                case RIGHT -> goal.rotate(-15, 0);
                case UP -> goal.rotate(0, 6);
                case DOWN -> goal.rotate(0, -6);
                case PLUS, ADD, EQUALS -> goal.zoom(0.85);
                case MINUS, SUBTRACT -> goal.zoom(1 / 0.85);
                case DIGIT1, NUMPAD1 -> showView(ViewPreset.FRONT);
                case DIGIT2, NUMPAD2 -> showView(ViewPreset.BACK);
                case DIGIT3, NUMPAD3 -> showView(ViewPreset.LEFT);
                case DIGIT4, NUMPAD4 -> showView(ViewPreset.RIGHT);
                case DIGIT0, NUMPAD0, R -> resetView();
                default -> {
                    return;
                }
            }
            interacted();
            snapIfReduced();
            e.consume();
        });
    }

    private Zone pickZone(MouseEvent e) {
        javafx.scene.input.PickResult pr = e.getPickResult();
        return pr == null || pr.getIntersectedNode() == null ? null : viewZones.get(pr.getIntersectedNode());
    }

    private static double[] pickUv(MouseEvent e) {
        javafx.scene.input.PickResult pr = e.getPickResult();
        if (pr == null || pr.getIntersectedTexCoord() == null) {
            return null;
        }
        return new double[]{pr.getIntersectedTexCoord().getX(), pr.getIntersectedTexCoord().getY()};
    }

    // ---------------------------------------------------------------- Diseño y capturas

    @Override
    protected void layoutChildren() {
        subScene.setWidth(getWidth());
        subScene.setHeight(getHeight());
    }

    @Override
    protected double computePrefWidth(double height) {
        return 640;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 720;
    }

    /** Captura del visor a {@code width} píxeles de ancho (mantiene la proporción actual). */
    public WritableImage capture(int width) {
        double k = width / Math.max(1, subScene.getWidth());
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(STAGE_BACKGROUND);
        params.setTransform(Transform.scale(k, k));
        return subScene.snapshot(params, null);
    }
}
