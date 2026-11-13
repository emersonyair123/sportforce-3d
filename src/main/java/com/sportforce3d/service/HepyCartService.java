package com.sportforce3d.service;

import com.sportforce3d.design.PgeyDesignDocument;
import com.sportforce3d.design.PgeyDesignJson;
import com.sportforce3d.design.PgeyDesignPricing;
import com.sportforce3d.design.PgeyMaterialSlot;
import com.sportforce3d.model.HepyCartLine;
import com.sportforce3d.model.HepyCartSummary;
import com.sportforce3d.model.HepyGarment;
import com.sportforce3d.model.HepyMaterial;
import com.sportforce3d.model.HepyProductDetail;
import com.sportforce3d.model.PpqhUser;
import com.sportforce3d.repository.HepyCartRepository;
import com.sportforce3d.repository.HepyDesignRepository;
import com.sportforce3d.repository.HepyDesignRepository.HepyStoredDesign;
import com.sportforce3d.repository.HepyProductRepository;
import com.sportforce3d.repository.HepyProductRepository.HepyVariantInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HepyCartService {

    public static final int HEPY_MAX_PER_LINE = 99;

    public static final Set<HepyGarment> HEPY_DESIGNABLE = HepyGarment.HEPY_DESIGNABLE;

    private final HepyCartRepository hepyCarts;
    private final HepyProductRepository hepyProducts;
    private final HepyCatalogService hepyCatalog;
    private final HepyDesignRepository hepyDesigns;
    private final PpqhSession hepySession;

    public HepyCartService(HepyCartRepository hepyCarts, HepyProductRepository hepyProducts, HepyCatalogService hepyCatalog,
                       HepyDesignRepository hepyDesigns, PpqhSession hepySession) {
        this.hepyCarts = hepyCarts;
        this.hepyProducts = hepyProducts;
        this.hepyCatalog = hepyCatalog;
        this.hepyDesigns = hepyDesigns;
        this.hepySession = hepySession;
    }

    public HepyCartSummary hepySummary() {
        PpqhUser hepyMe = hepySession.ppqhRequireUser();
        return hepySummarize(hepyRefreshDesignSurcharges(hepyMe.ppqhId(), hepyCarts.hepyFindLines(hepyMe.ppqhId())));
    }

    HepyCartSummary hepySummarize(List<HepyCartLine> hepyLines) {
        long hepySubtotal = hepyLines.stream().mapToLong(hepyL -> hepyL.hepyUnitPriceCents() * hepyL.hepyQuantity()).sum();
        long hepySurcharge = hepyLines.stream().mapToLong(hepyL -> hepyL.hepySurchargeCents() * hepyL.hepyQuantity()).sum();
        return new HepyCartSummary(hepyLines, hepySubtotal, hepySurcharge, hepyCatalog.hepyShippingFor(hepySubtotal + hepySurcharge),
                hepyCatalog.hepyFreeShippingFromCents());
    }

    public int hepyCountUnits() {
        return hepySession.ppqhCurrent().map(hepyU -> hepyCarts.hepyCountUnits(hepyU.ppqhId())).orElse(0);
    }

    public HepyCartSummary hepyAdd(long hepyVariantId, int hepyQuantity) {
        PpqhUser hepyMe = hepySession.ppqhRequireUser();
        hepyCheckQuantity(hepyQuantity);
        HepyVariantInfo hepyVariant = hepyProducts.hepyFindVariantInfo(hepyVariantId)
                .filter(HepyVariantInfo::hepyPurchasable)
                .orElseThrow(() -> new PpqhServiceException("Este producto ya no está disponible."));

        var hepyExisting = hepyCarts.hepyFindStandardItem(hepyMe.ppqhId(), hepyVariantId);
        int hepyTotal = hepyQuantity + hepyExisting.map(HepyCartRepository.HepyExistingItem::hepyQuantity).orElse(0);
        if (hepyTotal > HEPY_MAX_PER_LINE) {
            throw new PpqhServiceException("Puedes llevar hasta " + HEPY_MAX_PER_LINE + " unidades de un mismo producto.");
        }
        hepyCheckStock(hepyVariant.hepyProductName() + " (talla " + hepyVariant.hepySizeName() + ")", hepyTotal, hepyVariant.hepyAvailable());

        if (hepyExisting.isPresent()) {
            hepyCarts.hepyUpdateQuantity(hepyMe.ppqhId(), hepyExisting.get().hepyItemId(), hepyTotal, hepyVariant.hepyUnitPriceCents());
        } else {
            hepyCarts.hepyAddItem(hepyMe.ppqhId(), hepyVariantId, hepyQuantity, hepyVariant.hepyUnitPriceCents());
        }
        return hepySummary();
    }

    public HepyCartSummary hepyAddDesign(long hepyDesignId, long hepyVariantId, int hepyQuantity) {
        PpqhUser hepyMe = hepySession.ppqhRequireUser();
        hepyCheckQuantity(hepyQuantity);
        HepyStoredDesign hepyDesign = hepyDesigns.hepyFind(hepyDesignId)
                .filter(hepyD -> hepyD.hepyUserId() == hepyMe.ppqhId() && !hepyD.hepyDeleted())
                .orElseThrow(() -> new PpqhServiceException("No encontramos ese diseño."));
        HepyVariantInfo hepyVariant = hepyProducts.hepyFindVariantInfo(hepyVariantId)
                .filter(HepyVariantInfo::hepyPurchasable)
                .orElseThrow(() -> new PpqhServiceException("Este producto ya no está disponible."));
        HepyProductDetail hepyProduct = hepyCatalog.hepyDetail(hepyVariant.hepyProductId());
        if (!hepyProduct.hepyCustomizable() || !HEPY_DESIGNABLE.contains(hepyProduct.hepyGarment())) {
            throw new PpqhServiceException(hepyProduct.hepyName() + " no se puede personalizar en el diseñador 3D.");
        }
        long hepySurcharge = hepySurchargeFor(PgeyDesignJson.pgeyRead(hepyDesign.hepyDocumentJson()), hepyProduct);

        var hepyExisting = hepyCarts.hepyFindDesignItem(hepyMe.ppqhId(), hepyVariantId, hepyDesignId);
        int hepyTotal = hepyQuantity + hepyExisting.map(HepyCartRepository.HepyExistingItem::hepyQuantity).orElse(0);
        if (hepyTotal > HEPY_MAX_PER_LINE) {
            throw new PpqhServiceException("Puedes llevar hasta " + HEPY_MAX_PER_LINE + " unidades de un mismo diseño y talla.");
        }
        hepyCheckStock(hepyVariant.hepyProductName() + " (talla " + hepyVariant.hepySizeName() + ")", hepyTotal, hepyVariant.hepyAvailable());
        if (hepyExisting.isPresent()) {
            hepyCarts.hepyUpdateQuantity(hepyMe.ppqhId(), hepyExisting.get().hepyItemId(), hepyTotal, hepyVariant.hepyUnitPriceCents());
            hepyCarts.hepyUpdateSurcharge(hepyMe.ppqhId(), hepyExisting.get().hepyItemId(), hepySurcharge);
        } else {
            hepyCarts.hepyAddDesignItem(hepyMe.ppqhId(), hepyVariantId, hepyDesignId, hepyQuantity, hepyVariant.hepyUnitPriceCents(), hepySurcharge);
        }
        return hepySummary();
    }

    long hepySurchargeFor(PgeyDesignDocument hepyDoc, HepyProductDetail hepyProduct) {
        Set<String> hepyAllowed = hepyProduct.hepyMaterials().stream().map(HepyMaterial::hepyCode).collect(Collectors.toSet());
        Map<String, HepyMaterial> hepyAll = hepyCatalog.hepyMaterialsByCode();
        for (PgeyMaterialSlot hepySlot : PgeyDesignPricing.pgeySlotsFor(hepyProduct.hepyGarment())) {
            String hepyCode = hepyDoc.pgeyMaterial(hepySlot);
            if (!hepyAllowed.contains(hepyCode)) {
                HepyMaterial hepyM = hepyAll.get(hepyCode);
                throw new PpqhServiceException("El material " + (hepyM == null ? hepyCode : hepyM.hepyName()) + " (" + hepySlot.pgeyLabel().toLowerCase()
                        + ") no está disponible para " + hepyProduct.hepyName() + ". Cámbialo en el diseñador.");
            }
        }
        return PgeyDesignPricing.pgeySurchargeCents(hepyDoc, hepyAll, hepyProduct.hepyGarment());
    }

    List<HepyCartLine> hepyRefreshDesignSurcharges(long hepyUserId, List<HepyCartLine> hepyLines) {
        if (hepyLines.stream().noneMatch(HepyCartLine::hepyHasDesign)) {
            return hepyLines;
        }
        Map<String, HepyMaterial> hepyAll = hepyCatalog.hepyMaterialsByCode();
        List<HepyCartLine> hepyOut = new ArrayList<>(hepyLines.size());
        for (HepyCartLine hepyLine : hepyLines) {
            if (!hepyLine.hepyHasDesign() || !hepyLine.hepyPurchasable()) {
                hepyOut.add(hepyLine);
                continue;
            }
            long hepyCurrent = hepyDesigns.hepyFind(hepyLine.hepyDesignId())
                    .map(hepyD -> PgeyDesignPricing.pgeySurchargeCents(PgeyDesignJson.pgeyRead(hepyD.hepyDocumentJson()), hepyAll, hepyLine.hepyGarment()))
                    .orElse(hepyLine.hepySurchargeCents());
            if (hepyCurrent != hepyLine.hepySurchargeCents()) {
                hepyCarts.hepyUpdateSurcharge(hepyUserId, hepyLine.hepyItemId(), hepyCurrent);
                hepyOut.add(hepyLine.hepyWithSurcharge(hepyCurrent));
            } else {
                hepyOut.add(hepyLine);
            }
        }
        return hepyOut;
    }

    public HepyCartSummary hepyUpdateQuantity(long hepyItemId, int hepyQuantity) {
        PpqhUser hepyMe = hepySession.ppqhRequireUser();
        hepyCheckQuantity(hepyQuantity);
        HepyCartLine hepyLine = hepyCarts.hepyFindLines(hepyMe.ppqhId()).stream()
                .filter(hepyL -> hepyL.hepyItemId() == hepyItemId)
                .findFirst()
                .orElseThrow(() -> new PpqhServiceException("Ese producto ya no está en tu carrito."));
        if (!hepyLine.hepyPurchasable()) {
            throw new PpqhServiceException(hepyLine.hepyProductName() + " ya no está disponible. Quítalo del carrito.");
        }
        hepyCheckStock(hepyLine.hepyProductName() + " (talla " + hepyLine.hepySizeName() + ")", hepyQuantity, hepyLine.hepyAvailable());
        hepyCarts.hepyUpdateQuantity(hepyMe.ppqhId(), hepyItemId, hepyQuantity, hepyLine.hepyUnitPriceCents());
        return hepySummary();
    }

    public HepyCartSummary hepyRemove(long hepyItemId) {
        PpqhUser hepyMe = hepySession.ppqhRequireUser();
        hepyCarts.hepyRemoveItem(hepyMe.ppqhId(), hepyItemId);
        return hepySummary();
    }

    private static void hepyCheckQuantity(int hepyQuantity) {
        if (hepyQuantity < 1 || hepyQuantity > HEPY_MAX_PER_LINE) {
            throw PpqhValidationException.ppqhOf("quantity", "La cantidad debe estar entre 1 y " + HEPY_MAX_PER_LINE + ".");
        }
    }

    static void hepyCheckStock(String hepyWhat, int hepyWanted, int hepyAvailable) {
        if (hepyAvailable <= 0) {
            throw new PpqhServiceException(hepyWhat + " está agotado.");
        }
        if (hepyWanted > hepyAvailable) {
            throw new PpqhServiceException("Solo " + (hepyAvailable == 1 ? "queda 1 unidad" : "quedan " + hepyAvailable + " unidades")
                    + " de " + hepyWhat + ".");
        }
    }
}
