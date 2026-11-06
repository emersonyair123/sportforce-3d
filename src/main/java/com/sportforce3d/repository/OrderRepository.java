package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.Address;
import com.sportforce3d.model.OrderDetail;
import com.sportforce3d.model.OrderItem;
import com.sportforce3d.model.OrderStatus;
import com.sportforce3d.model.OrderSummary;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.StatusChange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pedidos, historial de estados, pagos e inventario reservado.
 * Los métodos que reciben {@link Connection} se usan dentro de la transacción que abre OrderService.
 */
public class OrderRepository {

    /** Datos de un pedido nuevo, ya calculados y validados por el service. */
    public record NewOrder(long userId, PaymentMethod method, long subtotalCents, long surchargeCents,
                           long shippingCents, Address address, String notes, String placedAt) {

        public long totalCents() {
            return subtotalCents + surchargeCents + shippingCents;
        }
    }

    /** Línea nueva; las personalizadas llevan la copia del diseño (JSON + capturas ya copiadas a "orders/"). */
    public record NewItem(long productId, long variantId, String productName, String sku, String sizeCode,
                          String colorHex, int quantity, long unitPriceCents, long surchargeCents,
                          Long designId, String designName, Integer designVersion, String designJson,
                          String designFrontPath, String designBackPath) {
    }

    /** Unidades reservadas por un pedido, para liberarlas al cancelar. */
    public record Reservation(long variantId, int quantity) {
    }

    private static final String SUMMARY_SQL = """
            SELECT o.id, o.order_number, o.status, o.payment_method, o.placed_at, o.total_cents,
                   (SELECT COALESCE(sum(quantity), 0) FROM order_items oi WHERE oi.order_id = o.id) AS item_count,
                   u.full_name
            FROM orders o JOIN users u ON u.id = o.user_id
            """;

    private final Database db;

    public OrderRepository(Database db) {
        this.db = db;
    }

    // ------------------------------------------------------------------ Escritura (dentro de transacción)

