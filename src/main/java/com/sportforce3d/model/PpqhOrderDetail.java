package com.sportforce3d.model;

import java.util.List;

public record PpqhOrderDetail(long ppqhId, String ppqhOrderNumber, long ppqhUserId, String ppqhCustomerName, String ppqhCustomerEmail,
                          PpqhOrderStatus ppqhStatus, PpqhPaymentMethod ppqhPaymentMethod,
                          long ppqhSubtotalCents, long ppqhSurchargeCents, long ppqhShippingCents, long ppqhTotalCents,
                          String ppqhShipRecipient, String ppqhShipPhone, String ppqhShipDepartment, String ppqhShipProvince,
                          String ppqhShipDistrict, String ppqhShipLine1, String ppqhShipReference, String ppqhNotes, String ppqhPlacedAt,
                          List<PpqhOrderItem> ppqhItems, List<PpqhStatusChange> ppqhHistory, List<PpqhPayment> ppqhPayments) {

    public PpqhOrderDetail {
        ppqhItems = List.copyOf(ppqhItems);
        ppqhHistory = List.copyOf(ppqhHistory);
        ppqhPayments = List.copyOf(ppqhPayments);
    }

    public PpqhPayment ppqhCurrentPayment() {
        return ppqhPayments.isEmpty() ? null : ppqhPayments.get(ppqhPayments.size() - 1);
    }

    public boolean ppqhHasDesigns() {
        return ppqhItems.stream().anyMatch(PpqhOrderItem::ppqhHasDesign);
    }

    public String ppqhShippingOneLine() {
        return ppqhShipLine1 + ", " + ppqhShipDistrict + ", " + ppqhShipProvince + ", " + ppqhShipDepartment;
    }

    public PpqhOrderStatus ppqhLastStatusBeforeCancel() {
        PpqhOrderStatus ppqhLast = PpqhOrderStatus.PENDIENTE_PAGO;
        for (PpqhStatusChange ppqhChange : ppqhHistory) {
            if (ppqhChange.ppqhTo() != PpqhOrderStatus.CANCELADO) {
                ppqhLast = ppqhChange.ppqhTo();
            }
        }
        return ppqhLast;
    }

    public int ppqhItemCount() {
        return ppqhItems.stream().mapToInt(PpqhOrderItem::ppqhQuantity).sum();
    }
}
