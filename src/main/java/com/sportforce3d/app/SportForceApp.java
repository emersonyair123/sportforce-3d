package com.sportforce3d.app;

import com.sportforce3d.component.AppRoot;
import com.sportforce3d.component.Logo;
import com.sportforce3d.config.AppConfig;
import com.sportforce3d.db.Database;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Aplicación JavaFX. Argumentos:
 * <ul>
 *     <li>{@code --reset-db}: borra data/sportforce3d.db y lo recrea con los datos de prueba.</li>
 *     <li>{@code --gallery}: abre también la galería de componentes.</li>
 * </ul>
 */
public class SportForceApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(SportForceApp.class);

    public static final String ARG_RESET_DB = "--reset-db";
    public static final String ARG_GALLERY = "--gallery";

    private AppContext context;
    private Throwable startupError;

    @Override
    public void init() {
        List<String> args = getParameters().getRaw();
        try {
            AppConfig config = AppConfig.load();
            if (args.contains(ARG_RESET_DB)) {
                log.warn("Reiniciando la base de datos {}", config.databaseFile());
                Database.deleteFiles(config.databaseFile());
            }
            context = new AppContext(config, UiSettings.forUser());
            context.database().migrate();
        } catch (Throwable e) {
            log.error("No se pudo preparar la base de datos", e);
            startupError = e;
        }
    }

    @Override
    public void start(Stage stage) {
        Fonts.loadAll();
        stage.getIcons().addAll(Logo.monogram(256), Logo.monogram(64), Logo.monogram(32));

        if (startupError != null) {
            showStartupError(startupError);
            Platform.exit();
            return;
        }

        Navigator navigator = openMainWindow(stage, context);
        stage.show();

        if (getParameters().getRaw().contains(ARG_GALLERY)) {
            navigator.openComponentGallery();
        }
    }

    /**
     * Prepara la ventana principal con la pantalla de inicio de sesión (sin mostrarla). La usan {@link #start} y
     * las pruebas de interfaz, que le pasan un contexto con una base de datos temporal.
     */
    public static Navigator openMainWindow(Stage stage, AppContext context) {
        AppRoot root = new AppRoot();
        Scene scene = new Scene(root, 1280, 720);
        Theme.DARK.apply(scene);
        GlobalExceptionHandler.install(() -> root);

        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.setTitle(Navigator.APP_TITLE);

        Navigator navigator = new Navigator(context, stage, root);
        navigator.showLogin();
        return navigator;
    }

    @Override
    public void stop() {
        Async.shutdown();
    }

    private void showStartupError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(Navigator.APP_TITLE);
        alert.setHeaderText("No se pudo abrir la base de datos");
        alert.setContentText("""
                Revisa que la carpeta "data" se pueda escribir y que no haya otra copia de la aplicación abierta.
                Si el archivo está dañado, inicia la aplicación con --reset-db para recrearlo con datos de prueba.""");
        TextArea details = new TextArea(String.valueOf(error));
        details.setEditable(false);
        details.setWrapText(true);
        alert.getDialogPane().setExpandableContent(details);
        alert.showAndWait();
    }
}