    /** Correlativo anual atómico: 1, 2, 3… por año. */
    public int nextSequence(Connection c, int year) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO order_counters (year, last_value) VALUES (?, 1)
                ON CONFLICT (year) DO UPDATE SET last_value = last_value + 1
                RETURNING last_value
                """)) {
            ps.setInt(1, year);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public long insertOrder(Connection c, String orderNumber, NewOrder o) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO orders (order_number, user_id, status, payment_method, subtotal_cents, surcharge_cents,
                                    shipping_cents, total_cents, ship_recipient, ship_phone, ship_department,
                                    ship_province, ship_district, ship_line1, ship_reference, notes, placed_at)
                VALUES (?, ?, 'PENDIENTE_PAGO', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            Address a = o.address();
            Jdbc.bind(ps, java.util.Arrays.asList(orderNumber, o.userId(), o.method().name(), o.subtotalCents(),
                    o.surchargeCents(), o.shippingCents(), o.totalCents(), a.recipient(), a.phone(), a.department(),
                    a.province(), a.district(), a.line1(), a.reference(), o.notes(), o.placedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertItem(Connection c, long orderId, NewItem i) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO order_items (order_id, product_id, variant_id, product_name, sku, size_code, color_hex,
                                         quantity, unit_price_cents, surcharge_cents, line_total_cents, design_id,
                                         design_name, design_version, design_snapshot_json, design_front_path,
                                         design_back_path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            Jdbc.bind(ps, java.util.Arrays.asList(orderId, i.productId(), i.variantId(), i.productName(), i.sku(),
                    i.sizeCode(), i.colorHex(), i.quantity(), i.unitPriceCents(), i.surchargeCents(),
                    (i.unitPriceCents() + i.surchargeCents()) * i.quantity(), i.designId(), i.designName(),
                    i.designVersion(), i.designJson(), i.designFrontPath(), i.designBackPath()));
            ps.executeUpdate();
        }
    }

    public void insertHistory(Connection c, long orderId, OrderStatus from, OrderStatus to, Long changedBy,
                              String note, String createdAt) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO order_status_history (order_id, from_status, to_status, changed_by, note, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            Jdbc.bind(ps, java.util.Arrays.asList(orderId, from == null ? null : from.name(), to.name(), changedBy,
                    note, createdAt));
            ps.executeUpdate();
        }
    }

    public void insertPendingPayment(Connection c, long orderId, PaymentMethod method, long amountCents)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payments (order_id, method, status, amount_cents) VALUES (?, ?, 'PENDIENTE', ?)")) {
            ps.setLong(1, orderId);
            ps.setString(2, method.name());
            ps.setLong(3, amountCents);
            ps.executeUpdate();
        }
    }

    /** Reserva unidades si hay disponibles. Devuelve false (sin cambiar nada) si no alcanzan. */
    public boolean reserveStock(Connection c, long variantId, int quantity) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE inventory SET reserved = reserved + ? WHERE variant_id = ? AND quantity - reserved >= ?")) {
            ps.setInt(1, quantity);
            ps.setLong(2, variantId);
            ps.setInt(3, quantity);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Al aprobar el pago, la reserva se vuelve venta: baja la cantidad y la reserva a la vez. Devuelve false (sin
     * cambiar nada) si la reserva no alcanza, lo que indicaría un inventario editado a mano.
     */
    public boolean commitStock(Connection c, long variantId, int quantity) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE inventory SET quantity = quantity - ?, reserved = reserved - ?
                WHERE variant_id = ? AND reserved >= ? AND quantity >= ?
                """)) {
            Jdbc.bind(ps, List.of(quantity, quantity, variantId, quantity, quantity));
            return ps.executeUpdate() == 1;
        }
    }

    public void releaseStock(Connection c, long variantId, int quantity) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE inventory SET reserved = max(reserved - ?, 0) WHERE variant_id = ?")) {
            ps.setInt(1, quantity);
            ps.setLong(2, variantId);
            ps.executeUpdate();
        }
    }

    public List<Reservation> reservations(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT variant_id, quantity FROM order_items WHERE order_id = ? AND variant_id IS NOT NULL")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Reservation(rs.getLong(1), rs.getInt(2)));
                }
                return list;
            }
        }
    }

    /** Cambia el estado solo si sigue siendo {@code expected} (evita carreras). */
    public boolean updateStatus(Connection c, long orderId, OrderStatus expected, OrderStatus next) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE orders SET status = ? WHERE id = ? AND status = ?")) {
            ps.setString(1, next.name());
            ps.setLong(2, orderId);
            ps.setString(3, expected.name());
            return ps.executeUpdate() == 1;
        }
    }

    public void rejectOpenPayments(Connection c, long orderId, String reason) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE payments SET status = 'RECHAZADO', rejection_reason = ?
                WHERE order_id = ? AND status IN ('PENDIENTE', 'EN_VERIFICACION')
                """)) {
            ps.setString(1, reason);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ Lectura

    public List<OrderSummary> findByUser(long userId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SUMMARY_SQL + " WHERE o.user_id = ? ORDER BY o.placed_at DESC, o.id DESC")) {
                ps.setLong(1, userId);
                return summaries(ps);
            }
        });
    }

    /**
     * Pedidos para el ADMIN, del más reciente al más antiguo. {@code text} busca en número, nombre y correo del
     * cliente (sin distinguir mayúsculas); {@code status} null = todos.
     */
    public List<OrderSummary> findForAdmin(OrderStatus status, String text, int limit) {
        return db.withConnection(c -> {
            StringBuilder sql = new StringBuilder(SUMMARY_SQL).append(" WHERE 1 = 1");
            List<Object> params = new ArrayList<>();
            if (status != null) {
                sql.append(" AND o.status = ?");
                params.add(status.name());
            }
            if (text != null && !text.isBlank()) {
                sql.append(" AND (lower(o.order_number) LIKE ? OR lower(u.full_name) LIKE ? OR lower(u.email) LIKE ?)");
                String like = "%" + text.trim().toLowerCase(java.util.Locale.ROOT) + "%";
                params.add(like);
                params.add(like);
                params.add(like);
            }
            sql.append(" ORDER BY o.placed_at DESC, o.id DESC LIMIT ?");
            params.add(limit);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                Jdbc.bind(ps, params);
                return summaries(ps);
            }
        });
    }

    /** Cantidad de pedidos por estado (para los filtros y el dashboard). */
    public java.util.Map<OrderStatus, Integer> countByStatus() {
        return db.withConnection(c -> {
            java.util.Map<OrderStatus, Integer> map = new java.util.EnumMap<>(OrderStatus.class);
            for (OrderStatus s : OrderStatus.values()) {
                map.put(s, 0);
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT status, count(*) FROM orders GROUP BY status");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(OrderStatus.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }
            return map;
        });
    }

    /** Una línea con su pedido (para el visor de uniformes del ADMIN). */
    public record ItemWithOrder(long orderId, String orderNumber, long userId, OrderItem item) {
    }

    public Optional<ItemWithOrder> findItem(long itemId) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT oi.order_id, o.order_number, o.user_id FROM order_items oi JOIN orders o ON o.id = oi.order_id
                    WHERE oi.id = ?
                    """)) {
                ps.setLong(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    long orderId = rs.getLong(1);
                    String number = rs.getString(2);
                    long userId = rs.getLong(3);
                    return items(c, orderId).stream().filter(i -> i.id() == itemId).findFirst()
                            .map(i -> new ItemWithOrder(orderId, number, userId, i));
                }
            }
        });
    }

    public Optional<OrderDetail> findDetail(long orderId) {
        return db.withConnection(c -> findDetail(c, orderId));
    }

    public Optional<OrderDetail> findDetail(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT o.*, u.full_name, u.email FROM orders o JOIN users u ON u.id = o.user_id WHERE o.id = ?
                """)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new OrderDetail(
                        rs.getLong("id"), rs.getString("order_number"), rs.getLong("user_id"),
                        rs.getString("full_name"), rs.getString("email"),
                        OrderStatus.valueOf(rs.getString("status")),
                        PaymentMethod.valueOf(rs.getString("payment_method")),
                        rs.getLong("subtotal_cents"), rs.getLong("surcharge_cents"), rs.getLong("shipping_cents"),
                        rs.getLong("total_cents"), rs.getString("ship_recipient"), rs.getString("ship_phone"),
                        rs.getString("ship_department"), rs.getString("ship_province"), rs.getString("ship_district"),
                        rs.getString("ship_line1"), rs.getString("ship_reference"), rs.getString("notes"),
                        rs.getString("placed_at"), items(c, orderId), history(c, orderId),
                        PaymentRepository.forOrder(c, orderId)));
            }
        }
    }

    private static List<OrderItem> items(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM order_items WHERE order_id = ? ORDER BY id")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderItem> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new OrderItem(rs.getLong("id"), rs.getLong("product_id"), rs.getString("product_name"),
                            rs.getString("sku"), rs.getString("size_code"), rs.getString("color_hex"),
                            rs.getInt("quantity"), rs.getLong("unit_price_cents"), rs.getLong("surcharge_cents"),
                            rs.getLong("line_total_cents"), Jdbc.getLongOrNull(rs, "design_id"),
                            rs.getString("design_name"), (Integer) rs.getObject("design_version"),
                            rs.getString("design_snapshot_json"), rs.getString("design_front_path"),
                            rs.getString("design_back_path")));
                }
                return list;
            }
        }
    }

    private static List<StatusChange> history(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT h.from_status, h.to_status, u.full_name, h.note, h.created_at
                FROM order_status_history h LEFT JOIN users u ON u.id = h.changed_by
                WHERE h.order_id = ? ORDER BY h.created_at, h.id
                """)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<StatusChange> list = new ArrayList<>();
                while (rs.next()) {
                    String from = rs.getString(1);
                    list.add(new StatusChange(from == null ? null : OrderStatus.valueOf(from),
                            OrderStatus.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
                return list;
            }
        }
    }

    private static List<OrderSummary> summaries(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<OrderSummary> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new OrderSummary(rs.getLong("id"), rs.getString("order_number"),
                        OrderStatus.valueOf(rs.getString("status")),
                        PaymentMethod.valueOf(rs.getString("payment_method")), rs.getString("placed_at"),
                        rs.getLong("total_cents"), rs.getInt("item_count"), rs.getString("full_name")));
            }
            return list;
        }
    }
}
