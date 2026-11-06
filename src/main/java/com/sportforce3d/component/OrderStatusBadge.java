package com.sportforce3d.component;

import com.sportforce3d.model.OrderStatus;
import org.kordamp.ikonli.feather.Feather;

/** Badge del estado de un pedido con texto e icono (no solo color). */
public final class OrderStatusBadge {

    private OrderStatusBadge() {
    }

    public static SfBadge of(OrderStatus status) {
        return switch (status) {
            case PENDIENTE_PAGO -> new SfBadge(status.label()).withIcon(Feather.CLOCK);
            case PAGO_EN_VERIFICACION -> new SfBadge(status.label(), SfBadge.Kind.WARNING).withIcon(Feather.CLOCK);
            case PAGADO -> new SfBadge(status.label(), SfBadge.Kind.SUCCESS).withIcon(Feather.CHECK);
            case EN_PRODUCCION -> new SfBadge(status.label(), SfBadge.Kind.NEUTRAL).withIcon(Feather.TOOL);
            case ENVIADO -> new SfBadge(status.label(), SfBadge.Kind.NEUTRAL).withIcon(Feather.TRUCK);
            case ENTREGADO -> new SfBadge(status.label(), SfBadge.Kind.SUCCESS).withIcon(Feather.PACKAGE);
            case CANCELADO -> new SfBadge(status.label(), SfBadge.Kind.DANGER).withIcon(Feather.X);
        };
    }
}
