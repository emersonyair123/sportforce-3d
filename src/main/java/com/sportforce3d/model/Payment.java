package com.sportforce3d.model;

import java.util.List;

public record Payment(long id, long orderId, String orderNumber, String customerName, PaymentMethod method,
                      PaymentStatus status, long amountCents, long orderTotalCents, String operationCode,
                      String verifiedBy, String verifiedAt, String rejectionReason, String createdAt,
                      String updatedAt, List<PaymentProof> proofs) {

    public Payment {
        proofs = List.copyOf(proofs);
    }
}
