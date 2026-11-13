package com.sportforce3d.view;

import com.sportforce3d.app.AppContext;
import com.sportforce3d.app.Async;
import com.sportforce3d.app.Routes;
import com.sportforce3d.component.ProductCard;
import com.sportforce3d.component.Toast;
import com.sportforce3d.model.ProductSummary;
import com.sportforce3d.viewmodel.ErrorMessages;
import javafx.scene.Node;

final class ShopActions {

    private ShopActions() {
    }

    static ProductCard card(AppContext context, ProductSummary product) {
        ProductCard card = new ProductCard(product,
                ProductImages.load(context.storage(), product.imagePath(), ProductCard.WIDTH, ProductCard.MEDIA_HEIGHT));
        if (!card.hasImage() && product.look() != null) {
            ProductImages.realistic(context.recolorService(), product.look(), card::setImage);
        }
        card.setOnOpen(p -> context.router().go(Routes.PRODUCT, p.id()));
        card.setOnAddToCart(p -> addOrChooseSize(context, p, card));
        return card;
    }

    static void addOrChooseSize(AppContext context, ProductSummary product, Node anyNode) {
        if (product.singleVariantId() == null) {
            context.router().go(Routes.PRODUCT, product.id());
            return;
        }
        Async.run(() -> context.cartService().add(product.singleVariantId(), 1),
                summary -> {
                    context.setCartCount(summary.itemCount());
                    Toast.success(anyNode, product.name() + " se agregó al carrito.");
                },
                e -> Toast.error(anyNode, ErrorMessages.of(e)));
    }
}
