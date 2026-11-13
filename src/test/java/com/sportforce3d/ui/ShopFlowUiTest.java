package com.sportforce3d.ui;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.component.HepyFilterChip;
import com.sportforce3d.component.HepyProductCard;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.function.Predicate;

import static com.sportforce3d.ui.UiTestSupport.click;
import static com.sportforce3d.ui.UiTestSupport.textShown;
import static com.sportforce3d.ui.UiTestSupport.type;
import static com.sportforce3d.ui.UiTestSupport.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class ShopFlowUiTest {

    @TempDir
    Path tempDir;

    private PpqhAppContext context;

    @Start
    void start(Stage stage) {
        context = UiTestSupport.start(stage, tempDir);
    }

    @Test
    void clientBuysAShirtFromLoginToPendingOrder(FxRobot robot) {
        type(robot, "Correo", "cliente@sportforce3d.pe");
        type(robot, "Contraseña", "equivocada");
        click(robot, "Iniciar sesión");
        waitUntil("error de credenciales", () -> textShown(robot, "Correo o contraseña incorrectos."::equals));

        UiTestSupport.clickNode(robot, UiTestSupport.field(robot, "Contraseña").orElseThrow().getInput());
        robot.push(KeyCode.CONTROL, KeyCode.A).write("Cliente123!");
        click(robot, "Iniciar sesión");
        waitUntil("la tienda", () -> UiTestSupport.labeled(robot, "Catálogo").isPresent());

        click(robot, "Catálogo");
        waitUntil("tarjetas del catálogo", () -> !robot.lookup(".sf-product-card").queryAll().isEmpty());
        UiTestSupport.clickNode(robot, robot.lookup(".sf-search .text-field").<Node>query());
        robot.write("camiseta pro dry").push(KeyCode.ENTER);
        waitUntil("la Camiseta Pro Dry-Fit", () -> card(robot, "Camiseta Pro Dry-Fit") != null);
        UiTestSupport.clickNode(robot, card(robot, "Camiseta Pro Dry-Fit").lookup(".title-s"));

        waitUntil("tallas", () -> size(robot, "M") != null);
        UiTestSupport.clickNode(robot, size(robot, "M"));
        UiTestSupport.clickNode(robot, robot.lookup((Predicate<Node>) n -> "Aumentar cantidad".equals(n.getAccessibleText())).<Node>query());
        click(robot, "Agregar al carrito");
        waitUntil("contador del carrito", () -> context.cartCountProperty().get() == 2);

        click(robot, "Carrito");
        waitUntil("resumen del carrito", () -> UiTestSupport.labeled(robot, "Continuar al pago").isPresent());
        assertThat(textShown(robot, t -> t.contains("Camiseta Pro Dry-Fit"))).isTrue();
        click(robot, "Continuar al pago");
        waitUntil("checkout", () -> UiTestSupport.labeled(robot, "Confirmar pedido").isPresent());
        click(robot, "Yape");
        click(robot, "Confirmar pedido");

        waitUntil("número de pedido", () -> textShown(robot, t -> t.matches("PED-\\d{4}-\\d{6}")));
        waitUntil("estado", () -> textShown(robot, t -> t.equalsIgnoreCase("Pendiente de pago")));
        assertThat(UiTestSupport.labeled(robot, "Enviar comprobante")).isPresent();
        assertThat(context.cartCountProperty().get()).isZero();

        WaitForAsyncUtils.waitForFxEvents();
        assertThat(query("SELECT count(*) FROM orders WHERE status = 'PENDIENTE_PAGO'")).isEqualTo(1);
        assertThat(query("""
                SELECT i.reserved FROM inventory i JOIN product_variants v ON v.id = i.variant_id
                WHERE v.sku = 'SF-CAM-001-M'""")).isEqualTo(2);
        assertThat(query("SELECT count(*) FROM cart_items")).isZero();
    }

    private static Node card(FxRobot robot, String name) {
        return robot.lookup((Predicate<Node>) n -> n instanceof HepyProductCard c && c.lookupAll(".title-s").stream()
                .anyMatch(t -> t instanceof javafx.scene.control.Labeled l && name.equals(l.getText()))).tryQuery().orElse(null);
    }

    private static Node size(FxRobot robot, String name) {
        return robot.lookup((Predicate<Node>) n -> n instanceof HepyFilterChip c && name.equals(c.getText()) && !c.isDisabled())
                .tryQuery().orElse(null);
    }

    private long query(String sql) {
        return context.ppqhDatabase().hepyWithConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }
}
