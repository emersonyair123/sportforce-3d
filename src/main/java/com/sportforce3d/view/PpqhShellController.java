package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhRouter;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.app.PpqhViewLoader;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhMotion;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhSidebarNav;
import com.sportforce3d.viewmodel.PpqhShellViewModel;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.feather.Feather;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

public class PpqhShellController implements PpqhRouter {
    private static final Map<String, String> PPQH_SIDEBAR_PARENT = Map.of(
            PpqhRoutes.PPQH_PRODUCT, PpqhRoutes.PPQH_CATALOG,
            PpqhRoutes.PPQH_CHECKOUT, PpqhRoutes.PPQH_CART,
            PpqhRoutes.PPQH_ORDER, PpqhRoutes.PPQH_ORDERS,
            PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, PpqhRoutes.PPQH_ADMIN_PRODUCTS,
            PpqhRoutes.PPQH_ADMIN_ORDER, PpqhRoutes.PPQH_ADMIN_ORDERS,
            PpqhRoutes.PPQH_ADMIN_DESIGN, PpqhRoutes.PPQH_ADMIN_ORDERS);

    private static final Set<String> PPQH_IMPLEMENTED = Set.of(PpqhRoutes.PPQH_HOME, PpqhRoutes.PPQH_CATALOG, PpqhRoutes.PPQH_PRODUCT, PpqhRoutes.PPQH_CART,
            PpqhRoutes.PPQH_CHECKOUT, PpqhRoutes.PPQH_ORDERS, PpqhRoutes.PPQH_ORDER, PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PPQH_DESIGNS, PpqhRoutes.PPQH_ACCOUNT, PpqhRoutes.PPQH_ADMIN_USERS, PpqhRoutes.PPQH_ADMIN_PRODUCTS,
            PpqhRoutes.PPQH_ADMIN_PRODUCT_EDIT, PpqhRoutes.PPQH_ADMIN_INVENTORY, PpqhRoutes.PPQH_ADMIN_DASHBOARD, PpqhRoutes.PPQH_ADMIN_ORDERS, PpqhRoutes.PPQH_ADMIN_ORDER,
            PpqhRoutes.PPQH_ADMIN_PAYMENTS, PpqhRoutes.PPQH_ADMIN_DESIGN, PpqhRoutes.PPQH_ADMIN_REPORTS, PpqhRoutes.PPQH_ADMIN_SETTINGS);

    private record PpqhEntry(String ppqhRoute, Object ppqhParam) {
    }

    private final PpqhAppContext ppqhContext;
    private final PpqhShellViewModel ppqhViewModel;
    private final Deque<PpqhEntry> ppqhHistory = new ArrayDeque<>();
    private PpqhEntry ppqhCurrent;

    @FXML private HBox ppqhSearchBox;
    @FXML private TextField ppqhSearchField;
    @FXML private StackPane ppqhCartSlot;
    @FXML private MenuButton ppqhUserMenu;
    @FXML private PpqhSidebarNav ppqhSidebar;
    @FXML private StackPane ppqhContentArea;

    public PpqhShellController(PpqhAppContext ppqhContext) {
        this.ppqhContext = ppqhContext;
        this.ppqhViewModel = new PpqhShellViewModel(ppqhContext.ppqhSession());
    }

    @FXML
    private void initialize() {
        ppqhContext.ppqhSetRouter(this);
        ppqhBuildTopBar();
        ppqhBuildSidebar();
        ppqhSidebar.setOnNavigate(ppqhId -> ppqhGo(ppqhId, null));
        ppqhContext.ppqhRefreshCartCount();
        ppqhGo(PpqhRoutes.PPQH_HOME, null);
    }

    @Override
    public void ppqhGo(String ppqhRoute, Object ppqhParam) {
        if (ppqhCurrent != null) {
            ppqhHistory.push(ppqhCurrent);
            while (ppqhHistory.size() > 30) {
                ppqhHistory.removeLast();
            }
        }
        ppqhShow(new PpqhEntry(ppqhRoute, ppqhParam));
    }

