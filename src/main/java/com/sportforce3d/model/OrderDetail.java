package com.sportforce3d.model;

import java.util.List;

public record OrderDetail(long id, String orderNumber, long userId, String customerName, String customerEmail,
                          OrderStatus status, PaymentMethod paymentMethod,
                          long subtotalCents, long surchargeCents, long shippingCents, long totalCents,
                          String shipRecipient, String shipPhone, String shipDepartment, String shipProvince,
                          String shipDistrict, String shipLine1, String shipReference, String notes, String placedAt,
                          List<OrderItem> items, List<StatusChange> history) {

    public OrderDetail {
        items = List.copyOf(items);
        history = List.copyOf(history);
    }

    public String shippingOneLine() {
        return shipLine1 + ", " + shipDistrict + ", " + shipProvince + ", " + shipDepartment;
    }

    /** Último estado alcanzado antes de cancelar (para la línea de tiempo). */
    public OrderStatus lastStatusBeforeCancel() {
        OrderStatus last = OrderStatus.PENDIENTE_PAGO;
        for (StatusChange change : history) {
            if (change.to() != OrderStatus.CANCELADO) {
                last = change.to();
            }
        }
        return last;
    }

    public int itemCount() {
        return items.stream().mapToInt(OrderItem::quantity).sum();
    }
}
