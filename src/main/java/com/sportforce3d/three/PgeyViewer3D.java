package com.sportforce3d.three;

import com.sportforce3d.component.PpqhMotion;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.geom.PgeyMeshData;
import com.sportforce3d.three.geom.PgeyVec3;
import com.sportforce3d.three.human.PgeyHumanModel;
import com.sportforce3d.three.human.PgeyOutfitFitter;
import com.sportforce3d.three.model.PgeyModelData;
import com.sportforce3d.three.model.PgeyModelData.PgeyFocus;
import com.sportforce3d.three.model.PgeyModelData.PgeyPart;
import com.sportforce3d.three.model.PgeyZone;
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

public class PgeyViewer3D extends Region {
    public static final Color PGEY_STAGE_BACKGROUND = Color.web("#07080a");
    public static final Color PGEY_FLOOR_CENTER = Color.web("#14171c");

    private static final double PGEY_ROTATE_RATE = 11;
    private static final double PGEY_IDLE_BEFORE_SPIN_MS = 4000;
    private static final double PGEY_AUTO_SPIN_DEG_PER_S = 9;

    private final Group pgeyWorld = new Group();
    private final Group pgeyModelGroup = new Group();
    private final Group pgeyFloorGroup = new Group();
    private final PerspectiveCamera pgeyCamera = new PerspectiveCamera(true);
    private final SubScene pgeySubScene;

    private final Translate pgeyTargetT = new Translate();
    private final Rotate pgeyYawR = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pgeyPitchR = new Rotate(0, Rotate.X_AXIS);
    private final Translate pgeyDistanceT = new Translate();
    private final Rotate pgeyLightYawR = new Rotate(0, Rotate.Y_AXIS);

    private final PgeyOrbitState pgeyCurrent = new PgeyOrbitState(0, 5, 4, new PgeyVec3(0, 0.93, 0));
    private final PgeyOrbitState pgeyGoal = pgeyCurrent.pgeyCopy();
    private PgeyOrbitState pgeyHome = pgeyCurrent.pgeyCopy();

    private final BooleanProperty pgeyAutoRotate = new SimpleBooleanProperty(this, "autoRotate", true);
    private final Map<PgeyZone, List<MeshView>> pgeyZoneViews = new EnumMap<>(PgeyZone.class);
    private final Map<PgeyZone, PhongMaterial> pgeyZoneMaterials = new EnumMap<>(PgeyZone.class);
    private final Map<javafx.scene.Node, PgeyZone> pgeyViewZones = new java.util.HashMap<>();
    private PgeyModelData pgeyModel;
    private PgeyHumanModel pgeyPerson;
    private PgeyClothLook pgeyClothLook;
    private final Map<HepyGarment, Image> pgeyRawTextures = new EnumMap<>(HepyGarment.class);
    private final Map<PgeyZone, Boolean> pgeyPerforatedZones = new EnumMap<>(PgeyZone.class);
    private final PgeyPerforation pgeyPerforation = new PgeyPerforation();
    private Map<HepyGarment, PgeyFocus> pgeyFocusMap = Map.of();
    private double pgeyFrameHeight;
    private PgeyPickDrag pgeyPickDrag;
    private boolean pgeyDraggingElement;

    public interface PgeyPickDrag {
        boolean pgeyPress(PgeyZone pgeyZone, double pgeyU, double pgeyV);

        void pgeyDrag(PgeyZone pgeyZone, double pgeyU, double pgeyV);

        void pgeyRelease();
    }

    private double pgeyLastX;
    private double pgeyLastY;
    private double pgeyYawVelocity;
    private long pgeyLastInteraction = System.nanoTime();
    private long pgeyLastFrame;
    private final AnimationTimer pgeyTimer = new AnimationTimer() {
        @Override
        public void handle(long pgeyNow) {
            pgeyTick(pgeyNow);
        }
    };

