package com.sportforce3d.ui;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.design.PgeyElementType;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.three.PgeyOrbitState;
import com.sportforce3d.three.PgeyViewer3D;
import com.sportforce3d.three.model.PgeyZone;
import com.sportforce3d.viewmodel.PgeyDesignerViewModel;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.function.Predicate;

import static com.sportforce3d.ui.UiTestSupport.click;
import static com.sportforce3d.ui.UiTestSupport.type;
import static com.sportforce3d.ui.UiTestSupport.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(ApplicationExtension.class)
class DesignerUiTest {

    @TempDir
    Path tempDir;

    private PpqhAppContext context;

    @Start
    void start(Stage stage) {
        context = UiTestSupport.start(stage, tempDir);
    }

    @Test
    void editingKeepsTheCameraAndTextOptionsEditInsideTheirCard(FxRobot robot) {
        type(robot, "Correo", "cliente@sportforce3d.pe");
        type(robot, "Contraseña", "Cliente123!");
        click(robot, "Iniciar sesión");
        waitUntil("la tienda", () -> UiTestSupport.labeled(robot, "Catálogo").isPresent());
        robot.interact(() -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, null));
        click(robot, "Vista 3D");
        PgeyViewer3D viewer = robot.lookup(".sf-viewer3d").queryAs(PgeyViewer3D.class);
        PgeyDesignerViewModel vm = viewModel(robot);
        waitUntil("persona o maniquí en el visor", () -> viewer.getModel() != null);

        click(robot, "Espalda");
        robot.interact(() -> viewer.pgeyZoomBy(0.82));
        PgeyOrbitState before = viewer.pgeyCameraGoal();
        assertThat(Math.abs(before.pgeyYaw())).isCloseTo(180, within(0.5));

        robot.interact(() -> vm.pgeyEdit("Color", d -> d.pgeyWithColor(PgeyZone.JERSEY_BODY, "#D7141E"), null));
        robot.interact(() -> vm.garmentProperty().set(HepyGarment.SHORTS));
        robot.interact(() -> vm.garmentProperty().set(HepyGarment.JERSEY));

        click(robot, "Texto");
        click(robot, "Número en el pecho");
        waitUntil("editor del número del pecho", () -> card(robot, "Número en el pecho") != null
                && UiTestSupport.field(robot, "Número (0–99)").isPresent());
        Node chestCard = card(robot, "Número en el pecho");
        assertThat(isInside(UiTestSupport.field(robot, "Número (0–99)").orElseThrow(), chestCard)).isTrue();

        click(robot, "Nombre en la espalda");
        waitUntil("editor del nombre", () -> UiTestSupport.field(robot, "Nombre (máx. 12)").isPresent());
        Node nameCard = card(robot, "Nombre en la espalda");
        assertThat(isInside(UiTestSupport.field(robot, "Nombre (máx. 12)").orElseThrow(), nameCard)).isTrue();
        robot.interact(() -> UiTestSupport.field(robot, "Nombre (máx. 12)").orElseThrow().getInput().clear());
        type(robot, "Nombre (máx. 12)", "quispe");
        waitUntil("nombre en el documento", () -> vm.pgeyDoc().pgeyElements().stream()
                .anyMatch(e -> e.pgeyType() == PgeyElementType.NAME && "QUISPE".equals(e.pgeyText())));

        WaitForAsyncUtils.sleep(4500, java.util.concurrent.TimeUnit.MILLISECONDS);
        WaitForAsyncUtils.waitForFxEvents();
        PgeyOrbitState after = viewer.pgeyCameraGoal();
        assertThat(after.pgeyYaw()).isCloseTo(before.pgeyYaw(), within(1e-6));
        assertThat(after.pgeyPitch()).isCloseTo(before.pgeyPitch(), within(1e-6));
        assertThat(after.pgeyDistance()).isCloseTo(before.pgeyDistance(), within(1e-6));
        assertThat(after.pgeyTarget()).isEqualTo(before.pgeyTarget());

