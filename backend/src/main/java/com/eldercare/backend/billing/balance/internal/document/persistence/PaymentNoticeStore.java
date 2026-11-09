package com.eldercare.backend.billing.balance.internal.document.persistence;

import com.eldercare.backend.billing.balance.internal.document.domain.UnissuedStatement;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Danh sách bảng chi phí đã có giao dịch thanh toán nhưng chưa phát hành phiếu (017 FR-031; DBR-37). */
@Repository
public class PaymentNoticeStore {

    private final JdbcTemplate jdbc;

    public PaymentNoticeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mới nhất trước; phiếu đã phát hành được nhận ra qua khóa duy nhất {@code M11.PAYMENT_NOTICE:<bảng>}. */
    public List<UnissuedStatement> unissued(String keyPrefix) {
        return jdbc.query(
                "SELECT t.cost_statement_id, t.resident_id, t.confirmed_at FROM balance_transaction t"
                        + " WHERE t.type = 'STATEMENT_PAYMENT' AND t.status = 'CONFIRMED'"
                        + " AND t.cost_statement_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM record_event r"
                        + " WHERE r.unique_key = CONCAT(?, t.cost_statement_id)) ORDER BY t.id DESC",
                (rs, i) -> {
                    Timestamp paid = rs.getTimestamp("confirmed_at");
                    return new UnissuedStatement(
                            rs.getLong("cost_statement_id"),
                            rs.getLong("resident_id"),
                            paid == null ? null : paid.toInstant());
                },
                keyPrefix);
    }
}
