package com.sportforce3d.app;

import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.application.Platform;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class PpqhGlobalExceptionHandler {

    private static final Logger ppqhLog = LoggerFactory.getLogger(PpqhGlobalExceptionHandler.class);

    private PpqhGlobalExceptionHandler() {
    }

    public static void ppqhInstall(Supplier<Node> ppqhAnyNodeInMainWindow) {
        Thread.setDefaultUncaughtExceptionHandler((ppqhThread, ppqhError) -> {
            ppqhLog.error("Error no controlado en {}", ppqhThread.getName(), ppqhError);
            Platform.runLater(() -> {
                Node ppqhNode = ppqhAnyNodeInMainWindow.get();
                if (ppqhNode != null) {
                    PpqhToast.ppqhError(ppqhNode, PpqhErrorMessages.ppqhOf(ppqhError));
                }
            });
        });
    }
}
