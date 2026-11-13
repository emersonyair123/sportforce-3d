package com.sportforce3d.view;

import com.sportforce3d.design.PgeyRealisticSpec;
import com.sportforce3d.design.PgeyRealisticView;
import com.sportforce3d.three.PgeyElementImages;
import com.sportforce3d.render.PgeyArgbImage;
import com.sportforce3d.render.PgeyRealisticModel;
import com.sportforce3d.render.PgeyRealisticModelRepository;
import com.sportforce3d.render.PgeyRecolorRequest;
import com.sportforce3d.render.PgeyRecolorService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

final class PgeyRealisticPreview {

    private final StackPane pgeyNode = new StackPane();
    private final ImageView pgeyFront = pgeyView();
    private final ImageView pgeyBack = pgeyView();
    private final VBox pgeyBackCard;
    private final Label pgeyWaiting = new Label("Preparando la vista realista…");
    private final Label pgeyNotices = new Label();

    PgeyRealisticPreview(double pgeyReserved) {
        pgeyNode.getStyleClass().add("sf-designer-stage");
        VBox pgeyFrontCard = pgeyCard("FRENTE", pgeyFront);
        pgeyBackCard = pgeyCard("ESPALDA", pgeyBack);
        HBox pgeyPair = new HBox(24, pgeyFrontCard, pgeyBackCard);
        pgeyPair.setAlignment(Pos.CENTER);
        pgeyWaiting.getStyleClass().addAll("body-s", "ink-muted");
        pgeyNotices.getStyleClass().addAll("body-s", "ink-muted", "sf-realistic-notices");
        pgeyNotices.setWrapText(true);
        pgeyNotices.setMaxWidth(640);
        pgeyNotices.setVisible(false);
        StackPane.setAlignment(pgeyNotices, Pos.BOTTOM_CENTER);
        StackPane.setMargin(pgeyNotices, new javafx.geometry.Insets(0, 16, 40, 16));
        pgeyNode.getChildren().addAll(pgeyPair, pgeyWaiting, pgeyNotices);
        Runnable pgeyFit = () -> {
            double pgeyH = Math.max(200, pgeyNode.getHeight() - pgeyReserved);
            double pgeyW = Math.max(160, (pgeyNode.getWidth() - 96) / (pgeyBackCard.isManaged() ? 2 : 1));
            double pgeyHeight = Math.min(pgeyH, pgeyW * 900 / 720);
            pgeyFront.setFitHeight(pgeyHeight);
            pgeyBack.setFitHeight(pgeyHeight);
        };
        pgeyNode.heightProperty().addListener((pgeyO, pgeyA, pgeyB) -> pgeyFit.run());
        pgeyNode.widthProperty().addListener((pgeyO, pgeyA, pgeyB) -> pgeyFit.run());
        pgeyBackCard.managedProperty().addListener((pgeyO, pgeyA, pgeyB) -> pgeyFit.run());
    }

    Node pgeyNode() {
        return pgeyNode;
    }

    void pgeyShow(Image pgeyFrontImage, Image pgeyBackImage) {
        pgeyFront.setImage(pgeyFrontImage);
        pgeyBack.setImage(pgeyBackImage);
        pgeyBackCard.setVisible(pgeyBackImage != null);
        pgeyBackCard.setManaged(pgeyBackImage != null);
        pgeyWaiting.setVisible(false);
    }

    void pgeyShowNotices(java.util.List<String> pgeyList) {
        pgeyNotices.setText(String.join(" · ", pgeyList));
        pgeyNotices.setVisible(!pgeyList.isEmpty());
    }

    String pgeyNoticesText() {
        return pgeyNotices.isVisible() ? pgeyNotices.getText() : "";
    }

    void pgeyShowMessage(String pgeyText) {
        pgeyWaiting.setText(pgeyText);
        pgeyWaiting.setVisible(true);
    }

    static PgeyRealisticModel pgeyFrontModel(PgeyRealisticModelRepository pgeyModels, PgeyRealisticSpec pgeySpec) {
        return pgeyModels.pgeyFind(pgeySpec.pgeyModelKey()).orElseGet(() -> pgeyModels.pgeyRequire(PgeyRealisticSpec.PGEY_DEFAULT_MODEL));
    }

    record PgeyCrest(PgeyArgbImage pgeyPixels, String pgeyKey) {
        static final PgeyCrest PGEY_NONE = new PgeyCrest(null, "-");
    }