    public PgeyViewer3D() {
        getStyleClass().add("sf-viewer3d");
        pgeyCamera.setFieldOfView(30);
        pgeyCamera.setNearClip(0.05);
        pgeyCamera.setFarClip(60);
        pgeyCamera.getTransforms().add(pgeyDistanceT);

        Group pgeyRig = new Group(pgeyCamera);
        pgeyRig.getTransforms().addAll(pgeyTargetT, pgeyYawR, pgeyPitchR);

        Group pgeyLights = pgeyCreateLights();
        pgeyLights.getTransforms().add(pgeyLightYawR);

        pgeyWorld.getChildren().addAll(pgeyFloorGroup, pgeyModelGroup, pgeyLights, pgeyRig);
        pgeySubScene = new SubScene(pgeyWorld, 400, 400, true, SceneAntialiasing.BALANCED);
        pgeySubScene.setFill(PGEY_STAGE_BACKGROUND);
        pgeySubScene.setCamera(pgeyCamera);
        pgeySubScene.setFocusTraversable(false);
        getChildren().add(pgeySubScene);

        setFocusTraversable(true);
        setAccessibleText("Visor 3D del uniforme. Arrastra para girar, rueda para acercar, doble clic para restablecer.");
        pgeyInstallInput();
        pgeyApply(pgeyCurrent);

        sceneProperty().addListener((pgeyObs, pgeyOld, pgeyScene) -> {
            if (pgeyScene == null) {
                pgeyTimer.stop();
            } else {
                pgeyLastFrame = 0;
                pgeyTimer.start();
            }
        });
    }

    private Group pgeyCreateLights() {
        AmbientLight pgeyAmbient = new AmbientLight(Color.rgb(52, 56, 66));

        DirectionalLight pgeyKey = new DirectionalLight(Color.rgb(255, 242, 226).deriveColor(0, 1, 0.92, 1));
        pgeyKey.setDirection(new Point3D(0.55, 0.62, 0.56));

        DirectionalLight pgeyFill = new DirectionalLight(Color.rgb(150, 176, 226).deriveColor(0, 1, 0.42, 1));
        pgeyFill.setDirection(new Point3D(-0.88, 0.12, 0.46));

        DirectionalLight pgeyRim = new DirectionalLight(Color.rgb(235, 240, 255).deriveColor(0, 1, 0.62, 1));
        pgeyRim.setDirection(new Point3D(-0.30, 0.45, -0.84));

        return new Group(pgeyAmbient, pgeyKey, pgeyFill, pgeyRim);
    }

    public void setModel(PgeyModelData pgeyData) {
        pgeyClothLook = null;
        pgeyShow(pgeyData, null);
    }

    public void setDressedPerson(PgeyOutfitFitter.PgeyDressedPerson pgeyDressed) {
        pgeyClothLook = new PgeyClothLook(pgeyDressed.pgeyShading());
        pgeyShow(pgeyDressed.pgeyOutfit(), pgeyDressed.pgeyBody());
        pgeyZoneMaterials.forEach((pgeyZone, pgeyM) -> {
            if (pgeyClothLook.pgeyCovers(pgeyZone.pgeyGarment())) {
                pgeyM.setBumpMap(pgeyClothLook.pgeyBump(pgeyZone.pgeyGarment(), pgeyM.getBumpMap()));
                pgeyM.setSpecularColor(PgeyClothLook.PGEY_POLYESTER_SPECULAR);
                pgeyM.setSpecularPower(PgeyClothLook.PGEY_POLYESTER_POWER);
            }
        });
    }

