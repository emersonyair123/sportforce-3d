package com.sportforce3d.ui;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhFonts;
import com.sportforce3d.app.PpqhSportForceApp;
import com.sportforce3d.app.PpqhUiSettings;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.config.PpqhAppConfig;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Labeled;
import javafx.stage.Stage;
import org.testfx.api.FxRobot;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.prefs.Preferences;

final class UiTestSupport {

    static final int TIMEOUT_SECONDS = 20;

    private UiTestSupport() {
    }

    static PpqhAppContext start(Stage stage, Path tempDir) {
        PpqhFonts.ppqhLoadAll();
        PpqhUiSettings settings = new PpqhUiSettings(Preferences.userRoot().node("com/sportforce3d-test"));
        settings.reduceMotionProperty().set(true);
        PpqhAppContext context = new PpqhAppContext(new PpqhAppConfig(tempDir.resolve("data").resolve("ui.db")), settings);
        context.ppqhDatabase().hepyMigrate();
        PpqhSportForceApp.ppqhOpenMainWindow(stage, context);
        stage.show();
        return context;
    }

    static void waitUntil(String what, java.util.concurrent.Callable<Boolean> condition) {
        waitUntil(what, TIMEOUT_SECONDS, condition);
    }

    static void waitUntil(String what, int seconds, java.util.concurrent.Callable<Boolean> condition) {
        try {
            WaitForAsyncUtils.waitFor(seconds, TimeUnit.SECONDS, condition);
        } catch (Exception e) {
            throw new AssertionError("No apareció a tiempo: " + what + "\nTextos visibles: " + visibleTexts()
                    + "\nCaptura: " + saveFailureShot(what), e);
        }
        WaitForAsyncUtils.waitForFxEvents();
    }

    private static String visibleTexts() {
        java.util.List<String> texts = new java.util.ArrayList<>();
        for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
            if (w.getScene() != null) {
                for (Node n : w.getScene().getRoot().lookupAll(".label")) {
                    if (n instanceof Labeled l && l.getText() != null && !l.getText().isBlank() && visibleChain(n)) {
                        texts.add(l.getText());
                    }
                }
            }
        }
        return texts.toString();
    }

    private static String saveFailureShot(String what) {
        try {
            Path dir = Path.of("target", "ui-fallos");
            java.nio.file.Files.createDirectories(dir);
            Path file = dir.resolve(what.replaceAll("[^A-Za-z0-9]+", "-") + ".png");
            java.util.concurrent.CompletableFuture<javafx.scene.image.WritableImage> img = new java.util.concurrent.CompletableFuture<>();
            javafx.application.Platform.runLater(() -> {
                javafx.stage.Window w = javafx.stage.Window.getWindows().stream().filter(javafx.stage.Window::isShowing)
                        .findFirst().orElse(null);
                img.complete(w == null ? null : w.getScene().snapshot(null));
            });
            javafx.scene.image.WritableImage image = img.get(5, TimeUnit.SECONDS);
            if (image != null) {
                com.sportforce3d.three.PgeyPng.pgeyWrite(image, file);
            }
            return file.toAbsolutePath().toString();
        } catch (Exception e) {
            return "sin captura (" + e + ")";
        }
    }

    static Optional<Labeled> labeled(FxRobot robot, String text) {
        return robot.lookup((Predicate<Node>) n -> n instanceof Labeled l && text.equals(l.getText()) && n.isVisible()
                        && !n.isDisabled() && n.getScene() != null && visibleChain(n))
                .queryAllAs(Labeled.class).stream()
                .min(Comparator.comparingInt(UiTestSupport::depth).reversed());
    }

    static Labeled waitLabeled(FxRobot robot, String text) {
        waitUntil("\"" + text + "\"", () -> labeled(robot, text).isPresent());
        return labeled(robot, text).orElseThrow();
    }

    static void click(FxRobot robot, String text) {
        clickNode(robot, waitLabeled(robot, text));
    }

    static void clickNode(FxRobot robot, Node node) {
        robot.interact(() -> scrollIntoView(node));
        robot.clickOn(node, MouseButton.PRIMARY);
        WaitForAsyncUtils.waitForFxEvents();
    }

    static void scrollIntoView(Node node) {
        for (Node p = node.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp && sp.getContent() != null) {
                Bounds viewport = sp.getViewportBounds();
                Bounds content = sp.getContent().getLayoutBounds();
                Bounds inContent = sp.getContent().sceneToLocal(node.localToScene(node.getLayoutBounds()));
                double extra = content.getHeight() - viewport.getHeight();
                if (extra > 0) {
                    double target = inContent.getMinY() - (viewport.getHeight() - inContent.getHeight()) / 2;
                    sp.setVvalue(Math.max(0, Math.min(1, target / extra)) * (sp.getVmax() - sp.getVmin()) + sp.getVmin());
                    sp.layout();
                }
            }
        }
    }

    static void type(FxRobot robot, String fieldLabel, String value) {
        waitUntil("campo " + fieldLabel, () -> field(robot, fieldLabel).isPresent());
        PpqhSfTextField field = field(robot, fieldLabel).orElseThrow();
        clickNode(robot, field.getInput());
        robot.write(value);
        WaitForAsyncUtils.waitForFxEvents();
    }

    static Optional<PpqhSfTextField> field(FxRobot robot, String label) {
        return robot.lookup((Predicate<Node>) n -> n instanceof PpqhSfTextField f && visibleChain(f)
                        && f.lookupAll(".sf-field-label").stream()
                        .anyMatch(l -> l instanceof Labeled ll && ll.getText().equalsIgnoreCase(label)))
                .queryAllAs(PpqhSfTextField.class).stream().findFirst();
    }

    static boolean textShown(FxRobot robot, Predicate<String> match) {
        return robot.lookup((Predicate<Node>) n -> n instanceof Labeled l && l.getText() != null
                && match.test(l.getText()) && visibleChain(n)).tryQuery().isPresent();
    }

    private static boolean visibleChain(Node n) {
        for (Node p = n; p != null; p = p.getParent()) {
            if (!p.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private static int depth(Node n) {
        int d = 0;
        for (Node p = n; p != null; p = p.getParent()) {
            d++;
        }
        return d;
    }
}
