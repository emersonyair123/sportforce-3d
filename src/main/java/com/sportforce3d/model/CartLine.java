package com.sportforce3d.model;

public record CartLine(long itemId, long productId, long variantId, String productName, String sku,
                       Garment garment, String sizeName, String colorName, String colorHex, String imagePath,
                       long unitPriceCents, long surchargeCents, int quantity, int available, boolean purchasable,
                       Long designId, String designName, String designThumbnail) {

    public long lineTotalCents() {
        return (unitPriceCents + surchargeCents) * quantity;
    }

    public boolean enoughStock() {
        return quantity <= available;
    }

    public boolean hasDesign() {
        return designId != null;
    }

    public CartLine withSurcharge(long cents) {
        return new CartLine(itemId, productId, variantId, productName, sku, garment, sizeName, colorName, colorHex,
                imagePath, unitPriceCents, cents, quantity, available, purchasable, designId, designName,
                designThumbnail);
    }
}
