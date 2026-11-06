package com.sportforce3d.model;

import java.util.List;

/**
 * Intento de pago de un pedido. Un pedido puede tener varios: si el ADMIN rechaza uno, se abre otro PENDIENTE
 * para que el cliente vuelva a registrarlo. {@code orderNumber} y {@code customerName} sirven para la cola del ADMIN.
 */
public record Payment(long id, long orderId, String orderNumber, String customerName, PaymentMethod method,
                      PaymentStatus status, long amountCents, long orderTotalCents, String operationCode,
                      String verifiedBy, String verifiedAt, String rejectionReason, String createdAt,
                      String updatedAt, List<PaymentProof> proofs) {

    public Payment {
        proofs = List.copyOf(proofs);
    }
}
