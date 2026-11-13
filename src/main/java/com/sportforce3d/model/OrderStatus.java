package com.sportforce3d.model;

import java.util.List;

public enum OrderStatus {
    PENDIENTE_PAGO("Pendiente de pago"),
    PAGO_EN_VERIFICACION("Pago en verificación"),
    PAGADO("Pagado"),
    EN_PRODUCCION("En producción"),
    ENVIADO("Enviado"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    public static final List<OrderStatus> FLOW =
            List.of(PENDIENTE_PAGO, PAGO_EN_VERIFICACION, PAGADO, EN_PRODUCCION, ENVIADO, ENTREGADO);

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
