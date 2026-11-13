package com.sportforce3d.app;

import com.sportforce3d.component.AppRoot;
import com.sportforce3d.component.Motion;
import com.sportforce3d.component.Toast;
import com.sportforce3d.view.ComponentGallery;
import javafx.scene.Parent;
import javafx.stage.Stage;

public final class Navigator {

    public static final String APP_TITLE = "SPORTFORCE 3D";

    private final AppContext context;
    private final Stage stage;
    private final AppRoot root;

    public Navigator(AppContext context, Stage stage, AppRoot root) {
        this.context = context;
        this.stage = stage;
        this.root = root;
        context.setNavigator(this);
    }

    public void showLogin() {
        show("login", "Iniciar sesión");
    }

    public void showRegister() {
        show("register", "Crear cuenta");
    }

    public void showShell() {
        context.session().requireUser();
        show("shell", null);
    }

    public void logout() {
        context.authService().logout();
        showLogin();
        Toast.info(root, "Cerraste sesión.");
    }

    public void openComponentGallery() {
        ComponentGallery.open(stage);
    }

    public Stage stage() {
        return stage;
    }

    private void show(String view, String subtitle) {
        Parent page = ViewLoader.load(view, context).root();
        root.setContent(page);
        Motion.enter(page, Motion.MEDIUM, 0);
        stage.setTitle(subtitle == null ? APP_TITLE : APP_TITLE + " · " + subtitle);
    }
}