    private void pgeyShow(PgeyModelData pgeyData, PgeyHumanModel pgeyHuman) {
        this.pgeyModel = pgeyData;
        this.pgeyPerson = pgeyHuman;
        pgeyRawTextures.clear();
        pgeyPerforatedZones.clear();
        pgeyModelGroup.getChildren().clear();
        pgeyZoneViews.clear();
        pgeyZoneMaterials.clear();
        pgeyViewZones.clear();

        List<MeshView> pgeyTransparent = new ArrayList<>();
        List<PgeyMeshData> pgeyMeshes = new ArrayList<>();
        if (pgeyHuman != null) {
            for (PgeyHumanModel.PgeyPart pgeyPart : pgeyHuman.pgeyParts()) {
                MeshView pgeyView = pgeyPersonView(pgeyPart);
                if (pgeyPart.pgeyTransparent()) {
                    pgeyTransparent.add(pgeyView);
                } else {
                    pgeyModelGroup.getChildren().add(pgeyView);
                }
                pgeyMeshes.add(pgeyPart.pgeyMesh());
            }
        }
        PhongMaterial pgeySkin = PgeyMaterials.pgeyMannequin();
        for (PgeyPart pgeyPart : pgeyData.pgeyMannequin()) {
            MeshView pgeyView = new MeshView(pgeyPart.pgeyMesh().pgeyToTriangleMesh());
            pgeyView.setMaterial(pgeySkin);
            pgeyView.setCullFace(CullFace.BACK);
            pgeyView.setId(pgeyPart.pgeyName());
            pgeyModelGroup.getChildren().add(pgeyView);
        }
        Image pgeyFabric = PgeyFabricMaps.pgeyDryFit();
        pgeyData.pgeyZones().forEach((pgeyZone, pgeyParts) -> {
            PhongMaterial pgeyMaterial = PgeyMaterials.pgeyForZone(pgeyZone, PgeyMaterials.PGEY_DEFAULT_COLORS.get(pgeyZone), pgeyFabric);
            pgeyZoneMaterials.put(pgeyZone, pgeyMaterial);
            List<MeshView> pgeyViews = new ArrayList<>();
            for (PgeyPart pgeyPart : pgeyParts) {
                MeshView pgeyView = new MeshView(pgeyPart.pgeyMesh().pgeyToTriangleMesh());
                pgeyView.setMaterial(pgeyMaterial);
                pgeyView.setCullFace(pgeyZone.pgeyGarment() == HepyGarment.SHOES ? CullFace.BACK : CullFace.NONE);
                pgeyView.setId(pgeyPart.pgeyName());
                pgeyViewZones.put(pgeyView, pgeyZone);
                pgeyViews.add(pgeyView);
            }
            pgeyZoneViews.put(pgeyZone, pgeyViews);
            pgeyModelGroup.getChildren().addAll(pgeyViews);
        });
        pgeyModelGroup.getChildren().addAll(pgeyTransparent);
        pgeyData.pgeyZones().values().forEach(pgeyParts -> pgeyParts.forEach(pgeyP -> pgeyMeshes.add(pgeyP.pgeyMesh())));
        pgeyData.pgeyMannequin().forEach(pgeyP -> pgeyMeshes.add(pgeyP.pgeyMesh()));
        pgeyRebuildFloor(pgeyMeshes, pgeyHuman == null ? 0 : pgeyHuman.pgeyMinY());
        pgeyFrame(pgeyData.pgeyFocus(), pgeyData.pgeyHeight());
    }

    private static MeshView pgeyPersonView(PgeyHumanModel.PgeyPart pgeyPart) {
        PhongMaterial pgeyMaterial = new PhongMaterial(pgeyPart.pgeyDiffuse());
        pgeyMaterial.setDiffuseMap(pgeyPart.pgeyTexture());
        pgeyMaterial.setSpecularColor(pgeyPart.pgeySpecular());
        pgeyMaterial.setSpecularPower(pgeyPart.pgeySpecularPower());
        MeshView pgeyView = new MeshView(pgeyPart.pgeyMesh().pgeyToTriangleMesh());
        pgeyView.setMaterial(pgeyMaterial);
        pgeyView.setCullFace(CullFace.BACK);
        pgeyView.setId(pgeyPart.pgeyName());
        return pgeyView;
    }

