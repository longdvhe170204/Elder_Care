package com.eldercare.backend.billing.balance.internal.status.persistence;

import com.eldercare.backend.billing.balance.internal.status.domain.DepositRefundDue;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Mốc nhắc hoàn cọc trên {@code resident_balance} (017 T026, FR-015a): {@code deposit_refund_due_since}, {@code _reminded_at}. */
@Repository
public class DepositRefundMarks {

    private final JdbcTemplate jdbc;

    public DepositRefundMarks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Đặt mốc khi còn tiền cọc; đã có mốc thì giữ. */
    public void due(long residentId, Instant now) {
        jdbc.update(
                "UPDATE resident_balance SET deposit_refund_due_since = COALESCE(deposit_refund_due_since, ?)"
                        + " WHERE resident_id = ? AND deposit_balance > 0",
                Timestamp.from(now),
                residentId);
    }

    /** Bỏ mốc của hồ sơ đã hoàn hết cọc, trả các hồ sơ còn cần nhắc. */
    public List<DepositRefundDue> dueList() {
        jdbc.update("UPDATE resident_balance SET deposit_refund_due_since = NULL, deposit_refund_reminded_at = NULL"
                + " WHERE deposit_refund_due_since IS NOT NULL AND deposit_balance <= 0");
        return jdbc.query(
                "SELECT resident_id, deposit_balance, deposit_refund_reminded_at FROM resident_balance"
                        + " WHERE deposit_refund_due_since IS NOT NULL ORDER BY resident_id",
                (rs, i) -> {
                    Timestamp last = rs.getTimestamp("deposit_refund_reminded_at");
                    return new DepositRefundDue(
                            rs.getLong("resident_id"),
                            rs.getLong("deposit_balance"),
                            last == null ? null : last.toInstant());
                });
    }

    public void reminded(long residentId, Instant now) {
        jdbc.update(
                "UPDATE resident_balance SET deposit_refund_reminded_at = ? WHERE resident_id = ?",
                Timestamp.from(now),
                residentId);
    }
}