        Node remove = robot.lookup((Predicate<Node>) n -> "Quitar nombre en la espalda".equals(n.getAccessibleText())
                && isInside(n, card(robot, "Nombre en la espalda"))).query();
        UiTestSupport.clickNode(robot, remove);
        waitUntil("nombre quitado", () -> vm.pgeyDoc().pgeyElements().stream().noneMatch(e -> e.pgeyType() == PgeyElementType.NAME));
        assertThat(vm.pgeyDoc().pgeyElements()).hasSize(1);
    }

    @Test
    void longSleeveDesignsAreSavedWithPhotosFromThe3dView(FxRobot robot) throws Exception {
        type(robot, "Correo", "cliente@sportforce3d.pe");
        type(robot, "Contraseña", "Cliente123!");
        click(robot, "Iniciar sesión");
        waitUntil("la tienda", () -> UiTestSupport.labeled(robot, "Catálogo").isPresent());
        robot.interact(() -> context.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, null));
        PgeyDesignerViewModel vm = viewModel(robot);

        saveAs(robot, "Serie");
        java.awt.image.BufferedImage serie = savedFront("Serie");
        assertThat(luminance(serie.getRGB(4, 4))).as("fondo de estudio de la foto realista").isGreaterThan(60);

        robot.interact(() -> vm.pgeyEdit("Manga larga", d -> d.pgeyWithCut(d.pgeyCut().pgeyWithSleeve(
                com.sportforce3d.design.PgeyGarmentCut.PgeySleeve.LARGA)), null));
        saveAs(robot, "Manga larga");
        java.awt.image.BufferedImage foto = savedFront("Manga larga");
        assertThat((double) foto.getWidth() / foto.getHeight()).as("foto realista 720 × 900").isCloseTo(0.8, within(0.01));

        robot.interact(() -> vm.pgeyEdit("Short corto", d -> d.pgeyWithCut(d.pgeyCut().pgeyWithShorts(
                com.sportforce3d.design.PgeyGarmentCut.PgeyShortsLength.CORTO)), null));
        saveAs(robot, "Short corto");
        java.awt.image.BufferedImage larga = savedFront("Short corto");
        assertThat((double) larga.getWidth() / larga.getHeight()).isCloseTo(0.75, within(0.01));
        assertThat(luminance(larga.getRGB(4, 4))).as("fondo oscuro del visor 3D").isLessThan(30);
        int centre = larga.getRGB(larga.getWidth() / 2, (int) (larga.getHeight() * 0.40));
        assertThat(luminance(centre)).as("la persona en el centro").isGreaterThan(30);
    }

    private void saveAs(FxRobot robot, String name) {
        javafx.scene.control.Labeled save = UiTestSupport.waitLabeled(robot, "Guardar diseño");
        robot.interact(() -> ((javafx.scene.control.ButtonBase) save).fire());
        waitUntil("diálogo de guardar", () -> UiTestSupport.field(robot, "Nombre del diseño").isPresent());
        robot.interact(() -> UiTestSupport.field(robot, "Nombre del diseño").orElseThrow().getInput().setText(name));
        click(robot, "Guardar");
        waitUntil("diseño guardado: " + name, 90, () -> context.ppqhDesignService().pgeyMyDesigns().stream()
                .anyMatch(d -> d.pgeyName().equals(name)));
    }

    private java.awt.image.BufferedImage savedFront(String name) throws Exception {
        String path = context.ppqhDesignService().pgeyMyDesigns().stream().filter(d -> d.pgeyName().equals(name)).findFirst()
                .orElseThrow().pgeyThumbnailFront();
        return javax.imageio.ImageIO.read(context.ppqhStorage().hepyResolve(path).toFile());
    }

    private static int luminance(int argb) {
        return (int) (0.3 * ((argb >> 16) & 0xFF) + 0.59 * ((argb >> 8) & 0xFF) + 0.11 * (argb & 0xFF));
    }

    private static PgeyDesignerViewModel viewModel(FxRobot robot) {
        Node root = robot.lookup((Predicate<Node>) n -> n.getProperties().containsKey(PgeyDesignerViewModel.class)).query();
        return (PgeyDesignerViewModel) root.getProperties().get(PgeyDesignerViewModel.class);
    }

    private static Node card(FxRobot robot, String label) {
        for (Node c : robot.lookup(".sf-text-card").queryAll()) {
            if (c instanceof Parent p && p.lookupAll(".sf-btn").stream()
                    .anyMatch(b -> b instanceof Labeled l && label.equals(l.getText()))) {
                return c;
            }
        }
        return null;
    }

    private static boolean isInside(Node node, Node ancestor) {
        for (Node p = node; p != null; p = p.getParent()) {
            if (p == ancestor) {
                return true;
            }
        }
        return false;
    }
}