    public void setPerson(PgeyHumanModel pgeyHuman) {
        this.pgeyPerson = pgeyHuman;
        this.pgeyModel = null;
        pgeyModelGroup.getChildren().clear();
        pgeyZoneViews.clear();
        pgeyZoneMaterials.clear();
        pgeyViewZones.clear();

        List<PgeyMeshData> pgeyMeshes = new ArrayList<>();
        for (PgeyHumanModel.PgeyPart pgeyPart : pgeyHuman.pgeyParts()) {
            pgeyModelGroup.getChildren().add(pgeyPersonView(pgeyPart));
            pgeyMeshes.add(pgeyPart.pgeyMesh());
        }
        pgeyRebuildFloor(pgeyMeshes, pgeyHuman.pgeyMinY());

        double pgeyH = pgeyHuman.pgeyHeight();
        PgeyFocus pgeyFull = com.sportforce3d.three.human.PgeyOutfitFitter.pgeyFullBody(pgeyHuman.pgeyMinY(), pgeyH);
        Map<HepyGarment, PgeyFocus> pgeyAll = new EnumMap<>(HepyGarment.class);
        for (HepyGarment pgeyG : HepyGarment.values()) {
            pgeyAll.put(pgeyG, pgeyFull);
        }
        pgeyFrame(pgeyAll, pgeyH);
    }

    private void pgeyFrame(Map<HepyGarment, PgeyFocus> pgeyFocus, double pgeyH) {
        boolean pgeyKeep = !pgeyFocusMap.isEmpty() && pgeyFrameHeight > 0;
        double pgeyK = pgeyKeep ? pgeyH / pgeyFrameHeight : 1;
        pgeyFocusMap = pgeyFocus;
        pgeyFrameHeight = pgeyH;
        PgeyFocus pgeyFull = pgeyFocus.get(HepyGarment.KIT);
        pgeyCurrent.pgeySetDistanceLimits(0.55 * pgeyH / 1.812, 6.5 * pgeyH / 1.812);
        pgeyGoal.pgeySetDistanceLimits(0.55 * pgeyH / 1.812, 6.5 * pgeyH / 1.812);
        pgeyHome = new PgeyOrbitState(0, pgeyFull.pgeyPitch(), pgeyFull.pgeyDistance(), pgeyFull.pgeyTarget());
        pgeyHome.pgeySetDistanceLimits(pgeyCurrent.pgeyMinDistance(), pgeyCurrent.pgeyMaxDistance());
        if (pgeyKeep) {
            pgeyGoal.pgeyAimAt(pgeyGoal.pgeyYaw(), pgeyGoal.pgeyPitch(), pgeyGoal.pgeyDistance() * pgeyK, pgeyScaled(pgeyGoal.pgeyTarget(), pgeyK));
            pgeyCurrent.pgeySet(pgeyGoal);
        } else {
            pgeyGoal.pgeySet(pgeyHome);
            pgeyCurrent.pgeySet(pgeyHome);
        }
        pgeyYawVelocity = 0;
        pgeyApply(pgeyCurrent);
    }

    private static PgeyVec3 pgeyScaled(PgeyVec3 pgeyV, double pgeyK) {
        return new PgeyVec3(pgeyV.pgeyX() * pgeyK, pgeyV.pgeyY() * pgeyK, pgeyV.pgeyZ() * pgeyK);
    }

    public PgeyOrbitState pgeyCameraGoal() {
        return pgeyGoal.pgeyCopy();
    }

    public PgeyModelData getModel() {
        return pgeyModel;
    }

    public PgeyHumanModel getPerson() {
        return pgeyPerson;
    }

    public boolean pgeyHasContent() {
        return !pgeyFocusMap.isEmpty();
    }

    public PhongMaterial pgeyMaterial(PgeyZone pgeyZone) {
        return pgeyZoneMaterials.get(pgeyZone);
    }

    public void pgeySetGarmentTexture(HepyGarment pgeyGarment, Image pgeyTexture) {
        pgeyRawTextures.put(pgeyGarment, pgeyTexture);
        pgeyShowTexture(pgeyGarment);
    }

