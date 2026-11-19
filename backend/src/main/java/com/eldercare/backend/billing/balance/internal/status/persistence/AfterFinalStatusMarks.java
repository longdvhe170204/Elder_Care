package com.eldercare.backend.billing.balance.internal.status.persistence;

import com.eldercare.backend.billing.balance.internal.status.domain.AfterFinalCandidate;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Hồ sơ có thể cần nhắc sau trạng thái cuối (017 FR-024; cột {@code final_status_reminded_at} của {@code V017_01}): sổ có
 * giao dịch Đã xác nhận chưa nằm trong bảng quyết toán hiện hành (mã lớn hơn {@code lastTransactionId} của phiên bản mới
 * nhất ở {@code record_event}, hoặc chưa có bảng nào). Trạng thái cuối và căn cứ đang mở do tầng dịch vụ xét.
 */
@Repository
public class AfterFinalStatusMarks {

    private static final String CANDIDATES = """
            SELECT b.resident_id, b.balance, b.deposit_balance, b.final_status_reminded_at,
                   (SELECT MAX(t.confirmed_at) FROM balance_transaction t
                     WHERE t.resident_id = b.resident_id AND t.status = 'CONFIRMED') AS last_confirmed_at,
                   s.id AS statement_id
              FROM resident_balance b
              LEFT JOIN record_event s ON s.id = (
                    SELECT MAX(e.id) FROM record_event e
                     WHERE e.entity_type = 'M11.BALANCE' AND e.entity_id = b.resident_id
                       AND e.kind = 'M11.SETTLEMENT_STATEMENT')
             WHERE EXISTS (
                    SELECT 1 FROM balance_transaction t
                     WHERE t.resident_id = b.resident_id AND t.status = 'CONFIRMED'
                       AND t.id > COALESCE(JSON_VALUE(s.content, '$.lastTransactionId' RETURNING SIGNED), 0))
             ORDER BY b.resident_id""";

    private final JdbcTemplate jdbc;

    public AfterFinalStatusMarks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AfterFinalCandidate> candidates() {
        return jdbc.query(CANDIDATES, (rs, i) -> {
            Timestamp reminded = rs.getTimestamp("final_status_reminded_at");
            Timestamp last = rs.getTimestamp("last_confirmed_at");
            return new AfterFinalCandidate(
                    rs.getLong("resident_id"),
                    rs.getLong("balance"),
                    rs.getLong("deposit_balance"),
                    reminded == null ? null : reminded.toInstant(),
                    last == null ? null : last.toInstant(),
                    rs.getObject("statement_id") != null);
        });
    }

    public void reminded(long residentId, Instant now) {
        jdbc.update(
                "UPDATE resident_balance SET final_status_reminded_at = ? WHERE resident_id = ?",
                Timestamp.from(now),
                residentId);
    }
}
