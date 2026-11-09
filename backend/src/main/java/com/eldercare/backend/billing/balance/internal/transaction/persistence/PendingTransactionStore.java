package com.eldercare.backend.billing.balance.internal.transaction.persistence;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Ghi giao dịch Chờ duyệt và chuyển trạng thái của nó (017 T025, T026; data-model mục 3): Chờ duyệt sửa được (nhóm 2),
 * Đã xác nhận, Từ chối, Đã hủy khóa dòng ({@code locked_at}, trigger {@code trg_balance_transaction_row_lock}). Đọc dòng
 * qua {@code BalanceTransactionStore} của phần sổ.
 */
@Repository
public class PendingTransactionStore {

    private final JdbcTemplate jdbc;

    public PendingTransactionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dòng Chờ duyệt mới; {@code recipient} là JSON hoặc {@code null}. */
    public long insertPending(
            long residentId,
            Ledger ledger,
            TransactionType type,
            long amount,
            Instant at,
            PaymentMethod method,
            Long originalTransactionId,
            String recipient,
            String note,
            String reason,
            long createdBy) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO balance_transaction (resident_id, ledger, type, amount, occurred_at, method,"
                                    + " original_transaction_id, recipient, note, reason, status, created_at,"
                                    + " created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING_APPROVAL', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, residentId);
                    ps.setString(2, ledger.name());
                    ps.setString(3, type.name());
                    ps.setLong(4, amount);
                    ps.setTimestamp(5, Timestamp.from(at));
                    ps.setString(6, method.name());
                    if (originalTransactionId == null) ps.setNull(7, Types.BIGINT);
                    else ps.setLong(7, originalTransactionId);
                    ps.setString(8, recipient);
                    ps.setString(9, note);
                    ps.setString(10, reason);
                    ps.setTimestamp(11, Timestamp.from(at));
                    ps.setLong(12, createdBy);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    public void attachRequest(long id, long approvalRequestId) {
        jdbc.update(
                "UPDATE balance_transaction SET approval_request_id = ?, version = version + 1 WHERE id = ?"
                        + " AND status = 'PENDING_APPROVAL'",
                approvalRequestId,
                id);
    }

    /** Chờ duyệt → Đã xác nhận (lệnh duyệt; cặp cấn trừ thì kèm giao dịch liên kết). */
    public void confirm(long id, Long approvedBy, Long linkedTransactionId, Instant now) {
        Timestamp at = Timestamp.from(now);
        int updated = jdbc.update(
                "UPDATE balance_transaction SET status = 'CONFIRMED', approved_by = ?, linked_transaction_id ="
                        + " COALESCE(?, linked_transaction_id), confirmed_at = ?, locked_at = ?, version = version + 1"
                        + " WHERE id = ? AND status = 'PENDING_APPROVAL'",
                approvedBy,
                linkedTransactionId,
                at,
                at,
                id);
        if (updated != 1) throw new IllegalStateException("Giao dịch " + id + " không còn Chờ duyệt");
    }

    /** Chờ duyệt → Từ chối hoặc Đã hủy; trạng thái khác thì không đổi (sự kiện lặp). */
    public void close(long id, TransactionStatus status, Instant now) {
        jdbc.update(
                "UPDATE balance_transaction SET status = ?, locked_at = ?, version = version + 1"
                        + " WHERE id = ? AND status = 'PENDING_APPROVAL'",
                status.name(),
                Timestamp.from(now),
                id);
    }

    /** Dấu "đã điều chỉnh" của giao dịch gốc Đã xác nhận (FR-011); trigger chỉ cho đặt từ false sang true. */
    public void markAdjusted(long id) {
        jdbc.update("UPDATE balance_transaction SET adjusted = TRUE WHERE id = ? AND adjusted = FALSE", id);
    }

    /** Giao dịch con của cặp cấn trừ (Đã xác nhận ngay, trỏ giao dịch kia của cặp). */
    public long insertOffset(
            long residentId, Ledger ledger, long amount, Instant at, Long linked, String note, long by) {
        var keys = new GeneratedKeyHolder();
        boolean confirmed = linked != null;
        jdbc.update(
                con -> {
                    PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO balance_transaction (resident_id, ledger, type, amount, occurred_at, method,"
                                    + " linked_transaction_id, note, status, confirmed_at, locked_at, created_at,"
                                    + " created_by) VALUES (?, ?, 'DEPOSIT_OFFSET', ?, ?, 'INTERNAL', ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    Timestamp ts = Timestamp.from(at);
                    ps.setLong(1, residentId);
                    ps.setString(2, ledger.name());
                    ps.setLong(3, amount);
                    ps.setTimestamp(4, ts);
                    if (linked == null) ps.setNull(5, Types.BIGINT);
                    else ps.setLong(5, linked);
                    ps.setString(6, note);
                    ps.setString(7, confirmed ? "CONFIRMED" : "PENDING_APPROVAL");
                    ps.setTimestamp(8, confirmed ? ts : null);
                    ps.setTimestamp(9, confirmed ? ts : null);
                    ps.setTimestamp(10, ts);
                    ps.setLong(11, by);
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Mã giao dịch Chờ duyệt của mọi hồ sơ, cũ nhất trước. */
    public List<Long> pendingIds() {
        return jdbc.queryForList(
                "SELECT id FROM balance_transaction WHERE status = 'PENDING_APPROVAL' AND approval_request_id IS NOT NULL"
                        + " ORDER BY id",
                Long.class);
    }

    /** Người nhận (JSON) của giao dịch. */
    public String recipient(long id) {
        return jdbc.queryForObject("SELECT recipient FROM balance_transaction WHERE id = ?", String.class, id);
    }
}