    private void pgeyShowTexture(HepyGarment pgeyGarment) {
        Image pgeyTexture = pgeyRawTextures.get(pgeyGarment);
        if (pgeyTexture == null) {
            return;
        }
        List<com.sportforce3d.three.geom.PgeyUvRect> pgeyHoles = new ArrayList<>();
        pgeyPerforatedZones.forEach((pgeyZone, pgeyOn) -> {
            if (pgeyOn && pgeyZone.pgeyGarment() == pgeyGarment) {
                pgeyHoles.addAll(com.sportforce3d.three.model.PgeyUvLayout.PGEY_BY_ZONE.getOrDefault(pgeyZone, List.of()));
            }
        });
        if (!pgeyHoles.isEmpty()) {
            pgeyTexture = pgeyPerforation.pgeyApply(pgeyGarment, pgeyTexture, pgeyHoles);
        }
        Image pgeyShown = pgeyClothLook != null ? pgeyClothLook.pgeyShade(pgeyGarment, pgeyTexture) : pgeyTexture;
        pgeyZoneMaterials.forEach((pgeyZone, pgeyM) -> {
            if (pgeyZone.pgeyGarment() == pgeyGarment) {
                pgeyM.setDiffuseColor(Color.WHITE);
                pgeyM.setDiffuseMap(pgeyShown);
            }
        });
    }

    public void pgeySetLook(java.util.Collection<PgeyZone> pgeyZones, PgeyMaterialLook pgeyLook) {
        java.util.Set<HepyGarment> pgeyRetexture = java.util.EnumSet.noneOf(HepyGarment.class);
        for (PgeyZone pgeyZ : pgeyZones) {
            if (pgeyPerforatedZones.getOrDefault(pgeyZ, false) != pgeyLook.pgeyPerforated()) {
                pgeyPerforatedZones.put(pgeyZ, pgeyLook.pgeyPerforated());
                pgeyRetexture.add(pgeyZ.pgeyGarment());
            }
            PhongMaterial pgeyM = pgeyZoneMaterials.get(pgeyZ);
            if (pgeyM == null) {
                continue;
            }
            if (pgeyClothLook != null && pgeyClothLook.pgeyCovers(pgeyZ.pgeyGarment())) {
                pgeyM.setBumpMap(pgeyClothLook.pgeyBump(pgeyZ.pgeyGarment(), pgeyLook.pgeyBump()));
                pgeyM.setSpecularColor(pgeyLook.pgeySpecular());
                pgeyM.setSpecularPower(Math.max(6, Math.min(40, pgeyLook.pgeyPower())));
            } else {
                pgeyM.setBumpMap(pgeyLook.pgeyBump());
                pgeyM.setSpecularColor(pgeyLook.pgeySpecular());
                pgeyM.setSpecularPower(pgeyLook.pgeyPower());
            }
        }
        pgeyRetexture.forEach(this::pgeyShowTexture);
    }

    public void setPickDrag(PgeyPickDrag pgeyHandler) {
        this.pgeyPickDrag = pgeyHandler;
    }

    public WritableImage pgeyCaptureView(PgeyViewPreset pgeyPreset, int pgeyWidth) {
        PgeyOrbitState pgeySavedCurrent = pgeyCurrent.pgeyCopy();
        PgeyOrbitState pgeySavedGoal = pgeyGoal.pgeyCopy();
        pgeyJumpTo(pgeyPreset, HepyGarment.KIT);
        WritableImage pgeyImg = pgeyCapture(pgeyWidth);
        pgeyCurrent.pgeySet(pgeySavedCurrent);
        pgeyGoal.pgeySet(pgeySavedGoal);
        pgeyApply(pgeyCurrent);
        return pgeyImg;
    }

    public List<MeshView> pgeyViews(PgeyZone pgeyZone) {
        return Collections.unmodifiableList(pgeyZoneViews.getOrDefault(pgeyZone, List.of()));
    }

