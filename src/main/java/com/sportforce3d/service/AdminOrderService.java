package com.sportforce3d.service;

import com.sportforce3d.db.Database;
import com.sportforce3d.design.DesignDocument;
import com.sportforce3d.design.DesignElement;
import com.sportforce3d.design.DesignJson;
import com.sportforce3d.design.TechSheet;
import com.sportforce3d.model.DesignAsset;
import com.sportforce3d.model.Material;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.OrderSummary;
import com.sportforce3d.model.Role;
import com.sportforce3d.model.User;
import com.sportforce3d.repository.DesignRepository;
import com.sportforce3d.repository.OrderRepository;
import com.sportforce3d.storage.StorageService;
import com.sportforce3d.util.Validators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AdminOrderService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderService.class);
    public static final int NOTE_MAX = 300;

    private final Database db;
    private final OrderRepository orders;
    private final OrderService orderService;
    private final DesignRepository designs;
    private final CatalogService catalog;
    private final StorageService storage;
    private final Session session;
    private final Clock clock;

    public record OrderDesign(long orderId, String orderNumber, OrderItem item, DesignDocument document,
                              TechSheet sheet, Map<String, Material> materials, List<DesignAsset> assets) {
    }

    public AdminOrderService(Database db, OrderRepository orders, OrderService orderService, DesignRepository designs,
                             CatalogService catalog, StorageService storage, Session session, Clock clock) {
        this.db = db;
        this.orders = orders;
        this.orderService = orderService;
        this.designs = designs;
        this.catalog = catalog;
        this.storage = storage;
        this.session = session;
        this.clock = clock;
    }

    public List<OrderSummary> list(OrderStatus status, String text) {
        session.requireRole(Role.ADMIN);
        return orders.findForAdmin(status, text, 500);
    }

    public Map<OrderStatus, Integer> counts() {
        session.requireRole(Role.ADMIN);
        return orders.countByStatus();
    }

    public OrderDetail order(long orderId) {
        session.requireRole(Role.ADMIN);
        return orders.findDetail(orderId).orElseThrow(() -> new ServiceException("No encontramos ese pedido."));
    }

    public static List<OrderStatus> nextStates(OrderStatus current) {
        return switch (current) {
            case PENDIENTE_PAGO, PAGO_EN_VERIFICACION -> List.of(OrderStatus.CANCELADO);
            case PAGADO -> List.of(OrderStatus.EN_PRODUCCION);
            case EN_PRODUCCION -> List.of(OrderStatus.ENVIADO);
            case ENVIADO -> List.of(OrderStatus.ENTREGADO);
            case ENTREGADO, CANCELADO -> List.of();
        };
    }

    public OrderDetail changeStatus(long orderId, OrderStatus next, String note) {
        User admin = session.requireRole(Role.ADMIN);
        OrderDetail order = order(orderId);
        String cleanNote = Validators.blankToNull(note);
        if (cleanNote != null && cleanNote.length() > NOTE_MAX) {
            throw ValidationException.of("note", "La nota admite hasta " + NOTE_MAX + " caracteres.");
        }
        if (Validators.plainText(cleanNote, true) != null) {
            throw ValidationException.of("note", Validators.plainText(cleanNote, true));
        }
        if (next == OrderStatus.PAGADO) {
            throw new ServiceException("Un pedido pasa a Pagado solo al aprobar su pago en Administración → Pagos.");
        }
        if (!nextStates(order.status()).contains(next)) {
            throw new ServiceException("No se puede pasar de " + order.status().label() + " a " + next.label() + ".");
        }
        if (next == OrderStatus.CANCELADO) {
            if (cleanNote == null) {
                throw ValidationException.of("note", "Escribe el motivo de la cancelación: el cliente lo verá.");
            }
            return orderService.cancel(orderId, cleanNote);
        }
        String now = clock.instant().toString();
        db.inTransaction(c -> {
            if (!orders.updateStatus(c, orderId, order.status(), next)) {
                throw new ServiceException("El pedido cambió de estado. Actualiza la pantalla e inténtalo de nuevo.");
            }
            orders.insertHistory(c, orderId, order.status(), next, admin.id(), cleanNote, now);
            return null;
        });
        log.info("Pedido {}: {} → {} por el ADMIN {}", order.orderNumber(), order.status(), next, admin.id());
        return orders.findDetail(orderId).orElseThrow();
    }

    public OrderDesign design(long orderItemId) {
        session.requireRole(Role.ADMIN);
        OrderRepository.ItemWithOrder found = orders.findItem(orderItemId)
                .filter(i -> i.item().hasDesign())
                .orElseThrow(() -> new ServiceException("Esa línea no tiene un diseño personalizado."));
        DesignDocument doc = DesignJson.read(found.item().designSnapshotJson());
        Map<Long, DesignAsset> assets = new LinkedHashMap<>();
        for (DesignElement e : doc.elements()) {
            if (e.assetId() != null) {
                designs.findAsset(e.assetId()).ifPresent(a -> assets.put(a.asset().id(), a.asset()));
            }
        }
        Map<Long, String> names = new LinkedHashMap<>();
        assets.forEach((id, a) -> names.put(id, a.originalName()));
        Map<String, Material> materials = catalog.materialsByCode();
        return new OrderDesign(found.orderId(), found.orderNumber(), found.item(), doc,
                TechSheet.of(doc, materials, names), materials, List.copyOf(assets.values()));
    }

    public Path exportDesign(long orderItemId, Path parent) {
        OrderDesign d = design(orderItemId);
        Path dir = parent.resolve(d.orderNumber() + "-linea-" + d.item().id());
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("diseno.json"), d.item().designSnapshotJson(), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("ficha-tecnica.csv"),
                    d.sheet().toCsv(d.orderNumber() + " · " + d.item().productName() + " · " + d.item().designName()),
                    StandardCharsets.UTF_8);
            copy(d.item().designFrontPath(), dir.resolve("captura-frente.png"));
            copy(d.item().designBackPath(), dir.resolve("captura-espalda.png"));
            List<String> used = new ArrayList<>();
            for (DesignAsset a : d.assets()) {
                String name = "logo-" + a.id() + "-" + safe(a.originalName());
                copy(a.path(), dir.resolve(name));
                used.add(name);
            }
            log.info("Diseño de {} línea {} exportado a {} ({} logos)", d.orderNumber(), d.item().id(), dir, used.size());
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar los archivos en " + dir, e);
        }
    }

    private void copy(String storedPath, Path target) throws IOException {
        if (storedPath == null) {
            return;
        }
        Path source = storage.resolve(storedPath);
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String safe(String name) {
        String s = name == null ? "logo" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        return s.length() > 60 ? s.substring(s.length() - 60) : s;
    }
}