    @Override
    public void ppqhBack() {
        ppqhShow(ppqhHistory.isEmpty() ? new PpqhEntry(PpqhRoutes.PPQH_HOME, null) : ppqhHistory.pop());
    }

    private void ppqhShow(PpqhEntry ppqhEntry) {
        ppqhCurrent = ppqhEntry;
        Node ppqhView;
        if (!PPQH_IMPLEMENTED.contains(ppqhEntry.ppqhRoute())) {
            ppqhView = new PpqhEmptyState(Feather.CLOCK, "Próximamente", "Esta sección llegará en una próxima etapa.");
        } else if (PpqhRoutes.PPQH_ADMIN_ONLY.contains(ppqhEntry.ppqhRoute()) && !ppqhViewModel.isAdmin()) {
            PpqhSfButton ppqhHome = new PpqhSfButton("Volver al inicio", PpqhSfButton.PpqhVariant.PRIMARY).ppqhWithIcon(Feather.HOME);
            ppqhHome.setOnAction(ppqhE -> ppqhGo(PpqhRoutes.PPQH_HOME, null));
            ppqhView = new PpqhEmptyState(Feather.LOCK, "Sin acceso", "Esta sección es solo para administradores. Si "
                    + "necesitas permisos, pídeselos a un administrador de la tienda.", ppqhHome);
        } else {
            PpqhViewLoader.PpqhLoaded<Object> ppqhLoaded = PpqhViewLoader.ppqhLoad(ppqhEntry.ppqhRoute(), ppqhContext);
            if (ppqhLoaded.ppqhController() instanceof PpqhAccountController ppqhAccount) {
                ppqhAccount.ppqhSetOnProfileSaved(ppqhViewModel::ppqhRefresh);
            }
            if (ppqhLoaded.ppqhController() instanceof PpqhRoutable ppqhRoutable) {
                ppqhRoutable.ppqhOnRoute(ppqhEntry.ppqhParam());
            }
            ppqhView = ppqhLoaded.ppqhRoot();
        }
        ppqhContentArea.getChildren().setAll(ppqhView);
        PpqhMotion.ppqhEnter(ppqhView, PpqhMotion.PPQH_MEDIUM, 8);

        ppqhSidebar.ppqhSelect(PPQH_SIDEBAR_PARENT.getOrDefault(ppqhEntry.ppqhRoute(), ppqhEntry.ppqhRoute()));
    }

