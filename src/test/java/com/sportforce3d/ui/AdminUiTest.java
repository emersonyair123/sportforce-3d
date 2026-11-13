package com.sportforce3d.ui;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.SfTextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;

import static com.sportforce3d.ui.UiTestSupport.click;
import static com.sportforce3d.ui.UiTestSupport.labeled;
import static com.sportforce3d.ui.UiTestSupport.textShown;
import static com.sportforce3d.ui.UiTestSupport.type;
import static com.sportforce3d.ui.UiTestSupport.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ApplicationExtension.class)
class AdminUiTest {

    @TempDir
    Path tempDir;

    private AppContext context;

    @Start
    void start(Stage stage) {
        context = UiTestSupport.start(stage, tempDir);
    }

    private void login(FxRobot robot, String email, String password) {
        type(robot, "Correo", email);
        type(robot, "Contraseña", password);
        click(robot, "Iniciar sesión");
        waitUntil("la tienda", () -> labeled(robot, "Catálogo").isPresent());
    }

    @Test
    void adminEditsPaymentSettingsWithValidation(FxRobot robot) {
        login(robot, "admin@sportforce3d.pe", "Admin123!");
        click(robot, "Configuración");
        waitUntil("aviso de datos de prueba", () -> textShown(robot, t -> t.startsWith("Estos son los datos de prueba")));

        replace(robot, "Número de Yape", "812 345 678");
        replace(robot, "CCI (20 dígitos)", "123");
        click(robot, "Guardar datos de pago");
        waitUntil("errores por campo", () -> textShown(robot, "El CCI tiene exactamente 20 dígitos."::equals));
        assertThat(textShown(robot, t -> t.startsWith("Ingresa un celular de 9 dígitos"))).isTrue();

        replace(robot, "Número de Yape", "912345678");
        replace(robot, "Titular de Yape", "María Quispe");
        replace(robot, "CCI (20 dígitos)", "00219100123456780123");
        assertThat(textShown(robot, t -> t.contains("Yapea al 912345678 (María Quispe)."))).isTrue();
        click(robot, "Guardar datos de pago");
        waitUntil("guardado", () -> textShown(robot, "Guardado. Los clientes ya ven estos datos."::equals));

        assertThat(field(robot, "Número de Yape").getText()).isEqualTo("912 345 678");
        assertThat(field(robot, "CCI (20 dígitos)").getText()).isEqualTo("002-191-001234567801-23");
        assertThat(textShown(robot, t -> t.startsWith("Estos son los datos de prueba"))).isFalse();
    }

    @Test
    void adminGeneratesSalesReports(FxRobot robot) {
        login(robot, "admin@sportforce3d.pe", "Admin123!");
        click(robot, "Reportes");
        waitUntil("reporte de 30 días", () -> textShown(robot, "Sin ventas en este rango"::equals));
        assertThat(labeled(robot, "Exportar PDF")).isPresent();
        assertThat(labeled(robot, "Exportar Excel")).isPresent();

        click(robot, "Este año");
        waitUntil("rango del año", () -> textShown(robot, t -> t.startsWith("1 de enero de 2026")));

        robot.interact(() -> {
            lookupPicker(robot, "Fecha inicial del reporte").getEditor().setText("31/12/2026");
            lookupPicker(robot, "Fecha final del reporte").getEditor().setText("01/01/2026");
        });
        click(robot, "Generar reporte");
        waitUntil("error de rango", () -> textShown(robot, "La fecha inicial no puede ser posterior a la final."::equals));

        robot.interact(() -> lookupPicker(robot, "Fecha inicial del reporte").getEditor().setText("2026-01-01"));
        click(robot, "Generar reporte");
        waitUntil("error de formato", () -> textShown(robot, t -> t.startsWith("Escribe las fechas como dd/mm/aaaa")));
    }