    private void pgeyRebuildFloor(List<PgeyMeshData> pgeyMeshes, double pgeyFloorY) {
        List<float[]> pgeyNear = new ArrayList<>();
        double pgeyMinX = Double.MAX_VALUE;
        double pgeyMaxX = -Double.MAX_VALUE;
        double pgeySumZ = 0;
        int pgeyCount = 0;
        for (PgeyMeshData pgeyM : pgeyMeshes) {
            float[] pgeyPts = pgeyM.pgeyPoints();
            for (int pgeyI = 0; pgeyI < pgeyPts.length; pgeyI += 3) {
                if (-pgeyPts[pgeyI + 1] - pgeyFloorY < 0.03) {
                    pgeyNear.add(new float[]{pgeyPts[pgeyI], pgeyPts[pgeyI + 1], pgeyPts[pgeyI + 2]});
                    pgeyMinX = Math.min(pgeyMinX, pgeyPts[pgeyI]);
                    pgeyMaxX = Math.max(pgeyMaxX, pgeyPts[pgeyI]);
                    pgeySumZ += pgeyPts[pgeyI + 2];
                    pgeyCount++;
                }
            }
        }
        double pgeyCx = pgeyCount == 0 ? 0 : (pgeyMinX + pgeyMaxX) / 2;
        double pgeyCz = pgeyCount == 0 ? 0 : pgeySumZ / pgeyCount;
        pgeyFloorGroup.getChildren().setAll(PgeyStudioFloor.pgeyCreate(pgeyNear, pgeyCx, pgeyCz, PGEY_STAGE_BACKGROUND, PGEY_FLOOR_CENTER));
    }

    public void pgeyShowView(PgeyViewPreset pgeyPreset) {
        pgeyGoal.pgeyAimAt(pgeyPreset.pgeyYaw(), pgeyGoal.pgeyPitch(), pgeyGoal.pgeyDistance(), pgeyGoal.pgeyTarget());
        pgeyInteracted();
        pgeySnapIfReduced();
    }

    public void pgeyFocus(HepyGarment pgeyGarment) {
        if (pgeyFocusMap.isEmpty()) {
            return;
        }
        PgeyFocus pgeyF = pgeyFocusMap.getOrDefault(pgeyGarment, pgeyFocusMap.get(HepyGarment.KIT));
        pgeyGoal.pgeyAimAt(pgeyGoal.pgeyYaw(), pgeyF.pgeyPitch(), pgeyF.pgeyDistance(), pgeyF.pgeyTarget());
        pgeyInteracted();
        pgeySnapIfReduced();
    }

    public void pgeyResetView() {
        pgeyGoal.pgeyAimAt(pgeyHome.pgeyYaw(), pgeyHome.pgeyPitch(), pgeyHome.pgeyDistance(), pgeyHome.pgeyTarget());
        pgeyInteracted();
        pgeySnapIfReduced();
    }

    public void pgeyZoomBy(double pgeyFactor) {
        pgeyGoal.pgeyZoom(pgeyFactor);
        pgeyInteracted();
        pgeySnapIfReduced();
    }

    public void pgeyJumpTo(PgeyViewPreset pgeyPreset, HepyGarment pgeyGarment) {
        if (pgeyFocusMap.isEmpty()) {
            return;
        }
        PgeyFocus pgeyF = pgeyFocusMap.getOrDefault(pgeyGarment, pgeyFocusMap.get(HepyGarment.KIT));
        pgeyGoal.pgeyAimAt(pgeyPreset.pgeyYaw(), pgeyF.pgeyPitch(), pgeyF.pgeyDistance(), pgeyF.pgeyTarget());
        pgeyCurrent.pgeySet(pgeyGoal);
        pgeyYawVelocity = 0;
        pgeyApply(pgeyCurrent);
    }

    public void pgeyJumpToFace() {
        if (pgeyPerson == null) {
            return;
        }
        double pgeyH = pgeyPerson.pgeyHeight();
        pgeyGoal.pgeyAimAt(pgeyGoal.pgeyYaw(), 0, pgeyCurrent.pgeyMinDistance() * 1.25, new PgeyVec3(0, pgeyPerson.pgeyMinY() + 0.93 * pgeyH, 0.03 * pgeyH));
        pgeyCurrent.pgeySet(pgeyGoal);
        pgeyYawVelocity = 0;
        pgeyApply(pgeyCurrent);
    }

    public BooleanProperty autoRotateProperty() {
        return pgeyAutoRotate;
    }

    private void pgeySnapIfReduced() {
        if (PpqhMotion.isReduced()) {
            pgeyCurrent.pgeySet(pgeyGoal);
            pgeyApply(pgeyCurrent);
        }
    }

    private void pgeyInteracted() {
        pgeyLastInteraction = System.nanoTime();
    }

