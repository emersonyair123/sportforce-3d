package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.SfBadge;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.viewmodel.AdminUsersViewModel;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class AdminUsersController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Lima"));

    private final AppContext context;
    private final AdminUsersViewModel viewModel;

    @FXML private SfTextField filterField;
    @FXML private SfButton refreshButton;
    @FXML private TableView<User> table;

    public AdminUsersController(AppContext context) {
        this.context = context;
        this.viewModel = new AdminUsersViewModel(context.userAdminService());
    }

    @FXML
    private void initialize() {
        filterField.textProperty().bindBidirectional(viewModel.filterProperty());
        refreshButton.withIcon(Feather.REFRESH_CW);
        refreshButton.disableProperty().bind(viewModel.loadingProperty());
        refreshButton.setOnAction(e -> viewModel.load());

        table.setItems(viewModel.filteredUsers());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new EmptyState(Feather.USERS, "Sin resultados", "No hay usuarios que coincidan con la búsqueda."));
        table.getColumns().setAll(java.util.List.of(
                column("USUARIO", 250, this::userCell),
                column("ROLES", 150, this::rolesCell),
                column("ESTADO", 110, this::statusCell),
                column("ÚLTIMO ACCESO", 170, this::lastLoginCell),
                column("ACCIONES", 280, this::actionsCell)));

        viewModel.errorProperty().addListener((obs, old, error) -> {
            if (error != null && !error.isEmpty()) {
                table.setPlaceholder(new EmptyState(Feather.LOCK, "No se pudo cargar la lista", error));
            }
        });
        viewModel.load();
    }

    private TableColumn<User, User> column(String title, double width, Function<User, javafx.scene.Node> renderer) {
        TableColumn<User, User> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        // Sin truncar: si no cabe, la tabla muestra desplazamiento horizontal.
        col.setMinWidth(width);
        col.setSortable(false);
        col.setReorderable(false);
        col.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(null);
                setGraphic(empty || user == null ? null : renderer.apply(user));
            }
        });
        return col;
    }

    private javafx.scene.Node userCell(User user) {
        Label name = new Label(user.fullName());
        name.getStyleClass().addAll("ink", "sf-strong");
        Label email = new Label(user.email());
        email.getStyleClass().addAll("code", "ink-muted");
        VBox box = new VBox(0, name, email);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private javafx.scene.Node rolesCell(User user) {
        HBox box = new HBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        for (Role role : Role.values()) {
            if (user.hasRole(role)) {
                box.getChildren().add(new SfBadge(role.label(), role == Role.ADMIN ? SfBadge.Kind.BRAND : SfBadge.Kind.NEUTRAL));
            }
        }
        return box;
    }

    private javafx.scene.Node statusCell(User user) {
        return user.active()
                ? new SfBadge("Activo", SfBadge.Kind.SUCCESS).withIcon(Feather.CHECK)
                : new SfBadge("Inactivo", SfBadge.Kind.DANGER).withIcon(Feather.SLASH);
    }

    private javafx.scene.Node lastLoginCell(User user) {
        Label label = new Label(user.lastLoginAt() == null ? "Nunca" : DATE.format(Instant.parse(user.lastLoginAt())));
        label.getStyleClass().addAll("code", "ink-muted");
        return label;
    }

    private javafx.scene.Node actionsCell(User user) {
        boolean isMe = context.session().current().map(u -> u.id() == user.id()).orElse(false);

        SfButton active = new SfButton(user.active() ? "Desactivar" : "Activar",
                user.active() ? SfButton.Variant.DANGER : SfButton.Variant.GHOST, SfButton.Size.SM);
        active.setDisable(isMe);
        active.setOnAction(e -> confirmActive(user));

        SfButton admin = new SfButton(user.isAdmin() ? "Quitar admin" : "Hacer admin", SfButton.Variant.GHOST, SfButton.Size.SM);
        admin.setDisable(isMe);
        admin.setOnAction(e -> confirmAdmin(user));

        active.setMinWidth(Region.USE_PREF_SIZE);
        admin.setMinWidth(Region.USE_PREF_SIZE);
        HBox box = new HBox(8, active, admin);
        box.setAlignment(Pos.CENTER_LEFT);
        if (isMe) {
            Label you = new Label("(tu cuenta)");
            you.getStyleClass().addAll("body-s", "ink-subtle");
            you.setMinWidth(Region.USE_PREF_SIZE);
            box.getChildren().add(you);
        }
        return box;
    }

    private void confirmActive(User user) {
        boolean activate = !user.active();
        ConfirmDialog.show(table,
                activate ? "¿Activar la cuenta?" : "¿Desactivar la cuenta?",
                activate
                        ? user.fullName() + " podrá volver a iniciar sesión."
                        : user.fullName() + " ya no podrá iniciar sesión. Sus pedidos y diseños se conservan.",
                activate ? "Activar" : "Desactivar",
                !activate,
                () -> viewModel.setActive(user, activate,
                        updated -> Toast.success(table, activate ? "Cuenta activada." : "Cuenta desactivada."),
                        message -> Toast.error(table, message)));
    }

    private void confirmAdmin(User user) {
        boolean grant = !user.isAdmin();
        ConfirmDialog.show(table,
                grant ? "¿Dar rol de administrador?" : "¿Quitar rol de administrador?",
                grant
                        ? user.fullName() + " podrá gestionar productos, pedidos, pagos y usuarios."
                        : user.fullName() + " dejará de ver el panel administrativo.",
                grant ? "Dar rol ADMIN" : "Quitar rol",
                !grant,
                () -> viewModel.setAdmin(user, grant,
                        updated -> Toast.success(table, "Roles actualizados."),
                        message -> Toast.error(table, message)));
    }
}