    static PgeyCrest pgeyCrest(PgeyElementImages pgeyImages, com.sportforce3d.design.PgeyDesignElement pgeyE) {
        if (pgeyE == null || pgeyImages == null) {
            return PgeyCrest.PGEY_NONE;
        }
        javafx.scene.image.Image pgeyImg = pgeyE.pgeyType() == com.sportforce3d.design.PgeyElementType.LOGO ? pgeyImages.pgeyLogo(pgeyE.pgeyAssetPath())
                : pgeyImages.pgeyEmblem(pgeyE.pgeyEmblem(), pgeyE.pgeyColor(), pgeyE.pgeyAccentColor());
        if (pgeyImg == null || pgeyImg.getPixelReader() == null) {
            return PgeyCrest.PGEY_NONE;
        }
        int pgeyW = (int) pgeyImg.getWidth();
        int pgeyH = (int) pgeyImg.getHeight();
        int[] pgeyPx = new int[pgeyW * pgeyH];
        pgeyImg.getPixelReader().getPixels(0, 0, pgeyW, pgeyH, javafx.scene.image.PixelFormat.getIntArgbInstance(), pgeyPx, 0, pgeyW);
        String pgeyKey = pgeyE.pgeyType() == com.sportforce3d.design.PgeyElementType.LOGO ? "l:" + pgeyE.pgeyAssetPath()
                : "e:" + pgeyE.pgeyEmblem() + ":" + pgeyE.pgeyColor() + ":" + pgeyE.pgeyAccentColor();
        return new PgeyCrest(new PgeyArgbImage(pgeyW, pgeyH, pgeyPx), pgeyKey);
    }

    static PgeyArgbImage[] pgeyRenderPixels(PgeyRecolorService pgeyRecolor, PgeyRealisticView pgeyView, PgeyCrest pgeyCrest) {
        PgeyRealisticModelRepository pgeyModels = pgeyRecolor.pgeyModels();
        PgeyRealisticModel pgeyF = pgeyFrontModel(pgeyModels, pgeyView.pgeySpec());
        PgeyArgbImage pgeyFrontPx = pgeyRecolor.pgeyRenderPixels(PgeyRecolorRequest.pgeyOf(pgeyF.pgeyKey(), pgeyView, pgeyCrest.pgeyPixels(), pgeyCrest.pgeyKey()));
        PgeyArgbImage pgeyBackPx = pgeyModels.pgeyOtherView(pgeyF)
                .map(pgeyB -> pgeyRecolor.pgeyRenderPixels(PgeyRecolorRequest.pgeyOf(pgeyB.pgeyKey(), pgeyView, pgeyCrest.pgeyPixels(), pgeyCrest.pgeyKey())))
                .orElse(null);
        return new PgeyArgbImage[]{pgeyFrontPx, pgeyBackPx};
    }

    static Image[] pgeyRender(PgeyRecolorService pgeyRecolor, PgeyRealisticView pgeyView, PgeyCrest pgeyCrest) {
        PgeyArgbImage[] pgeyPx = pgeyRenderPixels(pgeyRecolor, pgeyView, pgeyCrest);
        return new Image[]{PgeyRecolorService.pgeyToImage(pgeyPx[0]), pgeyPx[1] == null ? null : PgeyRecolorService.pgeyToImage(pgeyPx[1])};
    }

    static PgeyRecolorRequest pgeyRequest(String pgeyModelKey, PgeyRealisticSpec pgeySpec) {
        return PgeyRecolorRequest.pgeyOf(pgeyModelKey, pgeySpec);
    }

    static PgeyArgbImage[] pgeyRenderPixels(PgeyRecolorService pgeyRecolor, PgeyRealisticSpec pgeySpec) {
        PgeyRealisticModelRepository pgeyModels = pgeyRecolor.pgeyModels();
        PgeyRealisticModel pgeyF = pgeyFrontModel(pgeyModels, pgeySpec);
        PgeyArgbImage pgeyFrontPx = pgeyRecolor.pgeyRenderPixels(pgeyRequest(pgeyF.pgeyKey(), pgeySpec));
        PgeyArgbImage pgeyBackPx = pgeyModels.pgeyOtherView(pgeyF).map(pgeyB -> pgeyRecolor.pgeyRenderPixels(pgeyRequest(pgeyB.pgeyKey(), pgeySpec))).orElse(null);
        return new PgeyArgbImage[]{pgeyFrontPx, pgeyBackPx};
    }

    static Image[] pgeyRender(PgeyRecolorService pgeyRecolor, PgeyRealisticSpec pgeySpec) {
        PgeyArgbImage[] pgeyPx = pgeyRenderPixels(pgeyRecolor, pgeySpec);
        return new Image[]{PgeyRecolorService.pgeyToImage(pgeyPx[0]), pgeyPx[1] == null ? null : PgeyRecolorService.pgeyToImage(pgeyPx[1])};
    }

    private static ImageView pgeyView() {
        ImageView pgeyV = new ImageView();
        pgeyV.setPreserveRatio(true);
        pgeyV.setSmooth(true);
        return pgeyV;
    }

    private static VBox pgeyCard(String pgeyTitle, ImageView pgeyImage) {
        Label pgeyLabel = new Label(pgeyTitle);
        pgeyLabel.getStyleClass().addAll("label-text", "ink-muted");
        StackPane pgeyFrame = new StackPane(pgeyImage);
        pgeyFrame.getStyleClass().add("sf-realistic-photo");
        Rectangle pgeyClip = new Rectangle();
        pgeyClip.setArcWidth(24);
        pgeyClip.setArcHeight(24);
        pgeyClip.widthProperty().bind(pgeyFrame.widthProperty());
        pgeyClip.heightProperty().bind(pgeyFrame.heightProperty());
        pgeyFrame.setClip(pgeyClip);
        VBox pgeyBox = new VBox(8, pgeyLabel, pgeyFrame);
        pgeyBox.setAlignment(Pos.CENTER);
        return pgeyBox;
    }
}