    private void ppqhBuildTopBar() {
        ppqhSearchBox.getChildren().add(0, PpqhIcons.ppqhOf(Feather.SEARCH, PpqhIcons.PPQH_SM));
        ppqhSearchField.setPromptText("Buscar camisetas, uniformes, zapatillas…");
        PpqhInputs.ppqhLimit(ppqhSearchField, com.sportforce3d.service.HepyCatalogService.HEPY_SEARCH_MAX);
        ppqhSearchField.setAccessibleText("Buscar productos");
        ppqhSearchField.setOnKeyPressed(ppqhE -> {
            if (ppqhE.getCode() == KeyCode.ENTER) {
                String ppqhText = ppqhSearchField.getText() == null ? "" : ppqhSearchField.getText().trim();
                ppqhGo(PpqhRoutes.PPQH_CATALOG, new PpqhRoutes.PpqhCatalogParams(ppqhText.isEmpty() ? null : ppqhText, null));
            } else if (ppqhE.getCode() == KeyCode.ESCAPE) {
                ppqhSearchField.clear();
            }
        });

        PpqhSfButton ppqhCart = new PpqhSfButton("Carrito", PpqhSfButton.PpqhVariant.GHOST, PpqhSfButton.PpqhSize.SM);
        Label ppqhCount = new Label();
        ppqhCount.getStyleClass().add("sf-cart-count");
        ppqhCount.textProperty().bind(ppqhContext.cartCountProperty().asString());
        ppqhCount.visibleProperty().bind(ppqhContext.cartCountProperty().greaterThan(0));
        ppqhCount.managedProperty().bind(ppqhCount.visibleProperty());
        HBox ppqhGraphic = new HBox(6, PpqhIcons.ppqhOf(Feather.SHOPPING_CART, PpqhIcons.PPQH_SM), ppqhCount);
        ppqhGraphic.setAlignment(Pos.CENTER_LEFT);
        ppqhCart.setGraphic(ppqhGraphic);
        ppqhCart.accessibleTextProperty().bind(ppqhContext.cartCountProperty().asString("Carrito, %d productos"));
        ppqhCart.setOnAction(ppqhE -> ppqhGo(PpqhRoutes.PPQH_CART, null));
        ppqhCartSlot.getChildren().add(ppqhCart);

        Label ppqhInitials = new Label();
        ppqhInitials.textProperty().bind(ppqhViewModel.initialsProperty());
        StackPane ppqhAvatar = new StackPane(ppqhInitials);
        ppqhAvatar.getStyleClass().add("sf-avatar");
        ppqhUserMenu.setGraphic(ppqhAvatar);
        ppqhUserMenu.textProperty().bind(ppqhViewModel.displayNameProperty());
        ppqhUserMenu.setAccessibleText("Menú de la cuenta");

        MenuItem ppqhAccount = new MenuItem("Mi cuenta", PpqhIcons.ppqhOf(Feather.USER, PpqhIcons.PPQH_SM));
        ppqhAccount.setOnAction(ppqhE -> ppqhGo(PpqhRoutes.PPQH_ACCOUNT, null));
        MenuItem ppqhOrders = new MenuItem("Mis pedidos", PpqhIcons.ppqhOf(Feather.PACKAGE, PpqhIcons.PPQH_SM));
        ppqhOrders.setOnAction(ppqhE -> ppqhGo(PpqhRoutes.PPQH_ORDERS, null));
        MenuItem ppqhGallery = new MenuItem("Galería de componentes", PpqhIcons.ppqhOf(Feather.GRID, PpqhIcons.PPQH_SM));
        ppqhGallery.setOnAction(ppqhE -> ppqhContext.ppqhNavigator().ppqhOpenComponentGallery());
        MenuItem ppqhLogout = new MenuItem("Cerrar sesión", PpqhIcons.ppqhOf(Feather.LOG_OUT, PpqhIcons.PPQH_SM));
        ppqhLogout.setOnAction(ppqhE -> {
            ppqhContext.setCartCount(0);
            ppqhContext.ppqhNavigator().ppqhLogout();
        });
        ppqhUserMenu.getItems().setAll(ppqhAccount, ppqhOrders, ppqhGallery, new SeparatorMenuItem(), ppqhLogout);
    }

    private void ppqhBuildSidebar() {
        ppqhSidebar.collapsedProperty().bindBidirectional(ppqhContext.ppqhUiSettings().sidebarCollapsedProperty());

        ppqhSidebar.ppqhAddSection("Tienda");
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_HOME, "Inicio", Feather.HOME, true);
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_CATALOG, "Catálogo", Feather.GRID, true);
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_DESIGNER, "Diseñador 3D", Feather.BOX, true);
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_CART, "Carrito", Feather.SHOPPING_CART, true);
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ORDERS, "Mis pedidos", Feather.PACKAGE, true);
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_DESIGNS, "Mis diseños", Feather.LAYERS, true);
        ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ACCOUNT, "Mi cuenta", Feather.USER, true);

        if (ppqhViewModel.isAdmin()) {
            ppqhSidebar.ppqhAddSection("Administración");
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_DASHBOARD, "Dashboard", Feather.BAR_CHART_2, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_PRODUCTS, "Productos", Feather.TAG, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_INVENTORY, "Inventario", Feather.ARCHIVE, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_ORDERS, "Pedidos", Feather.CLIPBOARD, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_PAYMENTS, "Pagos", Feather.CREDIT_CARD, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_USERS, "Usuarios", Feather.USERS, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_REPORTS, "Reportes", Feather.FILE_TEXT, true);
            ppqhSidebar.ppqhAddItem(PpqhRoutes.PPQH_ADMIN_SETTINGS, "Configuración", Feather.SETTINGS, true);
        }
    }
}
