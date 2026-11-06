package com.sportforce3d.model;

public record OrderSummary(long id, String orderNumber, OrderStatus status, PaymentMethod paymentMethod,
                           String placedAt, long totalCents, int itemCount, String customerName) {
}
