package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhSfBadge;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSfTextField;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PpqhRole;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.viewmodel.PpqhAdminUsersViewModel;
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

public class PpqhAdminUsersController {

    private static final DateTimeFormatter PPQH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Lima"));

    private final PpqhAppContext ppqhContext;
    private final PpqhAdminUsersViewModel ppqhViewModel;

    @FXML private PpqhSfTextField ppqhFilterField;
    @FXML private PpqhSfButton ppqhRefreshButton;
    @FXML private TableView<PpqhUser> ppqhTable;

    public PpqhAdminUsersController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
        this.ppqhViewModel = new PpqhAdminUsersViewModel(ppqhContext.ppqhUserAdminService());
    }

    @FXML
    private void initialize() {
        ppqhFilterField.textProperty().bindBidirectional(ppqhViewModel.filterProperty());
        ppqhRefreshButton.ppqhWithIcon(Feather.REFRESH_CW);
        ppqhRefreshButton.disableProperty().bind(ppqhViewModel.loadingProperty());
        ppqhRefreshButton.setOnAction(ppqhE -> ppqhViewModel.ppqhLoad());

        ppqhTable.setItems(ppqhViewModel.ppqhFilteredUsers());
        ppqhTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        ppqhTable.setPlaceholder(new PpqhEmptyState(Feather.USERS, "Sin resultados", "No hay usuarios que coincidan con la búsqueda."));
        ppqhTable.getColumns().setAll(java.util.List.of(
                ppqhColumn("USUARIO", 250, this::ppqhUserCell),
                ppqhColumn("ROLES", 150, this::ppqhRolesCell),
                ppqhColumn("ESTADO", 110, this::ppqhStatusCell),
                ppqhColumn("ÚLTIMO ACCESO", 170, this::ppqhLastLoginCell),
                ppqhColumn("ACCIONES", 280, this::ppqhActionsCell)));

        ppqhViewModel.errorProperty().addListener((ppqhObs, ppqhOld, ppqhError) -> {
            if (ppqhError != null && !ppqhError.isEmpty()) {
                ppqhTable.setPlaceholder(new PpqhEmptyState(Feather.LOCK, "No se pudo cargar la lista", ppqhError));
            }
        });
        ppqhViewModel.ppqhLoad();
    }

    private TableColumn<PpqhUser, PpqhUser> ppqhColumn(String ppqhTitle, double ppqhWidth, Function<PpqhUser, javafx.scene.Node> ppqhRenderer) {
        TableColumn<PpqhUser, PpqhUser> ppqhCol = new TableColumn<>(ppqhTitle);
        ppqhCol.setPrefWidth(ppqhWidth);
        ppqhCol.setMinWidth(ppqhWidth);
        ppqhCol.setSortable(false);
        ppqhCol.setReorderable(false);
        ppqhCol.setCellValueFactory(ppqhData -> new ReadOnlyObjectWrapper<>(ppqhData.getValue()));
        ppqhCol.setCellFactory(ppqhC -> new TableCell<>() {
            @Override
            protected void updateItem(PpqhUser ppqhUser, boolean ppqhEmpty) {
                super.updateItem(ppqhUser, ppqhEmpty);
                setText(null);
                setGraphic(ppqhEmpty || ppqhUser == null ? null : ppqhRenderer.apply(ppqhUser));
            }
        });
        return ppqhCol;
    }

    private javafx.scene.Node ppqhUserCell(PpqhUser ppqhUser) {
        Label ppqhName = new Label(ppqhUser.ppqhFullName());
        ppqhName.getStyleClass().addAll("ink", "sf-strong");
        Label ppqhEmail = new Label(ppqhUser.ppqhEmail());
        ppqhEmail.getStyleClass().addAll("code", "ink-muted");
        VBox ppqhBox = new VBox(0, ppqhName, ppqhEmail);
        ppqhBox.setAlignment(Pos.CENTER_LEFT);
        return ppqhBox;
    }

    private javafx.scene.Node ppqhRolesCell(PpqhUser ppqhUser) {
        HBox ppqhBox = new HBox(4);
        ppqhBox.setAlignment(Pos.CENTER_LEFT);
        for (PpqhRole ppqhRole : PpqhRole.values()) {
            if (ppqhUser.ppqhHasRole(ppqhRole)) {
                ppqhBox.getChildren().add(new PpqhSfBadge(ppqhRole.ppqhLabel(), ppqhRole == PpqhRole.ADMIN ? PpqhSfBadge.PpqhKind.BRAND : PpqhSfBadge.PpqhKind.NEUTRAL));
            }
        }
        return ppqhBox;
    }

    private javafx.scene.Node ppqhStatusCell(PpqhUser ppqhUser) {
        return ppqhUser.ppqhActive()
                ? new PpqhSfBadge("Activo", PpqhSfBadge.PpqhKind.SUCCESS).ppqhWithIcon(Feather.CHECK)
                : new PpqhSfBadge("Inactivo", PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.SLASH);
    }

    private javafx.scene.Node ppqhLastLoginCell(PpqhUser ppqhUser) {
        Label ppqhLabel = new Label(ppqhUser.ppqhLastLoginAt() == null ? "Nunca" : PPQH_DATE.format(Instant.parse(ppqhUser.ppqhLastLoginAt())));
        ppqhLabel.getStyleClass().addAll("code", "ink-muted");
        return ppqhLabel;
    }

    private javafx.scene.Node ppqhActionsCell(PpqhUser ppqhUser) {
        boolean ppqhIsMe = ppqhContext.ppqhSession().ppqhCurrent().map(ppqhU -> ppqhU.ppqhId() == ppqhUser.ppqhId()).orElse(false);

        PpqhSfButton ppqhActive = new PpqhSfButton(ppqhUser.ppqhActive() ? "Desactivar" : "Activar",
                ppqhUser.ppqhActive() ? PpqhSfButton.PpqhVariant.DANGER : PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        ppqhActive.setDisable(ppqhIsMe);
        ppqhActive.setOnAction(ppqhE -> ppqhConfirmActive(ppqhUser));

        PpqhSfButton ppqhAdmin = new PpqhSfButton(ppqhUser.ppqhIsAdmin() ? "Quitar admin" : "Hacer admin", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        ppqhAdmin.setDisable(ppqhIsMe);
        ppqhAdmin.setOnAction(ppqhE -> ppqhConfirmAdmin(ppqhUser));

        ppqhActive.setMinWidth(Region.USE_PREF_SIZE);
        ppqhAdmin.setMinWidth(Region.USE_PREF_SIZE);
        HBox ppqhBox = new HBox(8, ppqhActive, ppqhAdmin);
        ppqhBox.setAlignment(Pos.CENTER_LEFT);
        if (ppqhIsMe) {
            Label ppqhYou = new Label("(tu cuenta)");
            ppqhYou.getStyleClass().addAll("body-s", "ink-subtle");
            ppqhYou.setMinWidth(Region.USE_PREF_SIZE);
            ppqhBox.getChildren().add(ppqhYou);
        }
        return ppqhBox;
    }

    private void ppqhConfirmActive(PpqhUser ppqhUser) {
        boolean ppqhActivate = !ppqhUser.ppqhActive();
        PpqhConfirmDialog.ppqhShow(ppqhTable,
                ppqhActivate ? "¿Activar la cuenta?" : "¿Desactivar la cuenta?",
                ppqhActivate
                        ? ppqhUser.ppqhFullName() + " podrá volver a iniciar sesión."
                        : ppqhUser.ppqhFullName() + " ya no podrá iniciar sesión. Sus pedidos y diseños se conservan.",
                ppqhActivate ? "Activar" : "Desactivar",
                !ppqhActivate,
                () -> ppqhViewModel.ppqhSetActive(ppqhUser, ppqhActivate,
                        ppqhUpdated -> PpqhToast.ppqhSuccess(ppqhTable, ppqhActivate ? "Cuenta activada." : "Cuenta desactivada."),
                        ppqhMessage -> PpqhToast.ppqhError(ppqhTable, ppqhMessage)));
    }

    private void ppqhConfirmAdmin(PpqhUser ppqhUser) {
        boolean ppqhGrant = !ppqhUser.ppqhIsAdmin();
        PpqhConfirmDialog.ppqhShow(ppqhTable,
                ppqhGrant ? "¿Dar rol de administrador?" : "¿Quitar rol de administrador?",
                ppqhGrant
                        ? ppqhUser.ppqhFullName() + " podrá gestionar productos, pedidos, pagos y usuarios."
                        : ppqhUser.ppqhFullName() + " dejará de ver el panel administrativo.",
                ppqhGrant ? "Dar rol ADMIN" : "Quitar rol",
                !ppqhGrant,
                () -> ppqhViewModel.ppqhSetAdmin(ppqhUser, ppqhGrant,
                        ppqhUpdated -> PpqhToast.ppqhSuccess(ppqhTable, "Roles actualizados."),
                        ppqhMessage -> PpqhToast.ppqhError(ppqhTable, ppqhMessage)));
    }
}
