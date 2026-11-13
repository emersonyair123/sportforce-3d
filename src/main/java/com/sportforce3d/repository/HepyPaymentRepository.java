package com.sportforce3d.repository;

import com.sportforce3d.db.HepyDatabase;
import com.sportforce3d.model.PpqhPayment;
import com.sportforce3d.model.PpqhPaymentInstructions;
import com.sportforce3d.model.PpqhPaymentMethod;
import com.sportforce3d.model.PpqhPaymentProof;
import com.sportforce3d.model.PpqhPaymentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HepyPaymentRepository {

    private static final String HEPY_SELECT = """
            SELECT p.*, o.order_number, o.total_cents AS order_total, u.full_name AS customer_name,
                   vu.full_name AS verifier_name
            FROM payments p
            JOIN orders o ON o.id = p.order_id
            JOIN users u ON u.id = o.user_id
            LEFT JOIN users vu ON vu.id = p.verified_by
            """;

    private final HepyDatabase hepyDb;

    public HepyPaymentRepository(HepyDatabase hepyDb) {
        this.hepyDb = hepyDb;
    }

    public static List<PpqhPayment> hepyForOrder(Connection hepyC, long hepyOrderId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT + " WHERE p.order_id = ? ORDER BY p.id")) {
            hepyPs.setLong(1, hepyOrderId);
            return hepyList(hepyC, hepyPs);
        }
    }

    public Optional<PpqhPayment> hepyFind(long hepyPaymentId) {
        return hepyDb.hepyWithConnection(hepyC -> hepyFind(hepyC, hepyPaymentId));
    }

    public Optional<PpqhPayment> hepyFind(Connection hepyC, long hepyPaymentId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT + " WHERE p.id = ?")) {
            hepyPs.setLong(1, hepyPaymentId);
            List<PpqhPayment> hepyL = hepyList(hepyC, hepyPs);
            return hepyL.isEmpty() ? Optional.empty() : Optional.of(hepyL.get(0));
        }
    }

    public List<PpqhPayment> hepyFindByStatus(PpqhPaymentStatus hepyStatus, int hepyLimit) {
        String hepyOrder = hepyStatus == PpqhPaymentStatus.EN_VERIFICACION ? "p.updated_at, p.id" : "p.updated_at DESC, p.id DESC";
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement(HEPY_SELECT + " WHERE p.status = ? ORDER BY " + hepyOrder + " LIMIT ?")) {
                hepyPs.setString(1, hepyStatus.name());
                hepyPs.setInt(2, hepyLimit);
                return hepyList(hepyC, hepyPs);
            }
        });
    }

    public int hepyCountByStatus(PpqhPaymentStatus hepyStatus) {
        return hepyDb.hepyWithConnection(hepyC -> {
            try (PreparedStatement hepyPs = hepyC.prepareStatement("SELECT count(*) FROM payments WHERE status = ?")) {
                hepyPs.setString(1, hepyStatus.name());
                try (ResultSet hepyRs = hepyPs.executeQuery()) {
                    hepyRs.next();
                    return hepyRs.getInt(1);
                }
            }
        });
    }

    public boolean hepyOperationCodeInUse(Connection hepyC, PpqhPaymentMethod hepyMethod, String hepyCode) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                SELECT 1 FROM payments WHERE method = ? AND operation_code = ? AND status IN ('EN_VERIFICACION', 'APROBADO')
                """)) {
            hepyPs.setString(1, hepyMethod.name());
            hepyPs.setString(2, hepyCode);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                return hepyRs.next();
            }
        }
    }

    public PpqhPaymentInstructions hepyInstructions() {
        return hepyDb.hepyWithConnection(HepySettingsRepository::hepyPaymentInstructions);
    }

    public boolean hepySubmit(Connection hepyC, long hepyPaymentId, String hepyOperationCode, String hepyNow) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                UPDATE payments SET status = 'EN_VERIFICACION', operation_code = ?, updated_at = ?
                WHERE id = ? AND status = 'PENDIENTE'
                """)) {
            HepyJdbc.hepyBind(hepyPs, List.of(hepyOperationCode, hepyNow, hepyPaymentId));
            return hepyPs.executeUpdate() == 1;
        }
    }

    public void hepyInsertProof(Connection hepyC, long hepyPaymentId, String hepyPath, String hepyMimeType, long hepySizeBytes, String hepyNow)
            throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                INSERT INTO payment_proofs (payment_id, stored_path, mime_type, size_bytes, created_at)
                VALUES (?, ?, ?, ?, ?)
                """)) {
            HepyJdbc.hepyBind(hepyPs, List.of(hepyPaymentId, hepyPath, hepyMimeType, hepySizeBytes, hepyNow));
            hepyPs.executeUpdate();
        }
    }

    public boolean hepyApprove(Connection hepyC, long hepyPaymentId, long hepyAdminId, String hepyNow) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                UPDATE payments SET status = 'APROBADO', verified_by = ?, verified_at = ?, updated_at = ?
                WHERE id = ? AND status = 'EN_VERIFICACION'
                """)) {
            HepyJdbc.hepyBind(hepyPs, List.of(hepyAdminId, hepyNow, hepyNow, hepyPaymentId));
            return hepyPs.executeUpdate() == 1;
        }
    }

    public boolean hepyReject(Connection hepyC, long hepyPaymentId, long hepyAdminId, String hepyReason, String hepyNow) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement("""
                UPDATE payments SET status = 'RECHAZADO', verified_by = ?, verified_at = ?, rejection_reason = ?,
                                    updated_at = ?
                WHERE id = ? AND status = 'EN_VERIFICACION'
                """)) {
            HepyJdbc.hepyBind(hepyPs, List.of(hepyAdminId, hepyNow, hepyReason, hepyNow, hepyPaymentId));
            return hepyPs.executeUpdate() == 1;
        }
    }

    private static List<PpqhPayment> hepyList(Connection hepyC, PreparedStatement hepyPs) throws SQLException {
        List<PpqhPayment> hepyOut = new ArrayList<>();
        try (ResultSet hepyRs = hepyPs.executeQuery()) {
            while (hepyRs.next()) {
                long hepyId = hepyRs.getLong("id");
                hepyOut.add(new PpqhPayment(hepyId, hepyRs.getLong("order_id"), hepyRs.getString("order_number"),
                        hepyRs.getString("customer_name"), PpqhPaymentMethod.valueOf(hepyRs.getString("method")),
                        PpqhPaymentStatus.valueOf(hepyRs.getString("status")), hepyRs.getLong("amount_cents"),
                        hepyRs.getLong("order_total"), hepyRs.getString("operation_code"), hepyRs.getString("verifier_name"),
                        hepyRs.getString("verified_at"), hepyRs.getString("rejection_reason"), hepyRs.getString("created_at"),
                        hepyRs.getString("updated_at"), List.of()));
            }
        }
        List<PpqhPayment> hepyWithProofs = new ArrayList<>(hepyOut.size());
        for (PpqhPayment hepyP : hepyOut) {
            hepyWithProofs.add(new PpqhPayment(hepyP.ppqhId(), hepyP.ppqhOrderId(), hepyP.ppqhOrderNumber(), hepyP.ppqhCustomerName(), hepyP.ppqhMethod(), hepyP.ppqhStatus(),
                    hepyP.ppqhAmountCents(), hepyP.ppqhOrderTotalCents(), hepyP.ppqhOperationCode(), hepyP.ppqhVerifiedBy(), hepyP.ppqhVerifiedAt(),
                    hepyP.ppqhRejectionReason(), hepyP.ppqhCreatedAt(), hepyP.ppqhUpdatedAt(), hepyProofs(hepyC, hepyP.ppqhId())));
        }
        return hepyWithProofs;
    }

    private static List<PpqhPaymentProof> hepyProofs(Connection hepyC, long hepyPaymentId) throws SQLException {
        try (PreparedStatement hepyPs = hepyC.prepareStatement(
                "SELECT id, stored_path, mime_type, size_bytes, created_at FROM payment_proofs WHERE payment_id = ? ORDER BY id")) {
            hepyPs.setLong(1, hepyPaymentId);
            try (ResultSet hepyRs = hepyPs.executeQuery()) {
                List<PpqhPaymentProof> hepyList = new ArrayList<>();
                while (hepyRs.next()) {
                    hepyList.add(new PpqhPaymentProof(hepyRs.getLong(1), hepyRs.getString(2), hepyRs.getString(3), hepyRs.getLong(4),
                            hepyRs.getString(5)));
                }
                return hepyList;
            }
        }
    }
}
