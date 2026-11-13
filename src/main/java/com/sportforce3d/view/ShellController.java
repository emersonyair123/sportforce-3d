package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Router;
import com.sportforce3d.app.Routes;
import com.sportforce3d.app.ViewLoader;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.Motion;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.SidebarNav;
import com.sportforce3d.viewmodel.ShellViewModel;
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

public class ShellController implements Router {
    private static final Map<String, String> SIDEBAR_PARENT = Map.of(
            Routes.PRODUCT, Routes.CATALOG,
            Routes.CHECKOUT, Routes.CART,
            Routes.ORDER, Routes.ORDERS,
            Routes.ADMIN_PRODUCT_EDIT, Routes.ADMIN_PRODUCTS,
            Routes.ADMIN_ORDER, Routes.ADMIN_ORDERS,
            Routes.ADMIN_DESIGN, Routes.ADMIN_ORDERS);

    private static final Set<String> IMPLEMENTED = Set.of(Routes.HOME, Routes.CATALOG, Routes.PRODUCT, Routes.CART,
            Routes.CHECKOUT, Routes.ORDERS, Routes.ORDER, Routes.DESIGNER, Routes.DESIGNS, Routes.ACCOUNT, Routes.ADMIN_USERS, Routes.ADMIN_PRODUCTS,
            Routes.ADMIN_PRODUCT_EDIT, Routes.ADMIN_INVENTORY, Routes.ADMIN_DASHBOARD, Routes.ADMIN_ORDERS, Routes.ADMIN_ORDER,
            Routes.ADMIN_PAYMENTS, Routes.ADMIN_DESIGN, Routes.ADMIN_REPORTS, Routes.ADMIN_SETTINGS);

    private record Entry(String route, Object param) {
    }

    private final AppContext context;
    private final ShellViewModel viewModel;
    private final Deque<Entry> history = new ArrayDeque<>();
    private Entry current;

    @FXML private HBox searchBox;
    @FXML private TextField searchField;
    @FXML private StackPane cartSlot;
    @FXML private MenuButton userMenu;
    @FXML private SidebarNav sidebar;
    @FXML private StackPane contentArea;

    public ShellController(AppContext context) {
        this.context = context;
        this.viewModel = new ShellViewModel(context.session());
    }

    @FXML
    private void initialize() {
        context.setRouter(this);
        buildTopBar();
        buildSidebar();
        sidebar.setOnNavigate(id -> go(id, null));
        context.refreshCartCount();
        go(Routes.HOME, null);
    }

    @Override
    public void go(String route, Object param) {
        if (current != null) {
            history.push(current);
            while (history.size() > 30) {
                history.removeLast();
            }
        }
        show(new Entry(route, param));
    }

    @Override
    public void back() {
        show(history.isEmpty() ? new Entry(Routes.HOME, null) : history.pop());
    }

    private void show(Entry entry) {
        current = entry;
        Node view;
        if (!IMPLEMENTED.contains(entry.route())) {
            view = new EmptyState(Feather.CLOCK, "Próximamente", "Esta sección llegará en una próxima etapa.");
        } else if (Routes.ADMIN_ONLY.contains(entry.route()) && !viewModel.isAdmin()) {
            SfButton home = new SfButton("Volver al inicio", SfButton.Variant.PRIMARY).withIcon(Feather.HOME);
            home.setOnAction(e -> go(Routes.HOME, null));
            view = new EmptyState(Feather.LOCK, "Sin acceso", "Esta sección es solo para administradores. Si "
                    + "necesitas permisos, pídeselos a un administrador de la tienda.", home);
        } else {
            ViewLoader.Loaded<Object> loaded = ViewLoader.load(entry.route(), context);
            if (loaded.controller() instanceof AccountController account) {
                account.setOnProfileSaved(viewModel::refresh);
            }
            if (loaded.controller() instanceof Routable routable) {
                routable.onRoute(entry.param());
            }
            view = loaded.root();
        }
        contentArea.getChildren().setAll(view);
        Motion.enter(view, Motion.MEDIUM, 8);

        sidebar.select(SIDEBAR_PARENT.getOrDefault(entry.route(), entry.route()));
    }

