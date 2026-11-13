package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.PpqhConfirmDialog;
import com.sportforce3d.component.PpqhEmptyState;
import com.sportforce3d.component.PpqhIcons;
import com.sportforce3d.component.PpqhMotion;
import com.sportforce3d.component.PpqhSfButton;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.PgeyDesignSummary;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
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

public class PgeyDesignsController {

    private static final double PGEY_CARD_WIDTH = 248;

    private final PpqhAppContext pgeyContext;

    @FXML private Label pgeyCountLabel;
    @FXML private PpqhSfButton pgeyNewButton;
    @FXML private FlowPane pgeyGrid;

    public PgeyDesignsController(PpqhAppContext pgeyContext) {
        this.pgeyContext = pgeyContext;
    }

    @FXML
    private void initialize() {
        pgeyNewButton.ppqhWithIcon(Feather.PLUS);
        pgeyNewButton.setOnAction(pgeyE -> pgeyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER));
        pgeyLoad();
    }

    private void pgeyLoad() {
        PpqhAsync.ppqhRun(pgeyContext.ppqhDesignService()::pgeyMyDesigns, this::pgeyRender,
                pgeyE -> pgeyGrid.getChildren().setAll(new PpqhEmptyState(Feather.ALERT_TRIANGLE, "No se pudo cargar tus diseños",
                        PpqhErrorMessages.ppqhOf(pgeyE))));
    }

    private void pgeyRender(List<PgeyDesignSummary> pgeyDesigns) {
        pgeyCountLabel.setText(pgeyDesigns.isEmpty() ? "" : pgeyDesigns.size() == 1 ? "1 diseño guardado" : pgeyDesigns.size() + " diseños guardados");
        if (pgeyDesigns.isEmpty()) {
            PpqhSfButton pgeyStart = new PpqhSfButton("Abrir el diseñador 3D", PpqhSfButton.PpqhVariant.PRIMARY).ppqhWithIcon(Feather.BOX);
            pgeyStart.setOnAction(pgeyE -> pgeyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER));
            pgeyGrid.getChildren().setAll(new PpqhEmptyState(Feather.LAYERS, "Aún no tienes diseños",
                    "Personaliza un uniforme en el diseñador 3D y guárdalo: aparecerá aquí.", pgeyStart));
            return;
        }
        pgeyGrid.getChildren().clear();
        for (PgeyDesignSummary pgeyD : pgeyDesigns) {
            pgeyGrid.getChildren().add(pgeyCard(pgeyD));
        }
    }

    private VBox pgeyCard(PgeyDesignSummary pgeyD) {
        StackPane pgeyThumb = new StackPane();
        pgeyThumb.getStyleClass().add("sf-design-thumb");
        pgeyThumb.setPrefSize(PGEY_CARD_WIDTH, PGEY_CARD_WIDTH * 4 / 3);
        pgeyThumb.setMinSize(PGEY_CARD_WIDTH, PGEY_CARD_WIDTH * 4 / 3);
        pgeyThumb.getChildren().add(PpqhIcons.ppqhOf(Feather.IMAGE, PpqhIcons.PPQH_MD));
        if (pgeyD.pgeyThumbnailFront() != null) {
            Path pgeyFile = pgeyContext.ppqhStorage().hepyResolve(pgeyD.pgeyThumbnailFront());
            if (Files.exists(pgeyFile)) {
                Image pgeyImage = new Image(pgeyFile.toUri().toString(), PGEY_CARD_WIDTH * 4, 0, true, true, true);
                ImageView pgeyView = new ImageView(pgeyImage);
                pgeyView.setFitWidth(PGEY_CARD_WIDTH);
                pgeyView.setFitHeight(PGEY_CARD_WIDTH * 4 / 3);
                Runnable pgeyCrop = () -> {
                    double pgeyW = pgeyImage.getWidth();
                    double pgeyH = pgeyImage.getHeight();
                    double pgeyCh = pgeyH * 0.92;
                    double pgeyCw = Math.min(pgeyW, pgeyCh * 3 / 4);
                    pgeyCh = pgeyCw * 4 / 3;
                    pgeyView.setViewport(new javafx.geometry.Rectangle2D((pgeyW - pgeyCw) / 2, (pgeyH - pgeyCh) / 2, pgeyCw, pgeyCh));
                };
                if (pgeyImage.getProgress() >= 1) {
                    pgeyCrop.run();
                } else {
                    pgeyImage.progressProperty().addListener((pgeyObs, pgeyOld, pgeyP) -> {
                        if (pgeyP.doubleValue() >= 1 && !pgeyImage.isError()) {
                            pgeyCrop.run();
                        }
                    });
                }
                pgeyThumb.getChildren().setAll(pgeyView);
            }
        }
        javafx.scene.shape.Rectangle pgeyClip = new javafx.scene.shape.Rectangle();
        pgeyClip.widthProperty().bind(pgeyThumb.widthProperty());
        pgeyClip.heightProperty().bind(pgeyThumb.heightProperty().add(30));
        pgeyClip.setArcWidth(30);
        pgeyClip.setArcHeight(30);
        pgeyThumb.setClip(pgeyClip);
        pgeyThumb.setCursor(Cursor.HAND);
        pgeyThumb.setOnMouseClicked(pgeyE -> pgeyOpen(pgeyD));

        Label pgeyName = new Label(pgeyD.pgeyName());
        pgeyName.getStyleClass().addAll("title-s", "ink");
        pgeyName.setWrapText(true);
        Label pgeyMeta = new Label("Versión " + pgeyD.pgeyVersion() + " · " + pgeyDate(pgeyD.pgeyUpdatedAt()));
        pgeyMeta.getStyleClass().addAll("body-s", "ink-muted");

        PpqhSfButton pgeyOpen = new PpqhSfButton("Abrir", PpqhSfButton.PpqhVariant.PRIMARY, PpqhSfButton.PpqhSize.SM);
        pgeyOpen.setOnAction(pgeyE -> pgeyOpen(pgeyD));
        PpqhSfButton pgeyDuplicate = PpqhSfButton.ppqhIcon(Feather.COPY, "Duplicar");
        pgeyDuplicate.setOnAction(pgeyE -> PpqhAsync.ppqhRun(() -> pgeyContext.ppqhDesignService().pgeyDuplicate(pgeyD.pgeyId()), pgeyCopy -> {
            PpqhToast.ppqhSuccess(pgeyGrid, "Se creó “" + pgeyCopy.pgeyName() + "”.");
            pgeyLoad();
        }, pgeyEx -> PpqhToast.ppqhError(pgeyGrid, PpqhErrorMessages.ppqhOf(pgeyEx))));
        PpqhSfButton pgeyDelete = PpqhSfButton.ppqhIcon(Feather.TRASH_2, "Eliminar");
        pgeyDelete.setOnAction(pgeyE -> PpqhConfirmDialog.ppqhShow(pgeyGrid, "¿Eliminar “" + pgeyD.pgeyName() + "”?",
                "El diseño dejará de aparecer en tu lista. Los pedidos que ya lo usan no cambian.", "Eliminar", true,
                () -> PpqhAsync.ppqhRun(() -> {
                    pgeyContext.ppqhDesignService().pgeyDelete(pgeyD.pgeyId());
                    return true;
                }, pgeyOk -> {
                    PpqhToast.ppqhSuccess(pgeyGrid, "Diseño eliminado.");
                    pgeyLoad();
                }, pgeyEx -> PpqhToast.ppqhError(pgeyGrid, PpqhErrorMessages.ppqhOf(pgeyEx)))));
        HBox pgeyActions = new HBox(6, pgeyOpen, pgeyDuplicate, pgeyDelete);
        pgeyActions.setAlignment(Pos.CENTER_LEFT);

        VBox pgeyBody = new VBox(6, pgeyName, pgeyMeta, pgeyActions);
        pgeyBody.getStyleClass().add("sf-design-body");
        VBox pgeyCard = new VBox(pgeyThumb, pgeyBody);
        pgeyCard.getStyleClass().addAll("sf-card", "sf-design-card");
        pgeyCard.setPrefWidth(PGEY_CARD_WIDTH);
        pgeyCard.setMaxWidth(PGEY_CARD_WIDTH);
        PpqhMotion.ppqhLiftOnHover(pgeyCard);
        return pgeyCard;
    }

    private void pgeyOpen(PgeyDesignSummary pgeyD) {
        pgeyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_DESIGNER, PpqhRoutes.PpqhDesignerParams.ppqhOpen(pgeyD.pgeyId()));
    }

    private static String pgeyDate(String pgeyIso) {
        try {
            return PpqhOrdersController.PPQH_DATE.format(Instant.parse(pgeyIso));
        } catch (RuntimeException pgeyE) {
            return pgeyIso;
        }
    }
}
