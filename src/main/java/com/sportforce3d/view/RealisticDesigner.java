package com.sportforce3d.view;

import com.sportforce3d.app.Async;
import com.sportforce3d.component.ColorSwatchGroup;
import com.sportforce3d.component.FilterChip;
import com.sportforce3d.component.SfTextField;
import com.sportforce3d.component.Toast;
import com.sportforce3d.design.DesignDefaults;
import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.render.ArgbImage;
import com.sportforce3d.render.RealisticModel;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RealisticPattern;
import com.sportforce3d.render.RecolorRequest;
import com.sportforce3d.render.RecolorService;
import com.sportforce3d.viewmodel.DesignerViewModel;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Vista realista del diseñador: fotos de personas con la prenda recoloreada (frente y espalda lado a lado) y su
 * panel (categoría, corte, colores por zona, diseño, nombre y número). Lee y cambia la vista realista del
 * documento con {@link DesignerViewModel#editRealistic}, así deshacer, borrador y guardado funcionan igual que en 3D.
 * <p>
 * El recoloreado corre en segundo plano y como máximo 30 veces por segundo: si llega un cambio mientras se
 * dibuja, se dibuja solo el último cuando termina el anterior (nunca se acumulan tareas).
 */
final class RealisticDesigner {

    private static final long FRAME_NANOS = 1_000_000_000L / 30;
    private static final Locale ES_PE = Locale.of("es", "PE");
    static final Map<String, String> CATEGORIES = categories();
    static final Map<String, String> CUTS = cuts();

    private final DesignerViewModel viewModel;
    private final RecolorService recolor;
    private final RealisticModelRepository models;
    private final RealisticPreview preview = new RealisticPreview(170);

    private final List<Runnable> syncers = new ArrayList<>();
    private boolean syncing;
    private String zone = RealisticModel.CAMISETA;
    private RealisticSpec shown;
    private boolean busy;
    private long lastFrame;

    private final AnimationTimer loop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (now - lastFrame >= FRAME_NANOS) {
                lastFrame = now;
                renderIfChanged();
            }
        }
    };

    RealisticDesigner(DesignerViewModel viewModel, RecolorService recolor) {
        this.viewModel = viewModel;
        this.recolor = recolor;
        this.models = recolor.models();
    }

    Node stageNode() {
        return preview.node();
    }

    void start() {
        loop.start();
    }

    void stop() {
        loop.stop();
    }

    // ---------------------------------------------------------------- Modelos

    /** Modelo de frente del diseño (si la clave guardada ya no existe, el predeterminado). */
    RealisticModel frontModel(RealisticSpec spec) {
        return RealisticPreview.frontModel(models, spec);
    }

    /** Fotos de frente y espalda (null si el modelo no tiene espalda) en tamaño completo. Cualquier hilo. */
    ArgbImage[] renderPixels(RealisticSpec spec) {
        return RealisticPreview.renderPixels(recolor, spec);
    }

    private void renderIfChanged() {
        RealisticSpec spec = viewModel.realistic();
        if (busy || spec.equals(shown)) {
            return;
        }
        busy = true;
        Async.run(() -> RealisticPreview.render(recolor, spec), images -> {
            busy = false;
            shown = spec;
            preview.show(images[0], images[1]);
        }, e -> {
            busy = false;
            shown = spec;
            Toast.error(preview.node(), "No se pudo dibujar la vista realista. " + ErrorMessages.of(e));
        });
    }

    // ---------------------------------------------------------------- Panel

    /** Clave de estructura: el panel se reconstruye solo si cambia el modelo (sus zonas). */
    String structureKey() {
        return "R|" + frontModel(viewModel.realistic()).key();
    }

    void build(VBox panel) {
        syncers.clear();
        RealisticSpec spec = viewModel.realistic();
        RealisticModel model = frontModel(spec);
        if (!model.hasZone(zone)) {
            zone = model.zones().get(0);
        }

        // Categoría
        FlowPane categories = new FlowPane(6, 6);
        ToggleGroup categoryGroup = new ToggleGroup();
        CATEGORIES.forEach((audience, label) -> {
            FilterChip chip = new FilterChip(label);
            chip.setToggleGroup(categoryGroup);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                String cut = frontModel(viewModel.realistic()).cut();
                String key = models.find(key(cut, audience)).isPresent() ? key(cut, audience) : key("futbol", audience);
                viewModel.editRealistic("Categoría · " + label, s -> s.withModel(key), null);
            });
            sync(() -> chip.setSelected(audience.equals(frontModel(viewModel.realistic()).audience())));
            categories.getChildren().add(chip);
        });
        panel.getChildren().add(section("CATEGORÍA", categories));

        // Corte: solo los que existen para la categoría.
        FlowPane cuts = new FlowPane(6, 6);
        ToggleGroup cutGroup = new ToggleGroup();
        String audience = model.audience();
        CUTS.forEach((cut, label) -> {
            if (models.find(key(cut, audience)).isEmpty()) {
                return;
            }
            FilterChip chip = new FilterChip(label);
            chip.setToggleGroup(cutGroup);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                viewModel.editRealistic("Corte · " + label, s -> s.withModel(key(cut, audience)), null);
            });
            sync(() -> chip.setSelected(cut.equals(frontModel(viewModel.realistic()).cut())));
            cuts.getChildren().add(chip);
        });
        panel.getChildren().add(section("CORTE", cuts));

        // Colores por zona del modelo.
        VBox zones = new VBox(4);
        ToggleGroup zoneGroup = new ToggleGroup();
        for (String z : model.zones()) {
            Rectangle swatch = new Rectangle(16, 16);
            swatch.setArcWidth(5);
            swatch.setArcHeight(5);
            swatch.getStyleClass().add("sf-zone-swatch");
            ToggleButton row = new ToggleButton(zoneLabel(model, z), swatch);
            row.getStyleClass().setAll("sf-zone-row");
            row.setMaxWidth(Double.MAX_VALUE);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setGraphicTextGap(10);
            row.setToggleGroup(zoneGroup);
            row.setOnAction(e -> {
                zone = z;
                refresh();
            });
            sync(() -> {
                swatch.setFill(color(viewModel.realistic().color(z)));
                row.setSelected(zone.equals(z));
            });
            zones.getChildren().add(row);
        }
        ColorSwatchGroup zoneColor = new ColorSwatchGroup();
        zoneColor.valueProperty().addListener((obs, old, c) -> {
            if (!syncing && c != null) {
                String z = zone;
                viewModel.editRealistic("Color · " + zoneLabel(model, z), s -> s.withColor(z, DesignDefaults.hex(c)), null);
            }
        });
        sync(() -> zoneColor.setValue(color(viewModel.realistic().color(zone))));
        panel.getChildren().add(section("COLORES POR ZONA", zones, zoneColor));

        boolean shirt = model.shirt() != null && model.hasZone(RealisticModel.CAMISETA);
        if (!shirt) {
            panel.getChildren().add(section("DISEÑO Y TEXTOS",
                    note("Los buzos, la casaca y los productos solo cambian de color: no llevan diseño, nombre ni número.")));
            return;
        }

        // Diseño de la camiseta con su color.
        FlowPane patterns = new FlowPane(6, 6);
        ToggleGroup patternGroup = new ToggleGroup();
        for (RealisticPattern p : RealisticPattern.values()) {
            FilterChip chip = new FilterChip(p.label());
            chip.setToggleGroup(patternGroup);
            chip.setOnAction(e -> {
                chip.setSelected(true);
                viewModel.editRealistic("Diseño · " + p.label(), s -> s.withPattern(p.code()), null);
            });
            sync(() -> chip.setSelected(p.code().equals(viewModel.realistic().pattern())));
            patterns.getChildren().add(chip);
        }
        ColorSwatchGroup patternColor = new ColorSwatchGroup();
        patternColor.valueProperty().addListener((obs, old, c) -> {
            if (!syncing && c != null) {
                viewModel.editRealistic("Color del diseño", s -> s.withPatternColor(DesignDefaults.hex(c)), null);
            }
        });
        VBox patternOptions = new VBox(10, note("Color del diseño"), patternColor);
        sync(() -> {
            boolean on = !viewModel.realistic().plain();
            patternOptions.setVisible(on);
            patternOptions.setManaged(on);
            patternColor.setValue(color(viewModel.realistic().patternColor()));
        });
        panel.getChildren().add(section("DISEÑO", patterns, patternOptions));

        // Nombre (espalda) y número (frente y espalda).
        SfTextField name = new SfTextField("Nombre (espalda)");
        name.setPromptText("Máx. " + RealisticSpec.NAME_MAX + " letras");
        Inputs.limit(name.getInput(), RealisticSpec.NAME_MAX);
        name.textProperty().addListener((obs, old, text) -> {
            if (!syncing) {
                String clean = text == null || text.isBlank() ? null : text.trim().toUpperCase(ES_PE);
                viewModel.editRealistic("Nombre", s -> s.withName(clean), "realistic-name");
            }
        });
        SfTextField number = new SfTextField("Número (0–99)");
        number.setPromptText("10");
        number.getInput().setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().matches("\\d{0,2}") ? change : null));
        number.textProperty().addListener((obs, old, text) -> {
            if (!syncing) {
                Integer n = text == null || text.isEmpty() ? null : Integer.valueOf(text);
                viewModel.editRealistic("Número", s -> s.withNumber(n), "realistic-number");
            }
        });
        // Al salir del campo termina el gesto: lo escrito queda como un solo paso de deshacer.
        name.getInput().focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                viewModel.seal();
            }
        });
        number.getInput().focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                viewModel.seal();
            }
        });
        sync(() -> {
            RealisticSpec s = viewModel.realistic();
            String n = s.name() == null ? "" : s.name();
            if (!n.equalsIgnoreCase(name.getText() == null ? "" : name.getText().trim())) {
                name.setText(n);
            }
            String num = s.number() == null ? "" : String.valueOf(s.number());
            if (!num.equals(number.getText())) {
                number.setText(num);
            }
        });
        panel.getChildren().add(section("NOMBRE Y NÚMERO", name, number,
                note(models.otherView(model).isPresent()
                        ? "El nombre va en la espalda; el número, en el pecho y en la espalda."
                        : "El número se ve en el pecho. Esta foto no tiene espalda: el nombre y el número de la "
                        + "espalda se ven en la Vista 3D.")));
    }

    void refresh() {
        syncing = true;
        try {
            syncers.forEach(Runnable::run);
        } finally {
            syncing = false;
        }
    }

    private void sync(Runnable r) {
        syncers.add(r);
    }

    // ---------------------------------------------------------------- Ayudas

    static String key(String cut, String audience) {
        return cut + "_" + audience + "_frente";
    }

    static String zoneLabel(RealisticModel model, String zone) {
        return switch (zone) {
            case RealisticModel.CAMISETA -> "Camiseta";
            case RealisticModel.SHORT -> "Short";
            case RealisticModel.PANELES -> "raglan".equals(model.cut()) ? "Mangas" : "Paneles";
            case RealisticModel.MEDIAS -> "Medias";
            case RealisticModel.PRINCIPAL -> "casaca".equals(model.cut()) ? "Casaca" : "Buzo";
            default -> zone;
        };
    }

    private static Map<String, String> categories() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("hombre", "Hombre");
        m.put("mujer", "Mujer");
        m.put("nino", "Niño");
        m.put("nina", "Niña");
        return java.util.Collections.unmodifiableMap(m);
    }

    private static Map<String, String> cuts() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("futbol", "Fútbol cuello V");
        m.put("polo", "Polo");
        m.put("redondo", "Cuello redondo");
        m.put("raglan", "Raglán");
        m.put("mangalarga", "Manga larga");
        m.put("voley", "Vóley");
        m.put("basquet", "Básquet");
        m.put("buzo", "Buzo");
        m.put("shortlargo", "Short largo");
        m.put("licra", "Licra");
        m.put("arquero", "Arquero");
        m.put("compresion", "Compresión");
        m.put("casaca", "Casaca");
        return java.util.Collections.unmodifiableMap(m);
    }

    private static VBox section(String title, Node... content) {
        Label label = new Label(title);
        label.getStyleClass().addAll("label-text", "ink-muted");
        VBox box = new VBox(10, label);
        box.getChildren().addAll(content);
        return box;
    }

    private static Label note(String text) {
        Label l = new Label(text);
        l.getStyleClass().addAll("body-s", "ink-subtle");
        l.setWrapText(true);
        return l;
    }

    private static Color color(String hex) {
        try {
            return Color.web(hex);
        } catch (RuntimeException e) {
            return Color.GRAY;
        }
    }

    /** Frente y espalda lado a lado (para "Guardar captura"). */
    static ArgbImage sideBySide(ArgbImage a, ArgbImage b) {
        if (b == null) {
            return a;
        }
        int gap = 24;
        int w = a.width() + gap + b.width();
        int h = Math.max(a.height(), b.height());
        int[] px = new int[w * h];
        // El espacio entre las fotos queda transparente.
        for (int y = 0; y < a.height(); y++) {
            System.arraycopy(a.pixels(), y * a.width(), px, y * w, a.width());
        }
        for (int y = 0; y < b.height(); y++) {
            System.arraycopy(b.pixels(), y * b.width(), px, y * w + a.width() + gap, b.width());
        }
        return new ArgbImage(w, h, px);
    }

    static void writePng(ArgbImage img, Path file) throws java.io.IOException {
        RecolorService.writePng(img, file);
    }
}
