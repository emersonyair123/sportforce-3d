package com.sportforce3d.tools;

import com.sportforce3d.app.PpqhRoutes;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public final class ManualSteps extends ManualTour {

    private final List<String> failures = new ArrayList<>();
    private String prefix = "";
    private int number;

    ManualSteps(Path out) {
        super(out);
    }

    private interface FxAction {
        void run() throws Exception;
    }

    private void section(String p) {
        prefix = p;
        number = 0;
    }

    private void step(String name, String caption, FxAction action, Callable<Node[]> marks, long wait) {
        number++;
        String id = String.format("%s%02d-%s", prefix, number, name);
        try {
            if (action != null) {
                fx(() -> {
                    action.run();
                    return null;
                });
            }
            pause(wait);
            Node[] m = marks == null ? new Node[0] : fx(marks);
            shot(id, caption, m);
        } catch (Throwable e) {
            failures.add(id + ": " + e);
            System.out.println("FALLO " + id + ": " + e);
            try {
                fx(() -> {
                    for (javafx.stage.Window w : List.copyOf(javafx.stage.Window.getWindows())) {
                        if (w instanceof javafx.stage.PopupWindow pw) {
                            pw.hide();
                        }
                    }
                    return null;
                });
            } catch (Exception ignored) {
                failures.add(id + ": no se pudieron cerrar las ventanas emergentes");
            }
        }
    }

    private void step(String name, String caption, FxAction action, Callable<Node[]> marks) {
        step(name, caption, action, marks, 1100);
    }

    private void quiet(FxAction action, long wait) {
        try {
            fx(() -> {
                action.run();
                return null;
            });
            pause(wait);
        } catch (Throwable e) {
            failures.add(prefix + " (acción): " + e);
            System.out.println("FALLO acción en " + prefix + ": " + e);
        }
    }

    private Node[] m(Node... nodes) {
        return nodes;
    }

    private TextInputControl input(String prompt) {
        return (TextInputControl) all(n -> n instanceof TextInputControl t && t.getPromptText() != null
                && norm(t.getPromptText()).equals(norm(prompt))).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay campo con " + prompt));
    }

    private Node accStarts(String prefix, int index) {
        List<Node> found = all(n -> n.getAccessibleText() != null && norm(n.getAccessibleText()).startsWith(norm(prefix)));
        if (found.size() <= index) {
            throw new IllegalStateException("No hay control que empiece con " + prefix);
        }
        return found.get(index);
    }

    private <T extends Node> T nth(Class<T> type, int index) {
        List<Node> found = all(type::isInstance);
        if (found.size() <= index) {
            throw new IllegalStateException("No hay " + type.getSimpleName() + " " + index);
        }
        return type.cast(found.get(index));
    }

    private Node toggle(String text) {
        return all(n -> n instanceof ToggleButton t && t.getText() != null && norm(t.getText()).equals(norm(text)))
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("No hay pestaña " + text));
    }

    private void reveal(Node n) {
        for (Node p = n.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp && sp.getContent() != null) {
                double contentH = sp.getContent().getBoundsInLocal().getHeight();
                double viewH = sp.getViewportBounds().getHeight();
                if (contentH > viewH) {
                    double y = sp.getContent().sceneToLocal(n.localToScene(0, 0)).getY();
                    sp.setVvalue(Math.max(0, Math.min(1, (y - 40) / (contentH - viewH))));
                }
                return;
            }
        }
    }

    private void top() {
        for (Node n : all(x -> x instanceof ScrollPane)) {
            ((ScrollPane) n).setVvalue(0);
        }
    }

    private Node sidebar(String text) {
        return clickable(all(n -> n instanceof javafx.scene.control.Labeled l && l.getText() != null
                && norm(l.getText()).equals(norm(text)) && isInSidebar(n)).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay opción de menú " + text)));
    }

    private boolean isIn(Node n, String styleClass) {
        for (Node p = n; p != null; p = p.getParent()) {
            if (p.getStyleClass().contains(styleClass)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInSidebar(Node n) {
        for (Node p = n; p != null; p = p.getParent()) {
            if (p.getStyleClass().contains("sf-sidebar")) {
                return true;
            }
        }
        return false;
    }

    private long sql(String query) throws Exception {
        return context.ppqhDatabase().hepyWithConnection(c -> {
            try (var st = c.createStatement(); var rs = st.executeQuery(query)) {
                return rs.next() ? rs.getLong(1) : -1L;
            }
        });
    }

    private Path logo;

    @Override
    protected void run() throws Exception {
        Path proof = Files.createTempFile("comprobante-yape", ".png");
        writeProof(proof);
        logo = Files.createTempFile("logo-club", ".png");
        writeLogo(logo);
        long multiColor = sql("SELECT v.product_id FROM product_variants v JOIN products p ON p.id = v.product_id "
                + "WHERE p.deleted_at IS NULL AND p.active = 1 GROUP BY v.product_id HAVING count(DISTINCT v.color_hex) > 1 LIMIT 1");
        long uniform = sql("SELECT id FROM products WHERE sku = 'SF-UFU-001'");
        long shirt = sql("SELECT id FROM products WHERE sku = 'SF-CAM-001'");

        cuenta();
        interfaz();
        inicio();
        catalogo();
        producto(multiColor, uniform);
        realista();
        vista3d();
        guardarYCarrito();
        misDisenos();
        carritoYPago(shirt, proof);
        miCuenta();
        administrador();
        estadosCliente();

        Files.write(out.resolve("fallos.txt"), failures);
        System.out.println("FALLOS: " + failures.size());
        failures.forEach(System.out::println);
    }

    private static final String NOMBRE = "Carlos Mamani";
    private static final String CORREO = "carlos.mamani@correo.pe";
    private static final String CLAVE = "Futbol2026";

    private void cuenta() throws Exception {
        section("a");
        step("login", "Pantalla de inicio de sesión", null, () -> m(field("Correo"), field("Contraseña"), text("Iniciar sesión")));
        step("enlace-registro", "Enlace “Regístrate” para crear una cuenta nueva", null, () -> m(text("Regístrate")));
        step("registro", "Formulario “Crea tu cuenta”", () -> click(text("Regístrate")), () -> m(text("Crea tu cuenta")));
        step("registro-errores", "Mensajes de error al pulsar “Crear cuenta” con el formulario vacío",
                () -> click(text("Crear cuenta")), () -> m(text("Crear cuenta")));
        step("registro-nombre", "Campo Nombre completo", () -> field("Nombre completo").setText(NOMBRE), () -> m(field("Nombre completo")));
        step("registro-correo", "Campo Correo", () -> field("Correo").setText(CORREO), () -> m(field("Correo")));
        step("registro-celular", "Campo Celular (opcional)", () -> field("Celular (opcional)").setText("951234567"),
                () -> m(field("Celular (opcional)")));
        step("registro-clave", "Campo Contraseña", () -> field("Contraseña").setText(CLAVE), () -> m(field("Contraseña")));
        step("registro-repite", "Campo Repite la contraseña", () -> field("Repite la contraseña").setText(CLAVE),
                () -> m(field("Repite la contraseña")));
        step("registro-listo", "Formulario completo antes de crear la cuenta", null, () -> m(text("Crear cuenta")));
        step("registro-creada", "Cuenta creada: la aplicación abre la tienda con la sesión iniciada",
                () -> click(text("Crear cuenta")), null, 3000);
        logout();
        step("login-correo", "Escribir el correo en la pantalla de inicio de sesión",
                () -> field("Correo").setText(CORREO), () -> m(field("Correo")));
        step("login-clave-mal", "Contraseña equivocada", () -> field("Contraseña").setText("Equivocada1"), () -> m(field("Contraseña")));
        step("login-error", "Mensaje de error por correo o contraseña incorrectos", () -> click(text("Iniciar sesión")),
                () -> m(css(".sf-alert-error")), 2500);
        step("login-clave", "Contraseña correcta", () -> field("Contraseña").setText(CLAVE), () -> m(field("Contraseña")));
        step("login-boton", "Botón “Iniciar sesión”", null, () -> m(text("Iniciar sesión")));
        quiet(() -> click(text("Iniciar sesión")), 3000);
    }

    private void interfaz() throws Exception {
        section("b");
        go(PpqhRoutes.PPQH_HOME, null);
        step("ventana", "Ventana principal después de iniciar sesión", null, null);
        step("barra-superior", "Barra superior: logotipo, buscador, carrito y menú del usuario", null, () -> m(css("#topBar")));
        step("buscador", "Buscador general de la barra superior", null, () -> m(css("#searchBox")));
        step("boton-carrito", "Botón Carrito con el número de productos", null, () -> m(css("#cartSlot")));
        step("menu-usuario", "Menú del usuario abierto", () -> ((MenuButton) css(".menu-button")).show(), () -> m(css(".menu-button")));
        quiet(() -> ((MenuButton) css(".menu-button")).hide(), 400);
        step("menu-lateral", "Menú lateral de la tienda", null, () -> m(css("#sidebar")));
        step("contraer-menu", "Menú lateral contraído (solo iconos)", () -> click(acc("Contraer menú")), () -> m(css("#sidebar")));
        quiet(() -> click(css("#sidebar .sf-btn-icon")), 800);
        step("zona-central", "Zona central: muestra la pantalla elegida", null, () -> m(css("#contentArea")));
    }

    private void inicio() throws Exception {
        section("c");
        go(PpqhRoutes.PPQH_HOME, null);
        step("inicio", "Pantalla Inicio", () -> click(sidebar("Inicio")), () -> m(sidebar("Inicio")));
        step("cta-disenar", "Botón “Diseña tu uniforme 3D”", null, () -> m(text("Diseña tu uniforme 3D")));
        step("cta-catalogo", "Botón “Ver catálogo”", null, () -> m(text("Ver catálogo")));
        step("categorias", "Accesos directos a las categorías", () -> reveal(css("#categoriesPane")), () -> m(css("#categoriesPane")));
        step("destacados", "Productos destacados", () -> reveal(css("#featuredPane")), () -> m(css("#featuredPane")));
        step("tarjeta", "Tarjeta de producto: categoría, nombre, precio y botón", () -> reveal(css("#featuredPane")),
                () -> m(css("#featuredPane .sf-product-card")));
    }

    private void catalogo() throws Exception {
        section("d");
        step("catalogo", "Pantalla Catálogo", () -> click(sidebar("Catálogo")), () -> m(sidebar("Catálogo")), 2000);
        step("filtros", "Panel de filtros", null, () -> m(css("#filters")));
        step("resultados", "Resultados, contador y orden", null, () -> m(css("#searchBox"), text("Ordenar por"), css("#sortBox")));
        String[] categorias = {"Camisetas deportivas", "Uniformes de fútbol", "Uniformes de vóley", "Uniformes de básquet",
                "Shorts", "Medias", "Zapatillas", "Buzos y conjuntos", "Ropa de entrenamiento", "Accesorios", "Niños"};
        for (String c : categorias) {
            step("cat-" + slug(c), "Categoría " + c, () -> click(text(c)), () -> m(text(c)), 1800);
        }
        step("cat-todas", "Categoría Todas", () -> click(text("Todas")), () -> m(text("Todas")), 1500);
        step("deporte", "Filtro por deporte: Fútbol", () -> click(text("Fútbol")), () -> m(text("Fútbol")), 1500);
        step("talla", "Filtro por talla: M", () -> click(text("M")), () -> m(text("M")), 1500);
        step("precio", "Filtro por precio entre S/ 50 y S/ 150", () -> {
            input("Mín.").setText("50");
            input("Máx.").setText("150");
            click(text("OK"));
        }, () -> m(input("Mín."), input("Máx."), text("OK")), 1500);
        step("con-stock", "Casilla “Solo con stock”", () -> click(text("Solo con stock")), () -> m(text("Solo con stock")), 1500);
        step("personalizables", "Casilla “Personalizables en 3D”", () -> click(text("Personalizables en 3D")),
                () -> m(text("Personalizables en 3D")), 1500);
        step("limpiar", "Botón “Limpiar” quita todos los filtros", () -> click(text("Limpiar")), () -> m(text("Limpiar")), 1500);
        step("ordenar-lista", "Lista “Ordenar por”", () -> ((ComboBox<?>) css("#sortBox")).show(), () -> m(css("#sortBox")));
        step("ordenar-precio", "Productos ordenados por precio", () -> {
            ComboBox<?> c = (ComboBox<?>) css("#sortBox");
            c.hide();
            c.getSelectionModel().select(Math.min(1, c.getItems().size() - 1));
        }, () -> m(css("#sortBox")), 1500);
        quiet(() -> ((ComboBox<?>) css("#sortBox")).getSelectionModel().select(0), 1200);
        step("buscar-catalogo", "Búsqueda dentro del catálogo: “camiseta”", () -> {
            TextInputControl t = input("Buscar en el catálogo");
            t.setText("camiseta");
            t.fireEvent(new javafx.event.ActionEvent());
        }, () -> m(css("#resultsScroll #searchBox")), 1800);
        step("buscar-barra", "Búsqueda desde la barra superior: “vóley”", () -> {
            TextInputControl t = input("Buscar camisetas, uniformes, zapatillas…");
            t.setText("vóley");
            t.fireEvent(new javafx.event.ActionEvent());
        }, () -> m(css("#topBar #searchBox")), 1800);
        quiet(() -> input("Buscar camisetas, uniformes, zapatillas…").setText(""), 300);
        step("sin-resultados", "Búsqueda sin resultados", () -> {
            TextInputControl t = input("Buscar en el catálogo");
            t.setText("zapatilla voladora");
            t.fireEvent(new javafx.event.ActionEvent());
        }, () -> m(css("#resultsScroll #searchBox")), 1800);
        quiet(() -> click(text("Limpiar")), 1500);
        step("paginas", "Navegación entre páginas de resultados", () -> {
            ScrollPane sp = (ScrollPane) css("#resultsScroll");
            sp.setVvalue(1);
        }, () -> m(css("#pagerSlot")), 1200);
        step("pagina-2", "Página 2 del catálogo", () -> {
            click(text("Siguiente"));
        }, () -> m(css("#pagerSlot")), 1800);
    }

    private void producto(long multiColor, long uniform) throws Exception {
        section("e");
        go(PpqhRoutes.PPQH_PRODUCT, multiColor > 0 ? multiColor : uniform);
        step("detalle", "Detalle del producto", null, null);
        step("colores", "Color del producto (muestra y código HEX)", null, () -> m(css(".sf-swatch")));
        go(PpqhRoutes.PPQH_PRODUCT, uniform);
        step("uniforme", "Detalle de un uniforme personalizable", null, () -> m(text("Personalizable 3D")));
        step("talla", "Elegir la talla M: se muestra el stock disponible", () -> click(text("M")), () -> m(text("M"), textEnding(" disponibles")));
        step("cantidad", "Aumentar la cantidad con el botón +", () -> click(acc("Aumentar cantidad")), () -> m(css(".sf-stepper")));
        step("cantidad-menos", "Disminuir la cantidad con el botón −", () -> click(acc("Disminuir cantidad")), () -> m(css(".sf-stepper")));
        step("telas", "Telas disponibles y costo de envío", null, () -> m(text("Telas y materiales disponibles")));
        step("agregar", "Botón “Agregar al carrito”: el contador del carrito sube", () -> click(text("Agregar al carrito")),
                () -> m(text("Agregar al carrito"), css("#cartSlot")), 1800);
        step("personalizar", "Botón “Personalizar en 3D”", null, () -> m(text("Personalizar en 3D")));
        step("volver", "Enlace “Volver”", null, () -> m(text("Volver")));
    }

    private Node textEnding(String suffix) {
        return all(n -> n instanceof javafx.scene.control.Labeled l && l.getText() != null && norm(l.getText()).endsWith(norm(suffix)))
                .stream().findFirst().orElseThrow(() -> new IllegalStateException("No hay texto que termine en " + suffix));
    }

    private void realista() throws Exception {
        section("f");
        try {
            context.ppqhDesignService().pgeyUploadLogo(logo, true);
        } catch (Exception e) {
            failures.add("subir logo: " + e);
        }
        quiet(() -> click(text("Personalizar en 3D")), 3500);
        step("disenador", "Diseñador abierto desde el producto, en la Vista realista", null, () -> m(toggle("Vista realista")));
        step("frente-espalda", "La foto muestra el frente y la espalda lado a lado", null, () -> m(text("Frente"), text("Espalda")));
        step("herramientas", "Botones sobre la foto", null, () -> m(css("#toolbar")));
        String[] personas = {"Mujer", "Niño", "Niña", "Hombre"};
        for (String p : personas) {
            step("persona-" + slug(p), "Categoría " + p, () -> click(text(p)), () -> m(text(p)), 1800);
        }
        String[] cortes = {"Polo", "Cuello redondo", "Manga larga", "Vóley", "Básquet", "Buzo", "Short largo", "Arquero",
                "Compresión", "Casaca", "Fútbol cuello V"};
        for (String c : cortes) {
            step("corte-" + slug(c), "Corte " + c, () -> click(text(c)), () -> m(text(c)), 1800);
        }
        String[][] zonas = {{"Camiseta", "Rojo"}, {"Short", "Blanco"}, {"Paneles", "Amarillo"}, {"Medias", "Rojo"}};
        for (String[] z : zonas) {
            step("zona-" + slug(z[0]), "Zona " + z[0] + ": elegir la zona", () -> {
                reveal(toggle(z[0]));
                click(toggle(z[0]));
            }, () -> m(toggle(z[0])));
            step("color-" + slug(z[0]), "Zona " + z[0] + " en color " + z[1], () -> click(accStarts(z[1], 0)),
                    () -> m(accStarts(z[1], 0)), 1500);
        }
        step("hex", "Campo HEX: escribir un color exacto (#FF7A00)", () -> {
            click(toggle("Camiseta"));
            TextInputControl t = (TextInputControl) field("HEX").lookup(".text-field");
            t.setText("#FF7A00");
            t.fireEvent(new javafx.event.ActionEvent());
        }, () -> m(field("HEX")), 1500);
        quiet(() -> click(accStarts("Rojo", 0)), 1000);
        String[] disenos = {"Rayas", "Franjas horizontales", "Dinámico", "Geométrico", "Minimal (hombros)", "Banda diagonal", "Liso"};
        for (String d : disenos) {
            step("diseno-" + slug(d), "Diseño " + d, () -> {
                reveal(text(d));
                click(text(d));
            }, () -> m(text(d)), 1800);
        }
        quiet(() -> click(text("Rayas")), 1500);
        step("nombre", "Nombre en la espalda", () -> {
            reveal(field("Nombre (espalda)"));
            field("Nombre (espalda)").setText("MAMANI");
        }, () -> m(field("Nombre (espalda)")), 1800);
        step("numero", "Número en el pecho y la espalda", () -> field("Número (0–99)").setText("9"),
                () -> m(field("Número (0–99)")), 1800);
        step("resultado", "Resultado en la Vista realista: frente y espalda", () -> top(), () -> m(css("#stage")), 1200);
    }

    private void vista3d() throws Exception {
        section("g");
        step("pestana", "Pestaña Vista 3D", () -> click(toggle("Vista 3D")), () -> m(toggle("Vista 3D")), 7000);
        Node viewer = fx(() -> css(".sf-viewer3d"));
        step("girar", "Girar la persona: arrastrar con el ratón", null, () -> m(viewer));
        drag(viewer, 260, 0, 12);
        step("girada", "Persona girada después de arrastrar", null, () -> m(viewer), 1500);
        step("frente", "Botón Frente", () -> click(text("Frente")), () -> m(text("Frente")), 1600);
        step("espalda", "Botón Espalda", () -> click(text("Espalda")), () -> m(text("Espalda")), 1600);
        step("costado", "Botón Costado", () -> click(text("Costado")), () -> m(text("Costado")), 1600);
        step("acercar", "Botón Acercar (lupa con +)", () -> {
            click(acc("Acercar"));
            click(acc("Acercar"));
        }, () -> m(acc("Acercar")), 1600);
        step("alejar", "Botón Alejar (lupa con −)", () -> click(acc("Alejar")), () -> m(acc("Alejar")), 1600);
        scroll(viewer, 240);
        step("rueda", "Acercar con la rueda del ratón", null, () -> m(viewer), 1500);
        step("reiniciar", "Botón Reiniciar: vuelve al cuerpo completo", () -> click(text("Reiniciar")), () -> m(text("Reiniciar")), 1600);
        step("captura", "Botón Guardar captura (PNG)", null, () -> m(acc("Guardar captura (PNG)")));
        step("tab-diseno", "Pestañas Diseño, Texto, Logos y Capas", null, () -> m(toggle("Diseño").getParent()));
        step("prenda", "Selector de prenda", null, () -> m(css(".sf-designer-panel .sf-seg")));
        for (String p : new String[]{"Short", "Medias", "Zapatillas", "Camiseta"}) {
            step("prenda-" + slug(p), "Prenda " + p + ": la cámara la enfoca", () -> click(toggle(p)), () -> m(toggle(p)), 2200);
        }
        quiet(() -> click(text("Reiniciar")), 1500);
        for (String p : new String[]{"Mujer", "Niño", "Niña", "Hombre"}) {
            step("persona-" + slug(p), "Persona " + p + " en la Vista 3D", () -> click(text(p)), () -> m(text(p)), 6000);
        }
        step("manga-larga", "Manga larga", () -> {
            reveal(text("Larga"));
            click(text("Larga"));
        }, () -> m(text("Larga")), 4000);
        step("manga-corta", "Manga corta", () -> click(text("Corta")), () -> m(text("Corta")), 4000);
        for (String s : new String[]{"Corto", "Largo", "Medio"}) {
            step("short-" + slug(s), "Short " + s.toLowerCase(), () -> click(text(s)), () -> m(text(s)), 4000);
        }
        for (String s : new String[]{"Tobillera", "Media pierna"}) {
            step("medias-" + slug(s), "Medias " + s.toLowerCase(), () -> click(text(s)), () -> m(text(s)), 4000);
        }
        String[][] zonas = {{"Cuerpo", "Azul eléctrico"}, {"Mangas", "Blanco"}, {"Cuello", "Negro"}, {"Puños", "Negro"},
                {"Paneles laterales", "Amarillo"}};
        for (String[] z : zonas) {
            step("zona-" + slug(z[0]), "Zona " + z[0] + " en " + z[1], () -> {
                reveal(toggle(z[0]));
                click(toggle(z[0]));
                click(accStarts(z[1], 0));
            }, () -> m(toggle(z[0]), accStarts(z[1], 0)), 1800);
        }
        step("hex", "Campo HEX en la Vista 3D", () -> {
            click(toggle("Cuerpo"));
            TextInputControl t = (TextInputControl) field("HEX").lookup(".text-field");
            reveal(t);
            t.setText("#0E7C3A");
            t.fireEvent(new javafx.event.ActionEvent());
        }, () -> m(field("HEX")), 1800);
        String[] patrones = {"Franjas horizontales", "Franjas verticales", "Diagonales", "Degradado", "Cuadros",
                "Sublimado geométrico", "Liso"};
        for (String p : patrones) {
            step("patron-" + slug(p), "Patrón " + p, () -> {
                reveal(text(p));
                click(text(p));
            }, () -> m(text(p)), 1800);
        }
        quiet(() -> click(text("Franjas verticales")), 1500);
        for (String t : new String[]{"Poliéster liso", "Algodón peinado", "Malla deportiva", "Dry-Fit"}) {
            step("tela-" + slug(t), "Tela " + t, () -> {
                reveal(text(t));
                click(text(t));
            }, () -> m(text(t).getParent()), 1800);
        }
        step("restablecer", "Botón “Restablecer diseño”", () -> reveal(text("Restablecer diseño")), () -> m(text("Restablecer diseño")));
        step("deshacer", "Botón Deshacer (Ctrl+Z): vuelve al cambio anterior", () -> click(acc("Deshacer (Ctrl+Z)")),
                () -> m(acc("Deshacer (Ctrl+Z)")), 1800);
        step("rehacer", "Botón Rehacer (Ctrl+Y)", () -> click(acc("Rehacer (Ctrl+Y)")), () -> m(acc("Rehacer (Ctrl+Y)")), 1800);

        step("texto", "Pestaña Texto", () -> click(toggle("Texto")), () -> m(toggle("Texto")));
        step("texto-nombre", "Nombre en la espalda: abre su editor", () -> {
            click(text("Espalda"));
            click(text("Nombre en la espalda"));
        }, () -> m(text("Nombre en la espalda")), 2000);
        step("texto-escribir", "Escribir el nombre (máximo 12 letras)", () -> field("Nombre (máx. 12)").setText("MAMANI"),
                () -> m(field("Nombre (máx. 12)")), 2000);
        step("texto-tipografia", "Lista de tipografías", () -> {
            ComboBox<?> c = nth(ComboBox.class, 0);
            reveal(c);
            c.show();
        }, () -> m(nth(ComboBox.class, 0)));
        quiet(() -> {
            ComboBox<?> c = nth(ComboBox.class, 0);
            c.hide();
            c.getSelectionModel().select(Math.min(2, c.getItems().size() - 1));
        }, 1500);
        step("texto-color", "Color del texto", () -> {
            Node g = nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 0);
            reveal(g);
            click(accStarts("Amarillo", 0));
        }, () -> m(nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 0)), 1800);
        step("texto-contorno", "Color y grosor del contorno", () -> {
            Node g = nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 1);
            reveal(g);
            click(accStarts("Negro", 1));
            nth(Slider.class, 0).setValue(nth(Slider.class, 0).getMax() * 0.7);
        }, () -> m(nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 1), nth(Slider.class, 0)), 1800);
        step("texto-tamano", "Tamaño y giro del texto", () -> {
            Slider s = nth(Slider.class, 1);
            reveal(s);
            s.setValue(s.getMin() + (s.getMax() - s.getMin()) * 0.65);
        }, () -> m(nth(Slider.class, 1), nth(Slider.class, 2)), 1800);
        step("texto-numero-espalda", "Número en la espalda", () -> {
            reveal(text("Número en la espalda"));
            click(text("Número en la espalda"));
        }, () -> m(text("Número en la espalda")), 2000);
        step("texto-numero-pecho", "Número en el pecho", () -> {
            click(text("Frente"));
            reveal(text("Número en el pecho"));
            click(text("Número en el pecho"));
        }, () -> m(text("Número en el pecho")), 2000);
        step("texto-numero-short", "Número en el short", () -> {
            reveal(text("Número en el short"));
            click(text("Número en el short"));
        }, () -> m(text("Número en el short")), 2000);

        step("logos", "Pestaña Logos: emblemas de la tienda", () -> click(toggle("Logos")), () -> m(toggle("Logos")));
        step("logo-escudo", "Emblema Escudo clásico agregado en el pecho", () -> click(acc("Escudo clásico")),
                () -> m(acc("Escudo clásico")), 2000);
        step("logo-ajustes", "Ajustes del emblema: zona, colores, tamaño y giro", () -> reveal(text("Zona")),
                () -> m(text("Zona"), text("Color principal"), text("Color de acento")), 1200);
        step("logo-declaro", "Declaración de derechos antes de subir un logo propio", () -> {
            reveal(textStarting("Declaro"));
            click(textStarting("Declaro"));
        }, () -> m(textStarting("Declaro"), text("Subir logo (PNG o SVG)")));
        dump("g-logos");
        step("logo-propio", "Logo propio ya subido: aparece en “Tu logo”", () -> reveal(text("Tu logo")),
                () -> m(text("Tu logo")), 1200);

        step("capas", "Pestaña Capas: lista de elementos", () -> click(toggle("Capas")), () -> m(toggle("Capas")));
        Node[] hide = fx(() -> new Node[]{acc("Ocultar")});
        step("capa-ocultar", "Botón Ocultar de una capa: el elemento deja de verse", () -> click(hide[0]), () -> m(hide[0]), 1500);
        quiet(() -> click(hide[0]), 1200);
        Node[] lock = fx(() -> new Node[]{acc("Bloquear")});
        step("capa-bloquear", "Botón Bloquear de una capa: ya no se puede mover", () -> click(lock[0]), () -> m(lock[0]), 1500);
        quiet(() -> click(lock[0]), 1200);
        step("capa-orden", "Botones Subir capa y Bajar capa", () -> click(acc("Bajar capa")), () -> m(acc("Subir capa"), acc("Bajar capa")), 1500);
        step("capa-inspector", "Inspector del elemento elegido", null, () -> m(text("Zona")));
    }

    private void guardarYCarrito() throws Exception {
        section("h");
        step("guardar", "Botón “Guardar diseño”: abre el cuadro para ponerle nombre", () -> click(text("Guardar diseño")),
                () -> m(field("Nombre del diseño")), 1200);
        step("guardar-nombre", "Nombre del diseño", () -> field("Nombre del diseño").setText("Club Deportivo Mamani"),
                () -> m(field("Nombre del diseño"), text("Guardar")));
        step("guardado", "Diseño guardado: el subtítulo muestra su nombre y versión", () -> click(text("Guardar")),
                () -> m(css("#root")), 4000);
        step("agregar", "Botón “Agregar al carrito” del diseñador: abre el cuadro de talla y cantidad",
                () -> click(text("Agregar al carrito")), () -> m(text("Agregar al carrito", 1)), 2500);
        step("agregar-talla", "Elegir la talla y la cantidad del uniforme diseñado", () -> {
            click(text("M"));
            click(acc("Aumentar cantidad"));
        }, () -> m(text("M"), css(".sf-stepper")), 1500);
        step("agregar-listo", "Uniforme diseñado agregado al carrito", () -> click(text("Agregar al carrito", 1)),
                () -> m(css("#cartSlot")), 2500);
    }

    private void misDisenos() throws Exception {
        section("i");
        step("lista", "Pantalla Mis diseños", () -> click(sidebar("Mis diseños")), () -> m(sidebar("Mis diseños")), 2000);
        step("tarjeta", "Tarjeta del diseño con sus botones", null, () -> m(text("Abrir"), acc("Duplicar"), acc("Eliminar")));
        step("duplicar", "Botón Duplicar: crea una copia", () -> click(acc("Duplicar")), () -> m(acc("Duplicar")), 2500);
        step("eliminar", "Botón Eliminar de la copia: pide confirmación", () -> click(copyDelete()), null, 1200);
        step("eliminado", "Copia eliminada", () -> click(text("Eliminar", 0)), null, 2000);
        step("abrir", "Botón Abrir: vuelve al diseñador con el diseño", () -> click(text("Abrir")), null, 5000);
        step("nuevo", "Botón “Nuevo diseño”", () -> click(sidebar("Mis diseños")), () -> m(text("Nuevo diseño")), 2000);
    }

    private Node copyDelete() {
        for (Node open : all(n -> n instanceof javafx.scene.control.ButtonBase b && "Abrir".equals(b.getText()))) {
            for (Node card = open.getParent(); card != null; card = card.getParent()) {
                List<Node> labels = new ArrayList<>();
                collectLabels(card, labels);
                List<Node> deletes = new ArrayList<>();
                collectAcc(card, "Eliminar", deletes);
                if (!deletes.isEmpty()) {
                    boolean original = labels.stream().anyMatch(l -> "Club Deportivo Mamani".equals(((javafx.scene.control.Labeled) l).getText()));
                    if (!original) {
                        return deletes.get(0);
                    }
                    break;
                }
            }
        }
        throw new IllegalStateException("No hay copia para eliminar");
    }

    private static void collectLabels(Node n, List<Node> out) {
        if (n instanceof javafx.scene.control.Labeled) {
            out.add(n);
        }
        if (n instanceof javafx.scene.Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> collectLabels(c, out));
        }
    }

    private static void collectAcc(Node n, String text, List<Node> out) {
        if (text.equals(n.getAccessibleText())) {
            out.add(n);
        }
        if (n instanceof javafx.scene.Parent p) {
            p.getChildrenUnmodifiable().forEach(c -> collectAcc(c, text, out));
        }
    }

    private void carritoYPago(long shirt, Path proof) throws Exception {
        section("j");
        go(PpqhRoutes.PPQH_PRODUCT, shirt);
        quiet(() -> {
            click(text("L"));
            click(text("Agregar al carrito"));
        }, 1800);
        step("carrito", "Pantalla Carrito", () -> click(css("#cartSlot .sf-btn")), () -> m(css("#cartSlot")), 2000);
        step("linea", "Línea del carrito: producto, talla, color, precio y cantidad", null,
                () -> m(all(n -> n.getStyleClass().contains("sf-cart-line")).stream().findFirst().orElse(css(".sf-stepper"))));
        step("aumentar", "Aumentar la cantidad", () -> click(acc("Aumentar cantidad", 0)), () -> m(nth(com.sportforce3d.component.HepyQuantityStepper.class, 0)), 1800);
        step("disminuir", "Disminuir la cantidad", () -> click(acc("Disminuir cantidad", 0)), () -> m(nth(com.sportforce3d.component.HepyQuantityStepper.class, 0)), 1800);
        step("quitar", "Botón Quitar (papelera) de una línea", null, () -> m(accStarts("Quitar", 0)));
        step("quitado", "Línea quitada del carrito", () -> click(accStarts("Quitar Camiseta", 0)), null, 2000);
        step("resumen", "Resumen: subtotal, recargos, envío y total", null, () -> m(text("Resumen")));
        step("continuar", "Botón “Continuar al pago”", null, () -> m(text("Continuar al pago"), text("Seguir comprando")));

        section("k");
        step("checkout", "Pantalla Finaliza tu compra", () -> click(text("Continuar al pago")), null, 2000);
        step("agregar-direccion", "Botón “Agregar dirección”", null, () -> m(text("Agregar dirección")));
        step("direccion-form", "Formulario de nueva dirección", () -> click(text("Agregar dirección")), () -> m(text("Nueva dirección")), 1500);
        step("direccion-llena", "Dirección completa", () -> {
            field("Nombre de la dirección").setText("Casa");
            field("Quién recibe").setText(NOMBRE);
            field("Celular").setText("951234567");
            @SuppressWarnings("unchecked")
            ComboBox<Object> dep = (ComboBox<Object>) all(n -> n instanceof ComboBox).get(0);
            for (Object o : dep.getItems()) {
                if (String.valueOf(o).equals("Puno")) {
                    dep.getSelectionModel().select(o);
                }
            }
            field("Provincia").setText("San Román");
            field("Distrito").setText("Juliaca");
            ((CheckBox) text("Usar como dirección predeterminada")).setSelected(true);
            input("Jr. Mariano Núñez 245, dpto. 3").setText("Av. Floral 512");
            field("Referencia (opcional)").setText("Frente al estadio");
        }, () -> m(text("Guardar dirección")), 1500);
        step("direccion-guardada", "Dirección guardada y elegida", () -> click(text("Guardar dirección")), null, 2000);
        step("metodos", "Métodos de pago", null, () -> m(text("2. Método de pago")));
        step("yape", "Método Yape elegido", () -> click(text("Yape")), () -> m(text("Yape").getParent()));
        step("tarjeta", "Tarjeta de crédito o débito: “Próximamente” (deshabilitada)", null,
                () -> m(text("Tarjeta de crédito o débito").getParent()));
        step("notas", "Notas para la tienda (opcional)", () -> {
            TextInputControl t = nth(javafx.scene.control.TextArea.class, 0);
            reveal(t);
            t.setText("Entregar por la tarde, por favor.");
        }, () -> m(nth(javafx.scene.control.TextArea.class, 0)));
        step("tu-pedido", "Resumen “Tu pedido” y botón “Confirmar pedido”", null, () -> m(text("Tu pedido"), text("Confirmar pedido")));
        step("pedido-creado", "Pedido creado: pendiente de pago con instrucciones de Yape", () -> click(text("Confirmar pedido")), null, 3000);
        step("instrucciones", "Instrucciones de pago con Yape", () -> reveal(textStarting("Monto a pagar")), () -> m(textStarting("Monto a pagar"), textStarting("Yapea")));
        step("codigo", "Código de operación de Yape", () -> field("Código de operación").setText("48213077"), () -> m(field("Código de operación")));
        step("elegir", "Botón “Elegir comprobante”", null, () -> m(text("Elegir comprobante")));
        step("elegido", "Comprobante elegido", () -> {
            @SuppressWarnings("unchecked")
            var hook = (java.util.function.Consumer<java.io.File>) css("#send-payment").getProperties().get("proof");
            hook.accept(proof.toFile());
        }, () -> m(text("Elegir comprobante").getParent()));
        step("enviar", "Comprobante enviado: pago en verificación", () -> click(text("Enviar comprobante")), null, 3000);
        step("estado", "Estado “Pago en verificación” en la línea de tiempo", () -> top(), () -> m(css(".sf-timeline")));

        section("l");
        compraRapida(shirt, "Plin");
        step("plin", "Pedido con Plin: instrucciones y número de Plin", () -> reveal(textStarting("Monto a pagar")),
                () -> m(textStarting("Monto a pagar"), textStarting("Paga con Plin")));
        step("plin-codigo", "Código de operación de Plin", () -> field("Código de operación").setText("55129034"), () -> m(field("Código de operación")));
        step("plin-enviado", "Comprobante de Plin enviado", () -> {
            @SuppressWarnings("unchecked")
            var hook = (java.util.function.Consumer<java.io.File>) css("#send-payment").getProperties().get("proof");
            hook.accept(proof.toFile());
            click(text("Enviar comprobante"));
        }, null, 3000);
        compraRapida(shirt, "Transferencia bancaria");
        step("transferencia", "Pedido con transferencia: banco, cuenta y CCI", () -> reveal(textStarting("Monto a pagar")),
                () -> m(textStarting("Monto a pagar"), textStarting("Transfiere")));
        step("transferencia-codigo", "Número de operación de la transferencia", () -> field("Código de operación").setText("OP-778812"),
                () -> m(field("Código de operación")));
        cancelarPedido();

        section("m");
        step("mis-pedidos", "Pantalla Mis pedidos", () -> click(sidebar("Mis pedidos")), () -> m(sidebar("Mis pedidos")), 2000);
        step("pedido-fila", "Cada pedido: número, fecha, productos, método, estado y total", null,
                () -> m(textStarting("PED-2026").getParent()));
    }

    private void compraRapida(long product, String method) throws Exception {
        go(PpqhRoutes.PPQH_PRODUCT, product);
        quiet(() -> {
            click(text("M"));
            click(text("Agregar al carrito"));
        }, 1800);
        go(PpqhRoutes.PPQH_CHECKOUT, null);
        quiet(() -> click(text(method)), 800);
        quiet(() -> click(text("Confirmar pedido")), 3000);
    }

    private void miCuenta() throws Exception {
        section("n");
        step("cuenta", "Pantalla Mi cuenta", () -> click(sidebar("Mi cuenta")), () -> m(sidebar("Mi cuenta")), 1800);
        step("perfil", "Perfil: nombre completo y celular", () -> field("Nombre completo").setText("Carlos Mamani Quispe"),
                () -> m(field("Nombre completo"), field("Celular"), text("Guardar cambios")));
        step("perfil-guardado", "Perfil guardado", () -> click(text("Guardar cambios")), () -> m(text("Guardar cambios")), 2000);
        step("clave-actual", "Contraseña actual", () -> field("Contraseña actual").setText(CLAVE), () -> m(field("Contraseña actual")));
        step("clave-nueva", "Nueva contraseña y su repetición", () -> {
            field("Nueva contraseña").setText("Futbol2027");
            field("Repite la nueva contraseña").setText("Futbol2027");
        }, () -> m(field("Nueva contraseña"), field("Repite la nueva contraseña")));
        step("clave-cambiada", "Contraseña cambiada", () -> click(text("Cambiar contraseña", 0)), () -> m(text("Cambiar contraseña", 0)), 3500);
        step("animaciones", "Casilla “Reducir animaciones”", () -> reveal(text("Reducir animaciones")), () -> m(text("Reducir animaciones")));
        step("cerrar-sesion", "Cerrar sesión desde el menú del usuario", () -> ((MenuButton) css(".menu-button")).show(),
                () -> m(css(".menu-button")));
        quiet(() -> ((MenuButton) css(".menu-button")).hide(), 400);
        logout();
    }

    private void administrador() throws Exception {
        section("p");
        login("admin@sportforce3d.pe", "Admin123!");
        step("menu-admin", "Menú de Administración (solo ADMIN)", () -> click(sidebar("Dashboard")), () -> m(text("Administración")), 2500);
        step("dashboard-aviso", "Aviso de datos de pago de prueba", null, () -> m(text("Ir a Configuración")));
        step("dashboard-tarjetas", "Indicadores: ventas de hoy, del mes, pagos por verificar y por producir", null,
                () -> m(text("Ventas de hoy"), text("Ventas del mes"), text("Pagos por verificar"), text("Por producir")));
        step("dashboard-graficos", "Gráfico de ventas, pedidos por estado y más vendidos",
                () -> reveal(text("Pedidos por estado")), () -> m(text("Ventas de los últimos 30 días"), text("Pedidos por estado")));

        section("q");
        step("pagos", "Pantalla Pagos: cola de pagos por verificar", () -> click(sidebar("Pagos")), () -> m(sidebar("Pagos")), 2000);
        step("pago-tarjeta", "Datos del pago: pedido, monto, cliente, método y operación", null,
                () -> m(textStarting("Operación"), text("Aprobar", 0), text("Rechazar", 0), text("Descargar comprobante", 0)));
        step("aprobar", "Botón Aprobar: pide confirmación", () -> click(buttonNear(all(n -> n instanceof javafx.scene.control.Labeled l
                && l.getText() != null && l.getText().endsWith("· Yape")).get(0), "Aprobar")), null, 1200);
        step("aprobar-nota", "Nota de la aprobación", () -> {
            List<Node> inputs = all(x -> x instanceof TextInputControl && !isIn(x, "sf-topbar"));
            ((TextInputControl) inputs.get(inputs.size() - 1)).setText("Visto en la app de Yape");
        }, () -> m(text("Aprobar pago")));
        step("aprobado", "Pago aprobado: el pedido pasa a Pagado", () -> click(text("Aprobar pago")), null, 2500);
        step("rechazar", "Botón Rechazar: pide el motivo", () -> click(text("Rechazar", 0)), null, 1200);
        step("rechazar-motivo", "Motivo del rechazo (lo verá el cliente)", () -> nth(javafx.scene.control.TextArea.class, 0)
                .setText("El código 55129034 no aparece en los movimientos de Plin de hoy."), () -> m(text("Rechazar pago")));
        step("rechazado", "Pago rechazado", () -> click(text("Rechazar pago")), null, 2500);
        step("aprobados", "Filtro Aprobados", () -> click(text("Aprobados")), () -> m(text("Aprobados")), 1500);
        step("rechazados", "Filtro Rechazados", () -> click(text("Rechazados")), () -> m(text("Rechazados")), 1500);
        step("descargar", "Botón “Descargar comprobante”", () -> click(text("Aprobados")), () -> m(text("Descargar comprobante", 0)), 1500);

        section("r");
        step("pedidos", "Pantalla Pedidos", () -> click(sidebar("Pedidos")), () -> m(sidebar("Pedidos")), 2000);
        step("pedidos-buscar", "Buscar pedidos por número, cliente o correo", () -> field("Buscar").setText("Mamani"), () -> m(field("Buscar")), 1500);
        step("pedidos-filtro", "Filtro por estado: Pagado", () -> click(textStarting("Pagado (")), () -> m(textStarting("Pagado (")), 1500);
        step("pedido-abrir", "Botón Abrir de un pedido", null, () -> m(text("Abrir", 0)));
        step("pedido-detalle", "Detalle del pedido para el ADMIN", () -> click(text("Abrir", 0)), null, 2500);
        step("pedido-recorrido", "Recorrido del pedido y productos", null, () -> m(text("Recorrido")));
        step("pedido-estado", "Cambiar el estado: lista Nuevo estado", () -> {
            ComboBox<?> c = nth(ComboBox.class, 0);
            reveal(c);
            c.show();
        }, () -> m(nth(ComboBox.class, 0)));
        step("pedido-produccion", "Nuevo estado “En producción” con nota", () -> {
            @SuppressWarnings("unchecked")
            ComboBox<Object> c = nth(ComboBox.class, 0);
            c.hide();
            for (Object o : c.getItems()) {
                if (o.toString().contains("PRODUCCION") || o.toString().toLowerCase().contains("producción")) {
                    c.getSelectionModel().select(o);
                }
            }
            field("Nota para el historial").setText("Sublimado en curso");
        }, () -> m(nth(ComboBox.class, 0), field("Nota para el historial"), text("Cambiar estado")));
        step("pedido-cambiado", "Estado cambiado a En producción", () -> click(text("Cambiar estado")), null, 2500);
        quiet(() -> {
            @SuppressWarnings("unchecked")
            ComboBox<Object> c = nth(ComboBox.class, 0);
            for (Object o : c.getItems()) {
                if (o.toString().contains("ENVIADO") || o.toString().toLowerCase().contains("enviado")) {
                    c.getSelectionModel().select(o);
                }
            }
            field("Nota para el historial").setText("Enviado por Olva Courier, guía 123456");
            click(text("Cambiar estado"));
        }, 2500);
        step("pedido-enviado", "Pedido Enviado con su historial", () -> reveal(text("Historial")), () -> m(text("Historial")));
        step("ver-uniforme", "Botón “Ver uniforme 3D y ficha técnica”", () -> {
            top();
            reveal(text("Ver uniforme 3D y ficha técnica"));
        }, () -> m(text("Ver uniforme 3D y ficha técnica")));
        step("uniforme", "Visor del uniforme (solo lectura)", () -> click(text("Ver uniforme 3D y ficha técnica")), null, 6000);
        for (String v : new String[]{"Espalda", "Izquierda", "Derecha", "Frente"}) {
            step("uniforme-" + slug(v), "Visor del uniforme: " + v, () -> click(text(v)), () -> m(text(v)), 1800);
        }
        step("uniforme-capturas", "Capturas del pedido y botón “Descargar archivos” (ficha CSV)", null,
                () -> m(text("Capturas del pedido"), text("Descargar archivos")));
        step("ficha-realista", "Ficha técnica: modelo y colores HEX de la vista realista", () -> reveal(text("Vista realista")),
                () -> m(text("Vista realista")));
        step("ficha-corte", "Ficha técnica: corte y colores por zona", () -> reveal(text("Corte")),
                () -> m(text("Corte"), text("Colores por zona (visor 3D)")));
        step("ficha-materiales", "Ficha técnica: materiales, patrones y elementos", () -> reveal(text("Materiales")),
                () -> m(text("Materiales"), text("Patrones")));

        section("s");
        step("productos", "Pantalla Productos", () -> click(sidebar("Productos")), () -> m(sidebar("Productos")), 2000);
        step("productos-buscar", "Buscar productos por nombre o SKU", () -> field("Buscar").setText("uniforme"), () -> m(field("Buscar")), 1500);
        quiet(() -> field("Buscar").setText(""), 1200);
        step("nuevo", "Botón “Nuevo producto”", null, () -> m(text("Nuevo producto")));
        step("nuevo-form", "Formulario de producto nuevo", () -> click(text("Nuevo producto")), null, 2000);
        step("nuevo-datos", "Nombre, SKU, precio y descripción", () -> {
            field("Nombre").setText("Camiseta Entrenamiento Andina");
            field("SKU").setText("SF-CAM-090");
            field("Precio (S/)").setText("69.90");
            field("Precio anterior (opcional)").setText("79.90");
            nth(javafx.scene.control.TextArea.class, 0).setText("Camiseta liviana para entrenar en altura.");
        }, () -> m(field("Nombre"), field("SKU"), field("Precio (S/)"), field("Precio anterior (opcional)")));
        step("nuevo-categoria", "Lista Categoría", () -> nth(ComboBox.class, 0).show(), () -> m(nth(ComboBox.class, 0)));
        quiet(() -> {
            ComboBox<?> c = nth(ComboBox.class, 0);
            c.hide();
            c.getSelectionModel().select(0);
        }, 800);
        step("nuevo-casillas", "Casillas Activo, Personalizable, Nuevo y Destacado", () -> {
            reveal(text("Activo (se vende en la tienda)"));
            CheckBox nuevo = (CheckBox) text("Marcar como nuevo");
            nuevo.setSelected(true);
        }, () -> m(text("Activo (se vende en la tienda)"), text("Marcar como nuevo"), text("Destacado en el inicio")));
        step("nuevo-telas", "Telas y materiales que se ofrecen", () -> {
            reveal(text("Telas y materiales que se ofrecen"));
            ((CheckBox) textStarting("Dry-Fit")).setSelected(true);
            ((CheckBox) textStarting("Malla deportiva")).setSelected(true);
        }, () -> m(text("Telas y materiales que se ofrecen")));
        step("nuevo-tallas", "Botón “Agregar todas las tallas”", () -> {
            reveal(text("Agregar todas las tallas"));
            click(text("Agregar todas las tallas"));
        }, () -> m(text("Agregar todas las tallas")), 1500);
        step("nuevo-variantes", "Variantes: color, HEX, SKU, ajuste, stock y aviso", () -> {
            for (Node row : all(x -> x.getClass().getSimpleName().contains("VariantRow"))) {
                boolean sinTalla = row.lookupAll(".combo-box").stream()
                        .anyMatch(c -> ((ComboBox<?>) c).getSelectionModel().isEmpty());
                if (sinTalla) {
                    click(row.lookupAll(".sf-btn-icon").iterator().next());
                }
            }
            for (Node n : all(x -> x instanceof TextInputControl t && t.getText() != null && t.getText().equals("0")
                    && t.getParent() != null && t.getParent().getClass().getSimpleName().contains("VariantRow"))) {
                ((TextInputControl) n).setText("20");
            }
            reveal(text("Variantes, precio y stock"));
        }, () -> m(text("Variantes, precio y stock")));
        step("nuevo-guardar", "Botones Cancelar y “Guardar producto”", () -> reveal(text("Guardar producto")),
                () -> m(text("Guardar producto"), text("Cancelar")));
        step("nuevo-guardado", "Producto guardado", () -> click(text("Guardar producto")), null, 2500);
        dump("s-guardado");
        go(PpqhRoutes.PPQH_ADMIN_PRODUCTS, null);
        step("editar", "Botón Editar de un producto", () -> field("Buscar").setText("Uniforme Fútbol Pro"), () -> m(text("Editar", 0)), 1500);
        step("editar-form", "Editar producto", () -> click(text("Editar", 0)), null, 2500);
        step("editar-precio", "Cambiar el precio", () -> field("Precio (S/)").setText("154.90"), () -> m(field("Precio (S/)")));
        step("zonas", "Foto realista: colores por zona", () -> reveal(text("Foto realista")), () -> m(text("Foto realista")), 1200);
        step("zona-camiseta", "Color de la zona Camiseta", () -> {
            reveal(text("Camiseta"));
            Node g = nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 0);
            click(g.lookupAll(".sf-swatch").stream().skip(3).findFirst().orElseThrow());
        }, () -> m(nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 0)), 2500);
        step("zona-medias", "Color de la zona Medias con el campo HEX", () -> {
            Node g = nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 3);
            reveal(g);
            TextInputControl t = (TextInputControl) g.lookup(".text-field");
            t.setText("#FFFFFF");
            t.fireEvent(new javafx.event.ActionEvent());
        }, () -> m(nth(com.sportforce3d.component.PgeyColorSwatchGroup.class, 3)), 2500);
        step("imagenes", "Imágenes del producto (Agregar imagen)", () -> reveal(text("Agregar imagen")), () -> m(text("Agregar imagen")));
        step("editar-guardado", "Cambios guardados", () -> click(text("Guardar producto")), null, 2500);
        go(PpqhRoutes.PPQH_ADMIN_PRODUCTS, null);
        eliminarProducto();

        section("t");
        inventario();


        section("u");
        step("reportes", "Pantalla Reportes", () -> click(sidebar("Reportes")), () -> m(sidebar("Reportes")), 2000);
        step("reportes-fechas", "Fechas Desde y Hasta", () -> nth(DatePicker.class, 0).show(), () -> m(nth(DatePicker.class, 0), nth(DatePicker.class, 1)));
        quiet(() -> nth(DatePicker.class, 0).hide(), 500);
        step("reportes-rangos", "Rangos rápidos", () -> click(text("Hoy")), () -> m(css("#presets")), 1200);
        step("reportes-generar", "Botón “Generar reporte”", () -> click(text("Generar reporte")), () -> m(text("Generar reporte")), 2500);
        step("reportes-resultado", "Resultado del reporte", () -> reveal(text("Ventas totales")), () -> m(text("Ventas totales")));
        step("reportes-detalle", "Detalle por categoría, producto y método", () -> {
            ScrollPane sp = (ScrollPane) all(x -> x instanceof ScrollPane).get(0);
            sp.setVvalue(0.7);
        }, null, 1200);
        step("reportes-exportar", "Botones “Exportar PDF” y “Exportar Excel”", () -> top(), () -> m(text("Exportar PDF"), text("Exportar Excel")));

        section("v");
        step("usuarios", "Pantalla Usuarios", () -> click(sidebar("Usuarios")), () -> m(sidebar("Usuarios")), 2000);
        step("usuarios-buscar", "Buscar un usuario", () -> field("Buscar").setText("Mamani"), () -> m(field("Buscar")), 1500);
        step("usuarios-acciones", "Botones Desactivar y Hacer admin", null, () -> m(text("Desactivar", 0), text("Hacer admin", 0)));
        step("usuarios-confirmar", "Confirmación para desactivar la cuenta", () -> click(text("Desactivar", 0)), null, 1200);
        step("usuarios-desactivado", "Cuenta desactivada", () -> {
            List<Node> b = all(n -> n instanceof javafx.scene.control.ButtonBase bb && "Desactivar".equals(bb.getText()));
            click(b.get(b.size() - 1));
        }, null, 2000);
        activar();
        step("usuarios-actualizar", "Botón Actualizar", null, () -> m(text("Actualizar")));

        section("w");
        step("config", "Pantalla Configuración", () -> click(sidebar("Configuración")), () -> m(sidebar("Configuración")), 2000);
        step("config-yape", "Datos de Yape: número y titular", () -> {
            field("Número de Yape").setText("912 345 678");
            field("Titular de Yape").setText("SPORTFORCE 3D S.A.C.");
        }, () -> m(field("Número de Yape"), field("Titular de Yape")));
        step("config-plin", "Datos de Plin: número y titular", () -> {
            reveal(field("Número de Plin"));
            field("Número de Plin").setText("923 456 789");
            field("Titular de Plin").setText("SPORTFORCE 3D S.A.C.");
        }, () -> m(field("Número de Plin"), field("Titular de Plin")));
        step("config-banco", "Datos de la transferencia bancaria", () -> {
            reveal(field("Banco"));
            field("Banco").setText("BCP");
            field("Titular de la cuenta").setText("SPORTFORCE 3D S.A.C.");
            field("Número de cuenta").setText("191-23456789-0-12");
            field("CCI (20 dígitos)").setText("00219100234567890125");
        }, () -> m(field("Banco"), field("Titular de la cuenta"), field("Número de cuenta"), field("CCI (20 dígitos)")));
        step("config-vista", "Vista previa “Así lo ve el cliente”", () -> reveal(text("Así lo ve el cliente")), () -> m(text("Así lo ve el cliente")));
        step("config-error", "Error de validación (CCI incompleto)", () -> {
            field("CCI (20 dígitos)").setText("0021");
            click(text("Guardar datos de pago"));
        }, () -> m(field("CCI (20 dígitos)")), 1500);
        step("config-guardado", "Datos de pago guardados", () -> {
            field("CCI (20 dígitos)").setText("00219100234567890125");
            click(text("Guardar datos de pago"));
        }, () -> m(text("Guardar datos de pago"), text("Descartar cambios")), 2500);
        logout();
    }

    private Node lastButton(String text) {
        List<Node> b = all(n -> n instanceof javafx.scene.control.ButtonBase bb && bb.getText() != null
                && norm(bb.getText()).equals(norm(text)));
        if (b.isEmpty()) {
            throw new IllegalStateException("No hay botón " + text);
        }
        return b.get(b.size() - 1);
    }

    private void cancelarPedido() {
        step("cancelar", "Botón “Cancelar pedido”", () -> reveal(text("Cancelar pedido")), () -> m(text("Cancelar pedido")));
        step("cancelar-confirmar", "Confirmación para cancelar el pedido", () -> click(text("Cancelar pedido")), null, 1200);
        step("cancelado", "Pedido cancelado", () -> click(lastButton("Cancelar pedido")), null, 2500);
    }

    private void eliminarProducto() throws Exception {
        go(PpqhRoutes.PPQH_ADMIN_PRODUCTS, null);
        step("eliminar", "Botón Eliminar: pide confirmación", () -> {
            field("Buscar").setText("Andina");
        }, () -> m(text("Eliminar", 0)), 1500);
        step("eliminar-confirmar", "Confirmación de eliminación", () -> click(text("Eliminar", 0)), null, 1200);
        step("eliminado", "Producto eliminado (borrado lógico)", () -> click(lastButton("Eliminar")), null, 2000);
        step("mostrar-eliminados", "Casilla “Mostrar eliminados”", () -> click(text("Mostrar eliminados")),
                () -> m(text("Mostrar eliminados")), 1500);
    }

    private void inventario() {
        step("inventario", "Pantalla Inventario", () -> click(sidebar("Inventario")), () -> m(sidebar("Inventario")), 2000);
        step("inventario-bajo", "Casilla “Solo stock bajo”", () -> click(text("Solo stock bajo")), () -> m(text("Solo stock bajo")), 1500);
        step("inventario-buscar", "Buscar una variante", () -> {
            click(text("Solo stock bajo"));
            field("Buscar").setText("SF-UFU-001-M");
        }, () -> m(field("Buscar")), 1500);
        step("inventario-stock", "Escribir el nuevo stock", () -> {
            TextInputControl t = (TextInputControl) all(x -> x instanceof TextInputControl tt && tt.getParent() != null
                    && !(tt.getParent() instanceof com.sportforce3d.component.PpqhSfTextField)
                    && tt.getText() != null && tt.getText().matches("\\d+")).get(0);
            t.setText(String.valueOf(Integer.parseInt(t.getText()) + 10));
        }, () -> m(text("Guardar", 0)), 1200);
        step("inventario-guardado", "Stock guardado", () -> click(text("Guardar", 0)), null, 2000);
    }

    private void activar() {
        step("usuarios-confirmar-activar", "Confirmación para activar la cuenta otra vez", () -> click(text("Activar", 0)), null, 1200);
        step("usuarios-activado", "Cuenta activada otra vez", () -> {
            List<Node> b = all(n -> n instanceof javafx.scene.control.ButtonBase bb && "Activar".equals(bb.getText()));
            click(b.get(b.size() - 1));
        }, () -> m(text("Desactivar", 0)), 2000);
    }

    void continuar() throws Exception {
        long transfer = sql("SELECT id FROM orders WHERE payment_method = 'TRANSFERENCIA' ORDER BY id DESC LIMIT 1");
        login("admin@sportforce3d.pe", "Admin123!");
        section("s");
        number = 20;
        eliminarProducto();
        section("t");
        inventario();
        section("v");
        number = 5;
        go(PpqhRoutes.PPQH_ADMIN_USERS, null);
        quiet(() -> field("Buscar").setText("Mamani"), 1500);
        activar();
        step("usuarios-actualizar", "Botón Actualizar", null, () -> m(text("Actualizar")));
        logout();
        login(CORREO, "Futbol2027");
        section("l");
        number = 5;
        go(PpqhRoutes.PPQH_ORDER, transfer);
        cancelarPedido();
        logout();
        estadosCliente();
        Files.write(out.resolve("fallos-continuacion.txt"), failures);
        System.out.println("FALLOS: " + failures.size());
        failures.forEach(System.out::println);
    }

    private Node buttonNear(Node anchor, String text) {
        for (Node p = anchor.getParent(); p != null; p = p.getParent()) {
            List<Node> found = new ArrayList<>();
            collectText(p, text, found);
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }
        throw new IllegalStateException("No hay " + text + " cerca");
    }

    private static void collectText(Node n, String text, List<Node> out) {
        if (n instanceof javafx.scene.control.ButtonBase b && b.getText() != null && norm(b.getText()).equals(norm(text))) {
            out.add(n);
        }
        if (n instanceof javafx.scene.Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                collectText(c, text, out);
            }
        }
    }

    private void estadosCliente() throws Exception {
        section("x");
        login(CORREO, "Futbol2027");
        step("pedidos", "Mis pedidos con sus estados", () -> click(sidebar("Mis pedidos")), () -> m(sidebar("Mis pedidos")), 2000);
        step("enviado", "Pedido Enviado: línea de tiempo e historial", () -> click(text("PED-2026-000001").getParent()),
                () -> m(css(".sf-timeline")), 2500);
        step("enviado-historial", "Historial con las notas del administrador", () -> reveal(text("Historial")), () -> m(text("Historial")));
        go(PpqhRoutes.PPQH_ORDERS, null);
        step("rechazado", "Pedido con pago rechazado: motivo y nuevo registro de pago", () -> {
            List<Node> rows = all(n -> n instanceof javafx.scene.control.Labeled l && l.getText() != null && l.getText().contains("Plin"));
            click(rows.get(0).getParent());
        }, null, 2500);
        step("rechazado-motivo", "Motivo del rechazo visible para el cliente", () -> {
            Node motivo = all(n -> n instanceof javafx.scene.control.Labeled l && l.getText() != null
                    && l.getText().contains("no aparece")).get(0);
            reveal(motivo);
        }, () -> m(all(n -> n instanceof javafx.scene.control.Labeled l && l.getText() != null
                && l.getText().contains("no aparece")).get(0), field("Código de operación")), 1200);
    }

    private static String slug(String s) {
        String n = java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private void writeLogo(Path file) throws java.io.IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(256, 256, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(0x0E, 0x7C, 0x3A));
        g.fillOval(8, 8, 240, 240);
        g.setColor(java.awt.Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(10));
        g.drawOval(28, 28, 200, 200);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 92));
        g.drawString("CM", 50, 162);
        g.dispose();
        javax.imageio.ImageIO.write(img, "png", file.toFile());
    }

    Path logoFile() {
        return logo;
    }
}
