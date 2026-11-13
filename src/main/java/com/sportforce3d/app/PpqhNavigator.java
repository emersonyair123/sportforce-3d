package com.sportforce3d.app;

import com.sportforce3d.component.PpqhAppRoot;
import com.sportforce3d.component.PpqhMotion;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.view.PpqhComponentGallery;
import javafx.scene.Parent;
import javafx.stage.Stage;

public final class PpqhNavigator {

    public static final String PPQH_APP_TITLE = "SPORTFORCE 3D";

    private final PpqhAppContext ppqhContext;
    private final Stage ppqhStage;
    private final PpqhAppRoot ppqhRoot;

    public PpqhNavigator(PpqhAppContext ppqhContext, Stage ppqhStage, PpqhAppRoot ppqhRoot) {
        this.ppqhContext = ppqhContext;
        this.ppqhStage = ppqhStage;
        this.ppqhRoot = ppqhRoot;
        ppqhContext.ppqhSetNavigator(this);
    }

    public void ppqhShowLogin() {
        ppqhShow("login", "Iniciar sesión");
    }

    public void ppqhShowRegister() {
        ppqhShow("register", "Crear cuenta");
    }

    public void ppqhShowShell() {
        ppqhContext.ppqhSession().ppqhRequireUser();
        ppqhShow("shell", null);
    }

    public void ppqhLogout() {
        ppqhContext.ppqhAuthService().ppqhLogout();
        ppqhShowLogin();
        PpqhToast.ppqhInfo(ppqhRoot, "Cerraste sesión.");
    }

    public void ppqhOpenComponentGallery() {
        PpqhComponentGallery.ppqhOpen(ppqhStage);
    }

    public Stage ppqhStage() {
        return ppqhStage;
    }

    private void ppqhShow(String ppqhView, String ppqhSubtitle) {
        Parent ppqhPage = PpqhViewLoader.ppqhLoad(ppqhView, ppqhContext).ppqhRoot();
        ppqhRoot.setContent(ppqhPage);
        PpqhMotion.ppqhEnter(ppqhPage, PpqhMotion.PPQH_MEDIUM, 0);
        ppqhStage.setTitle(ppqhSubtitle == null ? PPQH_APP_TITLE : PPQH_APP_TITLE + " · " + ppqhSubtitle);
    }
}