    private void pgeyTick(long pgeyNow) {
        double pgeyDt = pgeyLastFrame == 0 ? 1 / 60.0 : Math.min(0.05, (pgeyNow - pgeyLastFrame) / 1e9);
        pgeyLastFrame = pgeyNow;

        if (Math.abs(pgeyYawVelocity) > 0.01) {
            pgeyGoal.pgeyRotate(pgeyYawVelocity, 0);
            pgeyYawVelocity *= Math.pow(0.0025, pgeyDt);
        } else {
            pgeyYawVelocity = 0;
        }
        boolean pgeyIdle = (pgeyNow - pgeyLastInteraction) / 1e6 > PGEY_IDLE_BEFORE_SPIN_MS;
        if (pgeyAutoRotate.get() && pgeyIdle && !PpqhMotion.isReduced() && pgeyHasContent()) {
            pgeyGoal.pgeyRotate(PGEY_AUTO_SPIN_DEG_PER_S * pgeyDt, 0);
        }
        if (PpqhMotion.isReduced()) {
            pgeyCurrent.pgeySet(pgeyGoal);
        } else {
            pgeyCurrent.pgeyApproach(pgeyGoal, PgeyOrbitState.pgeyAlpha(pgeyDt, PGEY_ROTATE_RATE));
        }
        pgeyApply(pgeyCurrent);
    }

    private void pgeyApply(PgeyOrbitState pgeyS) {
        PgeyVec3 pgeyT = pgeyS.pgeyTarget();
        pgeyTargetT.setX(pgeyT.pgeyX());
        pgeyTargetT.setY(-pgeyT.pgeyY());
        pgeyTargetT.setZ(-pgeyT.pgeyZ());
        pgeyYawR.setAngle(pgeyS.pgeyYaw());
        pgeyLightYawR.setAngle(pgeyS.pgeyYaw());
        pgeyPitchR.setAngle(-pgeyS.pgeyPitch());
        pgeyDistanceT.setZ(-pgeyS.pgeyDistance());
    }

