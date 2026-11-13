package com.sportforce3d.model;

import java.util.List;

public record PpqhPayment(long ppqhId, long ppqhOrderId, String ppqhOrderNumber, String ppqhCustomerName, PpqhPaymentMethod ppqhMethod,
                      PpqhPaymentStatus ppqhStatus, long ppqhAmountCents, long ppqhOrderTotalCents, String ppqhOperationCode,
                      String ppqhVerifiedBy, String ppqhVerifiedAt, String ppqhRejectionReason, String ppqhCreatedAt,
                      String ppqhUpdatedAt, List<PpqhPaymentProof> ppqhProofs) {

    public PpqhPayment {
        ppqhProofs = List.copyOf(ppqhProofs);
    }
}
