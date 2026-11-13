package com.sportforce3d.repository;

import com.sportforce3d.db.Database;
import com.sportforce3d.model.Payment;
import com.sportforce3d.model.PaymentInstructions;
import com.sportforce3d.model.PaymentMethod;
import com.sportforce3d.model.PaymentProof;
import com.sportforce3d.model.PaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepository {

    private static final String SELECT = """
            SELECT p.*, o.order_number, o.total_cents AS order_total, u.full_name AS customer_name,
                   vu.full_name AS verifier_name
            FROM payments p
            JOIN orders o ON o.id = p.order_id
            JOIN users u ON u.id = o.user_id
            LEFT JOIN users vu ON vu.id = p.verified_by
            """;

    private final Database db;

    public PaymentRepository(Database db) {
        this.db = db;
    }

    public static List<Payment> forOrder(Connection c, long orderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.order_id = ? ORDER BY p.id")) {
            ps.setLong(1, orderId);
            return list(c, ps);
        }
    }

    public Optional<Payment> find(long paymentId) {
        return db.withConnection(c -> find(c, paymentId));
    }

    public Optional<Payment> find(Connection c, long paymentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.id = ?")) {
            ps.setLong(1, paymentId);
            List<Payment> l = list(c, ps);
            return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
        }
    }

    public List<Payment> findByStatus(PaymentStatus status, int limit) {
        String order = status == PaymentStatus.EN_VERIFICACION ? "p.updated_at, p.id" : "p.updated_at DESC, p.id DESC";
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement(SELECT + " WHERE p.status = ? ORDER BY " + order + " LIMIT ?")) {
                ps.setString(1, status.name());
                ps.setInt(2, limit);
                return list(c, ps);
            }
        });
    }

    public int countByStatus(PaymentStatus status) {
        return db.withConnection(c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM payments WHERE status = ?")) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    public boolean operationCodeInUse(Connection c, PaymentMethod method, String code) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT 1 FROM payments WHERE method = ? AND operation_code = ? AND status IN ('EN_VERIFICACION', 'APROBADO')
                """)) {
            ps.setString(1, method.name());
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public PaymentInstructions instructions() {
        return db.withConnection(SettingsRepository::paymentInstructions);
    }

    public boolean submit(Connection c, long paymentId, String operationCode, String now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE payments SET status = 'EN_VERIFICACION', operation_code = ?, updated_at = ?
                WHERE id = ? AND status = 'PENDIENTE'
                """)) {
            Jdbc.bind(ps, List.of(operationCode, now, paymentId));
            return ps.executeUpdate() == 1;
        }
    }

    public void insertProof(Connection c, long paymentId, String path, String mimeType, long sizeBytes, String now)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO payment_proofs (payment_id, stored_path, mime_type, size_bytes, created_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            Jdbc.bind(ps, List.of(paymentId, path, mimeType, sizeBytes, now));
            ps.executeUpdate();
        }
    }

    public boolean approve(Connection c, long paymentId, long adminId, String now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE payments SET status = 'APROBADO', verified_by = ?, verified_at = ?, updated_at = ?
                WHERE id = ? AND status = 'EN_VERIFICACION'
                """)) {
            Jdbc.bind(ps, List.of(adminId, now, now, paymentId));
            return ps.executeUpdate() == 1;
        }
    }

    public boolean reject(Connection c, long paymentId, long adminId, String reason, String now) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("""
                UPDATE payments SET status = 'RECHAZADO', verified_by = ?, verified_at = ?, rejection_reason = ?,
                                    updated_at = ?
                WHERE id = ? AND status = 'EN_VERIFICACION'
                """)) {
            Jdbc.bind(ps, List.of(adminId, now, reason, now, paymentId));
            return ps.executeUpdate() == 1;
        }
    }

    private static List<Payment> list(Connection c, PreparedStatement ps) throws SQLException {
        List<Payment> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                out.add(new Payment(id, rs.getLong("order_id"), rs.getString("order_number"),
                        rs.getString("customer_name"), PaymentMethod.valueOf(rs.getString("method")),
                        PaymentStatus.valueOf(rs.getString("status")), rs.getLong("amount_cents"),
                        rs.getLong("order_total"), rs.getString("operation_code"), rs.getString("verifier_name"),
                        rs.getString("verified_at"), rs.getString("rejection_reason"), rs.getString("created_at"),
                        rs.getString("updated_at"), List.of()));
            }
        }
        List<Payment> withProofs = new ArrayList<>(out.size());
        for (Payment p : out) {
            withProofs.add(new Payment(p.id(), p.orderId(), p.orderNumber(), p.customerName(), p.method(), p.status(),
                    p.amountCents(), p.orderTotalCents(), p.operationCode(), p.verifiedBy(), p.verifiedAt(),
                    p.rejectionReason(), p.createdAt(), p.updatedAt(), proofs(c, p.id())));
        }
        return withProofs;
    }

    private static List<PaymentProof> proofs(Connection c, long paymentId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT id, stored_path, mime_type, size_bytes, created_at FROM payment_proofs WHERE payment_id = ? ORDER BY id")) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<PaymentProof> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new PaymentProof(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4),
                            rs.getString(5)));
                }
                return list;
            }
        }
    }
}
