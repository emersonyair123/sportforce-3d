package com.sportforce3d.view;

import com.sportforce3d.app.PpqhAppContext;
import com.sportforce3d.app.PpqhAsync;
import com.sportforce3d.app.PpqhRoutes;
import com.sportforce3d.component.HepyProductCard;
import com.sportforce3d.component.PpqhToast;
import com.sportforce3d.model.HepyProductSummary;
import com.sportforce3d.viewmodel.PpqhErrorMessages;
import javafx.scene.Node;

final class HepyShopActions {

    private HepyShopActions() {
    }

    static HepyProductCard hepyCard(PpqhAppContext hepyContext, HepyProductSummary hepyProduct) {
        HepyProductCard hepyCard = new HepyProductCard(hepyProduct,
                HepyProductImages.hepyLoad(hepyContext.ppqhStorage(), hepyProduct.hepyImagePath(), HepyProductCard.HEPY_WIDTH, HepyProductCard.HEPY_MEDIA_HEIGHT));
        if (!hepyCard.hepyHasImage() && hepyProduct.hepyLook() != null) {
            HepyProductImages.hepyRealistic(hepyContext.ppqhRecolorService(), hepyProduct.hepyLook(), hepyCard::setImage);
        }
        hepyCard.setOnOpen(hepyP -> hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_PRODUCT, hepyP.hepyId()));
        hepyCard.setOnAddToCart(hepyP -> hepyAddOrChooseSize(hepyContext, hepyP, hepyCard));
        return hepyCard;
    }

    static void hepyAddOrChooseSize(PpqhAppContext hepyContext, HepyProductSummary hepyProduct, Node hepyAnyNode) {
        if (hepyProduct.hepySingleVariantId() == null) {
            hepyContext.ppqhRouter().ppqhGo(PpqhRoutes.PPQH_PRODUCT, hepyProduct.hepyId());
            return;
        }
        PpqhAsync.ppqhRun(() -> hepyContext.ppqhCartService().hepyAdd(hepyProduct.hepySingleVariantId(), 1),
                hepySummary -> {
                    hepyContext.setCartCount(hepySummary.hepyItemCount());
                    PpqhToast.ppqhSuccess(hepyAnyNode, hepyProduct.hepyName() + " se agregó al carrito.");
                },
                hepyE -> PpqhToast.ppqhError(hepyAnyNode, PpqhErrorMessages.ppqhOf(hepyE)));
    }
}
