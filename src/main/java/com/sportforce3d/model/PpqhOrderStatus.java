package com.sportforce3d.model;

import java.util.List;

public enum PpqhOrderStatus {
    PENDIENTE_PAGO("Pendiente de pago"),
    PAGO_EN_VERIFICACION("Pago en verificación"),
    PAGADO("Pagado"),
    EN_PRODUCCION("En producción"),
    ENVIADO("Enviado"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    public static final List<PpqhOrderStatus> PPQH_FLOW =
            List.of(PENDIENTE_PAGO, PAGO_EN_VERIFICACION, PAGADO, EN_PRODUCCION, ENVIADO, ENTREGADO);

    private final String ppqhLabel;

    PpqhOrderStatus(String ppqhLabel) {
        this.ppqhLabel = ppqhLabel;
    }

    public String ppqhLabel() {
        return ppqhLabel;
    }
}