    private void pgeyInstallInput() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, pgeyE -> {
            requestFocus();
            pgeyLastX = pgeyE.getSceneX();
            pgeyLastY = pgeyE.getSceneY();
            pgeyYawVelocity = 0;
            pgeyInteracted();
            pgeyDraggingElement = false;
            if (pgeyPickDrag != null && pgeyE.getButton() == MouseButton.PRIMARY && pgeyE.getClickCount() == 1) {
                double[] pgeyHit = pgeyPickUv(pgeyE);
                PgeyZone pgeyZone = pgeyPickZone(pgeyE);
                pgeyDraggingElement = pgeyHit != null && pgeyZone != null && pgeyPickDrag.pgeyPress(pgeyZone, pgeyHit[0], pgeyHit[1]);
            }
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, pgeyE -> {
            if (pgeyDraggingElement) {
                double[] pgeyHit = pgeyPickUv(pgeyE);
                PgeyZone pgeyZone = pgeyPickZone(pgeyE);
                pgeyPickDrag.pgeyDrag(pgeyHit == null ? null : pgeyZone, pgeyHit == null ? 0 : pgeyHit[0], pgeyHit == null ? 0 : pgeyHit[1]);
                pgeyInteracted();
                return;
            }
            double pgeyDx = pgeyE.getSceneX() - pgeyLastX;
            double pgeyDy = pgeyE.getSceneY() - pgeyLastY;
            pgeyLastX = pgeyE.getSceneX();
            pgeyLastY = pgeyE.getSceneY();
            if (pgeyE.getButton() == MouseButton.PRIMARY && !pgeyE.isSynthesized()) {
                pgeyGoal.pgeyRotate(pgeyDx * 0.4, pgeyDy * 0.25);
                pgeyYawVelocity = pgeyDx * 0.4 * 0.5;
            } else if (pgeyE.getButton() == MouseButton.SECONDARY || pgeyE.getButton() == MouseButton.MIDDLE) {
                pgeyGoal.pgeyPan(pgeyDy * pgeyGoal.pgeyDistance() * 0.0012);
            }
            pgeyInteracted();
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, pgeyE -> {
            if (pgeyDraggingElement) {
                pgeyDraggingElement = false;
                pgeyPickDrag.pgeyRelease();
            }
            pgeyInteracted();
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, pgeyE -> {
            if (pgeyE.getButton() == MouseButton.PRIMARY && pgeyE.getClickCount() == 2) {
                pgeyYawVelocity = 0;
                pgeyResetView();
            }
        });
        setOnScroll(pgeyE -> {
            if (pgeyE.getTouchCount() >= 2) {
                return;
            }
            if (pgeyE.getTouchCount() == 1) {
                pgeyGoal.pgeyRotate(pgeyE.getDeltaX() * 0.3, pgeyE.getDeltaY() * 0.2);
            } else if (pgeyE.getDeltaY() != 0) {
                pgeyGoal.pgeyZoom(Math.exp(-pgeyE.getDeltaY() * 0.0018));
            }
            pgeyInteracted();
            pgeyE.consume();
        });
        setOnZoom(pgeyE -> {
            pgeyGoal.pgeyZoom(1 / pgeyE.getZoomFactor());
            pgeyInteracted();
            pgeyE.consume();
        });
        setOnRotate(pgeyE -> {
            pgeyGoal.pgeyRotate(-pgeyE.getAngle(), 0);
            pgeyInteracted();
            pgeyE.consume();
        });
        setOnKeyPressed(pgeyE -> {
            KeyCode pgeyK = pgeyE.getCode();
            switch (pgeyK) {
                case LEFT -> pgeyGoal.pgeyRotate(15, 0);
                case RIGHT -> pgeyGoal.pgeyRotate(-15, 0);
                case UP -> pgeyGoal.pgeyRotate(0, 6);
                case DOWN -> pgeyGoal.pgeyRotate(0, -6);
                case PLUS, ADD, EQUALS -> pgeyGoal.pgeyZoom(0.85);
                case MINUS, SUBTRACT -> pgeyGoal.pgeyZoom(1 / 0.85);
                case DIGIT1, NUMPAD1 -> pgeyShowView(PgeyViewPreset.FRONT);
                case DIGIT2, NUMPAD2 -> pgeyShowView(PgeyViewPreset.BACK);
                case DIGIT3, NUMPAD3 -> pgeyShowView(PgeyViewPreset.LEFT);
                case DIGIT4, NUMPAD4 -> pgeyShowView(PgeyViewPreset.RIGHT);
                case DIGIT0, NUMPAD0, R -> pgeyResetView();
                default -> {
                    return;
                }
            }
            pgeyInteracted();
            pgeySnapIfReduced();
            pgeyE.consume();
        });
    }

    private PgeyZone pgeyPickZone(MouseEvent pgeyE) {
        javafx.scene.input.PickResult pgeyPr = pgeyE.getPickResult();
        return pgeyPr == null || pgeyPr.getIntersectedNode() == null ? null : pgeyViewZones.get(pgeyPr.getIntersectedNode());
    }

    private static double[] pgeyPickUv(MouseEvent pgeyE) {
        javafx.scene.input.PickResult pgeyPr = pgeyE.getPickResult();
        if (pgeyPr == null || pgeyPr.getIntersectedTexCoord() == null) {
            return null;
        }
        return new double[]{pgeyPr.getIntersectedTexCoord().getX(), pgeyPr.getIntersectedTexCoord().getY()};
    }

    @Override
    protected void layoutChildren() {
        pgeySubScene.setWidth(getWidth());
        pgeySubScene.setHeight(getHeight());
    }

    @Override
    protected double computePrefWidth(double pgeyHeight) {
        return 640;
    }

    @Override
    protected double computePrefHeight(double pgeyWidth) {
        return 720;
    }

    public WritableImage pgeyCapture(int pgeyWidth) {
        double pgeyK = pgeyWidth / Math.max(1, pgeySubScene.getWidth());
        SnapshotParameters pgeyParams = new SnapshotParameters();
        pgeyParams.setFill(PGEY_STAGE_BACKGROUND);
        pgeyParams.setTransform(Transform.scale(pgeyK, pgeyK));
        return pgeySubScene.snapshot(pgeyParams, null);
    }
}
