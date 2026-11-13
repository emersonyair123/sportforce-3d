package com.sportforce3d.app;

import com.sportforce3d.component.PpqhAppRoot;
import com.sportforce3d.component.PpqhLogo;
import com.sportforce3d.config.PpqhAppConfig;
import com.sportforce3d.db.HepyDatabase;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PpqhSportForceApp extends Application {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhSportForceApp.class);

    public static final String PPQH_ARG_RESET_DB = "--reset-db";
    public static final String PPQH_ARG_GALLERY = "--gallery";

    private PpqhAppContext ppqhContext;
    private Throwable ppqhStartupError;

    @Override
    public void init() {
        List<String> ppqhArgs = getParameters().getRaw();
        try {
            PpqhAppConfig ppqhConfig = PpqhAppConfig.ppqhLoad();
            if (ppqhArgs.contains(PPQH_ARG_RESET_DB)) {
                ppqhLog.warn("Reiniciando la base de datos {}", ppqhConfig.ppqhDatabaseFile());
                HepyDatabase.hepyDeleteFiles(ppqhConfig.ppqhDatabaseFile());
            }
            ppqhContext = new PpqhAppContext(ppqhConfig, PpqhUiSettings.ppqhForUser());
            ppqhContext.ppqhDatabase().hepyMigrate();
        } catch (Throwable ppqhE) {
            ppqhLog.error("No se pudo preparar la base de datos", ppqhE);
            ppqhStartupError = ppqhE;
        }
    }

    @Override
    public void start(Stage ppqhStage) {
        PpqhFonts.ppqhLoadAll();
        ppqhStage.getIcons().addAll(PpqhLogo.ppqhMonogram(256), PpqhLogo.ppqhMonogram(64), PpqhLogo.ppqhMonogram(32));

        if (ppqhStartupError != null) {
            ppqhShowStartupError(ppqhStartupError);
            Platform.exit();
            return;
        }

        PpqhNavigator ppqhNavigator = ppqhOpenMainWindow(ppqhStage, ppqhContext);
        ppqhStage.show();

        if (getParameters().getRaw().contains(PPQH_ARG_GALLERY)) {
            ppqhNavigator.ppqhOpenComponentGallery();
        }
    }

    public static PpqhNavigator ppqhOpenMainWindow(Stage ppqhStage, PpqhAppContext ppqhContext) {
        PpqhAppRoot ppqhRoot = new PpqhAppRoot();
        Scene ppqhScene = new Scene(ppqhRoot, 1280, 720);
        PpqhTheme.DARK.ppqhApply(ppqhScene);
        PpqhGlobalExceptionHandler.ppqhInstall(() -> ppqhRoot);

        ppqhStage.setScene(ppqhScene);
        ppqhStage.setMinWidth(1024);
        ppqhStage.setMinHeight(640);
        ppqhStage.setTitle(PpqhNavigator.PPQH_APP_TITLE);

        PpqhNavigator ppqhNavigator = new PpqhNavigator(ppqhContext, ppqhStage, ppqhRoot);
        ppqhNavigator.ppqhShowLogin();
        return ppqhNavigator;
    }

    @Override
    public void stop() {
        PpqhAsync.ppqhShutdown();
    }

    private void ppqhShowStartupError(Throwable ppqhError) {
        Alert ppqhAlert = new Alert(Alert.AlertType.ERROR);
        ppqhAlert.setTitle(PpqhNavigator.PPQH_APP_TITLE);
        ppqhAlert.setHeaderText("No se pudo abrir la base de datos");
        ppqhAlert.setContentText("""
                Revisa que la carpeta "data" se pueda escribir y que no haya otra copia de la aplicación abierta.
                Si el archivo está dañado, inicia la aplicación con --reset-db para recrearlo con datos de prueba.""");
        TextArea ppqhDetails = new TextArea(String.valueOf(ppqhError));
        ppqhDetails.setEditable(false);
        ppqhDetails.setWrapText(true);
        ppqhAlert.getDialogPane().setExpandableContent(ppqhDetails);
        ppqhAlert.showAndWait();
    }
}
