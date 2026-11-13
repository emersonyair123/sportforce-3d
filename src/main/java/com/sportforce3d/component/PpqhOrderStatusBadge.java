package com.sportforce3d.component;

import com.sportforce3d.model.PpqhOrderStatus;
import org.kordamp.ikonli.feather.Feather;

public final class PpqhOrderStatusBadge {

    private PpqhOrderStatusBadge() {
    }

    public static PpqhSfBadge ppqhOf(PpqhOrderStatus ppqhStatus) {
        return switch (ppqhStatus) {
            case PENDIENTE_PAGO -> new PpqhSfBadge(ppqhStatus.ppqhLabel()).ppqhWithIcon(Feather.CLOCK);
            case PAGO_EN_VERIFICACION -> new PpqhSfBadge(ppqhStatus.ppqhLabel(), PpqhSfBadge.PpqhKind.WARNING).ppqhWithIcon(Feather.CLOCK);
            case PAGADO -> new PpqhSfBadge(ppqhStatus.ppqhLabel(), PpqhSfBadge.PpqhKind.SUCCESS).ppqhWithIcon(Feather.CHECK);
            case EN_PRODUCCION -> new PpqhSfBadge(ppqhStatus.ppqhLabel(), PpqhSfBadge.PpqhKind.NEUTRAL).ppqhWithIcon(Feather.TOOL);
            case ENVIADO -> new PpqhSfBadge(ppqhStatus.ppqhLabel(), PpqhSfBadge.PpqhKind.NEUTRAL).ppqhWithIcon(Feather.TRUCK);
            case ENTREGADO -> new PpqhSfBadge(ppqhStatus.ppqhLabel(), PpqhSfBadge.PpqhKind.SUCCESS).ppqhWithIcon(Feather.PACKAGE);
            case CANCELADO -> new PpqhSfBadge(ppqhStatus.ppqhLabel(), PpqhSfBadge.PpqhKind.DANGER).ppqhWithIcon(Feather.X);
        };
    }
}