    @Test
    void adminChangesAZoneColorOfTheRealisticPhoto(FxRobot robot) {
        login(robot, "admin@sportforce3d.pe", "Admin123!");
        long id = productId("SF-ZAP-003");
        robot.interact(() -> context.router().go(Routes.ADMIN_PRODUCT_EDIT, id));
        waitUntil("foto realista", () -> textShown(robot, "SUELA"::equals));
        assertThat(textShown(robot, "CAPELLADA"::equals)).isTrue();

        SfTextField hex = zoneHex(robot, "SUELA");
        assertThat(hex.getText()).isEqualTo("#F2F5F8");
        UiTestSupport.clickNode(robot, hex.getInput());
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE).write("#0B0E13").push(KeyCode.ENTER);
        click(robot, "Guardar producto");
        waitUntil("guardado", () -> textShown(robot, "Cambios guardados."::equals));

        var look = context.catalogService().realisticLook(id).orElseThrow();
        assertThat(look.zoneColors()).containsEntry("paneles", "#0b0e13").containsEntry("principal", "#1f5fff");
        assertThat(zoneHex(robot, "SUELA").getText()).as("el formulario recarga lo guardado").isEqualTo("#0B0E13");

        long casaca = productId("SF-BUZ-002");
        robot.interact(() -> context.router().go(Routes.ADMIN_PRODUCT_EDIT, casaca));
        waitUntil("aviso de no personalizable", () -> textShown(robot, t -> t.startsWith("Los buzos y accesorios no se personalizan")));
        javafx.scene.control.CheckBox check = robot.lookup((java.util.function.Predicate<javafx.scene.Node>) n ->
                n instanceof javafx.scene.control.CheckBox c && "Personalizable en el diseñador 3D".equals(c.getText()))
                .queryAs(javafx.scene.control.CheckBox.class);
        assertThat(check.isDisabled()).isTrue();
        assertThat(check.isSelected()).isFalse();
    }

    private long productId(String sku) {
        return context.database().withConnection(c -> {
            try (var ps = c.prepareStatement("SELECT id FROM products WHERE sku = ?")) {
                ps.setString(1, sku);
                try (var rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        });
    }

    private static SfTextField zoneHex(FxRobot robot, String zoneLabel) {
        javafx.scene.control.Label label = robot.lookup((java.util.function.Predicate<javafx.scene.Node>) n ->
                n instanceof javafx.scene.control.Label l && zoneLabel.equals(l.getText()))
                .queryAs(javafx.scene.control.Label.class);
        return (SfTextField) label.getParent().lookup(".sf-field");
    }

    @Test
    void clientDoesNotSeeNorOpenAdminSections(FxRobot robot) {
        login(robot, "cliente@sportforce3d.pe", "Cliente123!");
        assertThat(labeled(robot, "Reportes")).isEmpty();
        assertThat(labeled(robot, "Configuración")).isEmpty();
        assertThat(labeled(robot, "Dashboard")).isEmpty();

        for (String route : new String[]{Routes.ADMIN_SETTINGS, Routes.ADMIN_REPORTS, Routes.ADMIN_PAYMENTS}) {
            robot.interact(() -> context.router().go(route, null));
            waitUntil("sin acceso", () -> textShown(robot, "Sin acceso"::equals));
            assertThat(textShown(robot, "Guardar datos de pago"::equals)).isFalse();
        }
    }

    private static SfTextField field(FxRobot robot, String label) {
        waitUntil("campo " + label, () -> UiTestSupport.field(robot, label).isPresent());
        return UiTestSupport.field(robot, label).orElseThrow();
    }

    private static void replace(FxRobot robot, String label, String value) {
        SfTextField f = field(robot, label);
        UiTestSupport.clickNode(robot, f.getInput());
        robot.push(KeyCode.CONTROL, KeyCode.A).push(KeyCode.BACK_SPACE).write(value);
    }

    private static javafx.scene.control.DatePicker lookupPicker(FxRobot robot, String accessible) {
        return robot.lookup((java.util.function.Predicate<javafx.scene.Node>) n -> n instanceof javafx.scene.control.DatePicker
                && accessible.equals(n.getAccessibleText())).queryAs(javafx.scene.control.DatePicker.class);
    }
}
