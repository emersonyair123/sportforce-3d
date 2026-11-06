package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ConfirmDialog;
import com.sportforce3d.component.EmptyState;
import com.sportforce3d.component.Icons;
import com.sportforce3d.component.Motion;
import com.sportforce3d.component.SfButton;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.DesignSummary;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** "Mis diseños": tarjetas con la captura frontal, versión y fecha; abrir, duplicar y eliminar. */
public class DesignsController {

    private static final double CARD_WIDTH = 248;

    private final AppContext context;

    @FXML private Label countLabel;
    @FXML private SfButton newButton;
    @FXML private FlowPane grid;

    public DesignsController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void initialize() {
        newButton.withIcon(Feather.PLUS);
        newButton.setOnAction(e -> context.router().go(Routes.DESIGNER));
        load();
    }

    private void load() {
        Async.run(context.designService()::myDesigns, this::render,
                e -> grid.getChildren().setAll(new EmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar tus diseños",
                        ErrorMessages.of(e))));
    }

    private void render(List<DesignSummary> designs) {
        countLabel.setText(designs.isEmpty() ? "" : designs.size() == 1 ? "1 diseño guardado" : designs.size() + " diseños guardados");
        if (designs.isEmpty()) {
            SfButton start = new SfButton("Abrir el diseñador 3D", SfButton.Variant.PRIMARY).withIcon(Feather.BOX);
            start.setOnAction(e -> context.router().go(Routes.DESIGNER));
            grid.getChildren().setAll(new EmptyState(Feather.LAYERS, "Aún no tienes diseños",
                    "Personaliza un uniforme en el diseñador 3D y guárdalo: aparecerá aquí.", start));
            return;
        }
        grid.getChildren().clear();
        for (DesignSummary d : designs) {
            grid.getChildren().add(card(d));
        }
    }

    private VBox card(DesignSummary d) {
        StackPane thumb = new StackPane();
        thumb.getStyleClass().add("sf-design-thumb");
        thumb.setPrefSize(CARD_WIDTH, CARD_WIDTH * 4 / 3);
        thumb.setMinSize(CARD_WIDTH, CARD_WIDTH * 4 / 3);
        thumb.getChildren().add(Icons.of(Feather.IMAGE, Icons.MD));
        if (d.thumbnailFront() != null) {
            Path file = context.storage().resolve(d.thumbnailFront());
            if (Files.exists(file)) {
                Image image = new Image(file.toUri().toString(), CARD_WIDTH * 4, 0, true, true, true);
                ImageView view = new ImageView(image);
                view.setFitWidth(CARD_WIDTH);
                view.setFitHeight(CARD_WIDTH * 4 / 3);
                // La captura es de cuerpo completo con el encuadre del visor: se recorta al centro en 3:4.
                Runnable crop = () -> {
                    double w = image.getWidth();
                    double h = image.getHeight();
                    double ch = h * 0.92;
                    double cw = Math.min(w, ch * 3 / 4);
                    ch = cw * 4 / 3;
                    view.setViewport(new javafx.geometry.Rectangle2D((w - cw) / 2, (h - ch) / 2, cw, ch));
                };
                if (image.getProgress() >= 1) {
                    crop.run();
                } else {
                    image.progressProperty().addListener((obs, old, p) -> {
                        if (p.doubleValue() >= 1 && !image.isError()) {
                            crop.run();
                        }
                    });
                }
                thumb.getChildren().setAll(view);
            }
        }
        // Esquinas superiores redondeadas también para la imagen (el fondo ya las tiene por CSS).
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(thumb.widthProperty());
        clip.heightProperty().bind(thumb.heightProperty().add(30));
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        thumb.setClip(clip);
        thumb.setCursor(Cursor.HAND);
        thumb.setOnMouseClicked(e -> open(d));

        Label name = new Label(d.name());
        name.getStyleClass().addAll("title-s", "ink");
        name.setWrapText(true);
        Label meta = new Label("Versión " + d.version() + " · " + date(d.updatedAt()));
        meta.getStyleClass().addAll("body-s", "ink-muted");

        SfButton open = new SfButton("Abrir", SfButton.Variant.PRIMARY, SfButton.Size.SM);
        open.setOnAction(e -> open(d));
        SfButton duplicate = SfButton.icon(Feather.COPY, "Duplicar");
        duplicate.setOnAction(e -> Async.run(() -> context.designService().duplicate(d.id()), copy -> {
            Toast.success(grid, "Se creó “" + copy.name() + "”.");
            load();
        }, ex -> Toast.error(grid, ErrorMessages.of(ex))));
        SfButton delete = SfButton.icon(Feather.TRASH_2, "Eliminar");
        delete.setOnAction(e -> ConfirmDialog.show(grid, "¿Eliminar “" + d.name() + "”?",
                "El diseño dejará de aparecer en tu lista. Los pedidos que ya lo usan no cambian.", "Eliminar", true,
                () -> Async.run(() -> {
                    context.designService().delete(d.id());
                    return true;
                }, ok -> {
                    Toast.success(grid, "Diseño eliminado.");
                    load();
                }, ex -> Toast.error(grid, ErrorMessages.of(ex)))));
        HBox actions = new HBox(6, open, duplicate, delete);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox body = new VBox(6, name, meta, actions);
        body.getStyleClass().add("sf-design-body");
        VBox card = new VBox(thumb, body);
        card.getStyleClass().addAll("sf-card", "sf-design-card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        Motion.liftOnHover(card);
        return card;
    }

    private void open(DesignSummary d) {
        context.router().go(Routes.DESIGNER, Routes.DesignerParams.open(d.id()));
    }

    private static String date(String iso) {
        try {
            return OrdersController.DATE.format(Instant.parse(iso));
        } catch (RuntimeException e) {
            return iso;
        }
    }
}