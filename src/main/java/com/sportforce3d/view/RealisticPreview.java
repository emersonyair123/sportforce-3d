package com.sportforce3d.view;

import com.sportforce3d.design.RealisticSpec;
import com.sportforce3d.design.RealisticView;
import com.sportforce3d.three.ElementImages;
import com.sportforce3d.render.ArgbImage;
import com.sportforce3d.render.RealisticModel;
import com.sportforce3d.render.RealisticModelRepository;
import com.sportforce3d.render.RecolorRequest;
import com.sportforce3d.render.RecolorService;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

final class RealisticPreview {

    private final StackPane node = new StackPane();
    private final ImageView front = view();
    private final ImageView back = view();
    private final VBox backCard;
    private final Label waiting = new Label("Preparando la vista realista…");
    private final Label notices = new Label();

    RealisticPreview(double reserved) {
        node.getStyleClass().add("sf-designer-stage");
        VBox frontCard = card("FRENTE", front);
        backCard = card("ESPALDA", back);
        HBox pair = new HBox(24, frontCard, backCard);
        pair.setAlignment(Pos.CENTER);
        waiting.getStyleClass().addAll("body-s", "ink-muted");
        notices.getStyleClass().addAll("body-s", "ink-muted", "sf-realistic-notices");
        notices.setWrapText(true);
        notices.setMaxWidth(640);
        notices.setVisible(false);
        StackPane.setAlignment(notices, Pos.BOTTOM_CENTER);
        StackPane.setMargin(notices, new javafx.geometry.Insets(0, 16, 40, 16));
        node.getChildren().addAll(pair, waiting, notices);
        Runnable fit = () -> {
            double h = Math.max(200, node.getHeight() - reserved);
            double w = Math.max(160, (node.getWidth() - 96) / (backCard.isManaged() ? 2 : 1));
            double height = Math.min(h, w * 900 / 720);
            front.setFitHeight(height);
            back.setFitHeight(height);
        };
        node.heightProperty().addListener((o, a, b) -> fit.run());
        node.widthProperty().addListener((o, a, b) -> fit.run());
        backCard.managedProperty().addListener((o, a, b) -> fit.run());
    }

    Node node() {
        return node;
    }

    void show(Image frontImage, Image backImage) {
        front.setImage(frontImage);
        back.setImage(backImage);
        backCard.setVisible(backImage != null);
        backCard.setManaged(backImage != null);
        waiting.setVisible(false);
    }

    void showNotices(java.util.List<String> list) {
        notices.setText(String.join(" · ", list));
        notices.setVisible(!list.isEmpty());
    }

    String noticesText() {
        return notices.isVisible() ? notices.getText() : "";
    }

    void showMessage(String text) {
        waiting.setText(text);
        waiting.setVisible(true);
    }

    static RealisticModel frontModel(RealisticModelRepository models, RealisticSpec spec) {
        return models.find(spec.modelKey()).orElseGet(() -> models.require(RealisticSpec.DEFAULT_MODEL));
    }

    record Crest(ArgbImage pixels, String key) {
        static final Crest NONE = new Crest(null, "-");
    }

    static Crest crest(ElementImages images, com.sportforce3d.design.DesignElement e) {
        if (e == null || images == null) {
            return Crest.NONE;
        }
        javafx.scene.image.Image img = e.type() == com.sportforce3d.design.ElementType.LOGO ? images.logo(e.assetPath())
                : images.emblem(e.emblem(), e.color(), e.accentColor());
        if (img == null || img.getPixelReader() == null) {
            return Crest.NONE;
        }
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        int[] px = new int[w * h];
        img.getPixelReader().getPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), px, 0, w);
        String key = e.type() == com.sportforce3d.design.ElementType.LOGO ? "l:" + e.assetPath()
                : "e:" + e.emblem() + ":" + e.color() + ":" + e.accentColor();
        return new Crest(new ArgbImage(w, h, px), key);
    }

    static ArgbImage[] renderPixels(RecolorService recolor, RealisticView view, Crest crest) {
        RealisticModelRepository models = recolor.models();
        RealisticModel f = frontModel(models, view.spec());
        ArgbImage frontPx = recolor.renderPixels(RecolorRequest.of(f.key(), view, crest.pixels(), crest.key()));
        ArgbImage backPx = models.otherView(f)
                .map(b -> recolor.renderPixels(RecolorRequest.of(b.key(), view, crest.pixels(), crest.key())))
                .orElse(null);
        return new ArgbImage[]{frontPx, backPx};
    }

    static Image[] render(RecolorService recolor, RealisticView view, Crest crest) {
        ArgbImage[] px = renderPixels(recolor, view, crest);
        return new Image[]{RecolorService.toImage(px[0]), px[1] == null ? null : RecolorService.toImage(px[1])};
    }

    static RecolorRequest request(String modelKey, RealisticSpec spec) {
        return RecolorRequest.of(modelKey, spec);
    }

    static ArgbImage[] renderPixels(RecolorService recolor, RealisticSpec spec) {
        RealisticModelRepository models = recolor.models();
        RealisticModel f = frontModel(models, spec);
        ArgbImage frontPx = recolor.renderPixels(request(f.key(), spec));
        ArgbImage backPx = models.otherView(f).map(b -> recolor.renderPixels(request(b.key(), spec))).orElse(null);
        return new ArgbImage[]{frontPx, backPx};
    }

    static Image[] render(RecolorService recolor, RealisticSpec spec) {
        ArgbImage[] px = renderPixels(recolor, spec);
        return new Image[]{RecolorService.toImage(px[0]), px[1] == null ? null : RecolorService.toImage(px[1])};
    }

    private static ImageView view() {
        ImageView v = new ImageView();
        v.setPreserveRatio(true);
        v.setSmooth(true);
        return v;
    }

    private static VBox card(String title, ImageView image) {
        Label label = new Label(title);
        label.getStyleClass().addAll("label-text", "ink-muted");
        StackPane frame = new StackPane(image);
        frame.getStyleClass().add("sf-realistic-photo");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(24);
        clip.setArcHeight(24);
        clip.widthProperty().bind(frame.widthProperty());
        clip.heightProperty().bind(frame.heightProperty());
        frame.setClip(clip);
        VBox box = new VBox(8, label, frame);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