    private void buildTopBar() {
        searchBox.getChildren().add(0, Icons.of(Feather.SEARCH, Icons.SM));
        searchField.setPromptText("Buscar camisetas, uniformes, zapatillas…");
        Inputs.limit(searchField, com.sportforce3d.service.CatalogService.SEARCH_MAX);
        searchField.setAccessibleText("Buscar productos");
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String text = searchField.getText() == null ? "" : searchField.getText().trim();
                go(Routes.CATALOG, new Routes.CatalogParams(text.isEmpty() ? null : text, null));
            } else if (e.getCode() == KeyCode.ESCAPE) {
                searchField.clear();
            }
        });

        SfButton cart = new SfButton("Carrito", SfButton.Variant.GHOST, SfButton.Size.SM);
        Label count = new Label();
        count.getStyleClass().add("sf-cart-count");
        count.textProperty().bind(context.cartCountProperty().asString());
        count.visibleProperty().bind(context.cartCountProperty().greaterThan(0));
        count.managedProperty().bind(count.visibleProperty());
        HBox graphic = new HBox(6, Icons.of(Feather.SHOPPING_CART, Icons.SM), count);
        graphic.setAlignment(Pos.CENTER_LEFT);
        cart.setGraphic(graphic);
        cart.accessibleTextProperty().bind(context.cartCountProperty().asString("Carrito, %d productos"));
        cart.setOnAction(e -> go(Routes.CART, null));
        cartSlot.getChildren().add(cart);

        Label initials = new Label();
        initials.textProperty().bind(viewModel.initialsProperty());
        StackPane avatar = new StackPane(initials);
        avatar.getStyleClass().add("sf-avatar");
        userMenu.setGraphic(avatar);
        userMenu.textProperty().bind(viewModel.displayNameProperty());
        userMenu.setAccessibleText("Menú de la cuenta");

        MenuItem account = new MenuItem("Mi cuenta", Icons.of(Feather.USER, Icons.SM));
        account.setOnAction(e -> go(Routes.ACCOUNT, null));
        MenuItem orders = new MenuItem("Mis pedidos", Icons.of(Feather.PACKAGE, Icons.SM));
        orders.setOnAction(e -> go(Routes.ORDERS, null));
        MenuItem gallery = new MenuItem("Galería de componentes", Icons.of(Feather.GRID, Icons.SM));
        gallery.setOnAction(e -> context.navigator().openComponentGallery());
        MenuItem logout = new MenuItem("Cerrar sesión", Icons.of(Feather.LOG_OUT, Icons.SM));
        logout.setOnAction(e -> {
            context.setCartCount(0);
            context.navigator().logout();
        });
        userMenu.getItems().setAll(account, orders, gallery, new SeparatorMenuItem(), logout);
    }

    private void buildSidebar() {
        sidebar.collapsedProperty().bindBidirectional(context.uiSettings().sidebarCollapsedProperty());

        sidebar.addSection("Tienda");
        sidebar.addItem(Routes.HOME, "Inicio", Feather.HOME, true);
        sidebar.addItem(Routes.CATALOG, "Catálogo", Feather.GRID, true);
        sidebar.addItem(Routes.DESIGNER, "Diseñador 3D", Feather.BOX, true);
        sidebar.addItem(Routes.CART, "Carrito", Feather.SHOPPING_CART, true);
        sidebar.addItem(Routes.ORDERS, "Mis pedidos", Feather.PACKAGE, true);
        sidebar.addItem(Routes.DESIGNS, "Mis diseños", Feather.LAYERS, true);
        sidebar.addItem(Routes.ACCOUNT, "Mi cuenta", Feather.USER, true);

        if (viewModel.isAdmin()) {
            sidebar.addSection("Administración");
            sidebar.addItem(Routes.ADMIN_DASHBOARD, "Dashboard", Feather.BAR_CHART_2, true);
            sidebar.addItem(Routes.ADMIN_PRODUCTS, "Productos", Feather.TAG, true);
            sidebar.addItem(Routes.ADMIN_INVENTORY, "Inventario", Feather.ARCHIVE, true);
            sidebar.addItem(Routes.ADMIN_ORDERS, "Pedidos", Feather.CLIPBOARD, true);
            sidebar.addItem(Routes.ADMIN_PAYMENTS, "Pagos", Feather.CREDIT_CARD, true);
            sidebar.addItem(Routes.ADMIN_USERS, "Usuarios", Feather.USERS, true);
            sidebar.addItem(Routes.ADMIN_REPORTS, "Reportes", Feather.FILE_TEXT, true);
            sidebar.addItem(Routes.ADMIN_SETTINGS, "Configuración", Feather.SETTINGS, true);
        }
    }
}
