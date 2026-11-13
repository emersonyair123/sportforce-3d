package com.sportforce3d.model;

public record PpqhOrderSummary(long ppqhId, String ppqhOrderNumber, PpqhOrderStatus ppqhStatus, PpqhPaymentMethod ppqhPaymentMethod,
                           String ppqhPlacedAt, long ppqhTotalCents, int ppqhItemCount, String ppqhCustomerName) {
}
