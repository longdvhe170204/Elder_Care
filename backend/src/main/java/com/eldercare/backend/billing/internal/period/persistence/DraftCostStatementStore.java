package com.eldercare.backend.billing.internal.period.persistence;

import com.eldercare.backend.billing.internal.period.domain.DraftCostStatement;
import com.eldercare.backend.common.error.NotFoundException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** T011: caller khóa M11 theo người trước khi đọc khóa kỳ/bảng hoặc ghi bảng thường. */
@Repository
public class DraftCostStatementStore {
    private final JdbcTemplate jdbc;

    public DraftCostStatementStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String periodStatusForShare(long periodId) {
        return jdbc.query("SELECT status FROM cost_statement WHERE id=? AND kind='PERIOD' FOR SHARE",
                (row, number) -> row.getString("status"), periodId)
                .stream().findFirst().orElseThrow(NotFoundException::new);
    }

    public Optional<DraftCostStatement> regularForUpdate(long residentId, long periodId) {
        // Khóa M11 theo người đã giữ phía caller; không khóa khoảng rỗng làm cản lệnh của người khác.
        var ids = jdbc.query("""
                SELECT id FROM cost_statement WHERE kind='STATEMENT' AND resident_id=? AND period_id=?
                    AND statement_type='REGULAR'
                """, (row, number) -> row.getLong("id"), residentId, periodId);
        if (ids.isEmpty()) return Optional.empty();
        return jdbc.query("""
                SELECT id,period_id,resident_id,status,final_period,through_date,version
                FROM cost_statement WHERE id=? AND kind='STATEMENT' AND resident_id=? AND period_id=?
                    AND statement_type='REGULAR' FOR UPDATE
                """, (row, number) -> new DraftCostStatement(
                row.getLong("id"), row.getLong("period_id"), row.getLong("resident_id"),
                row.getString("status"), row.getBoolean("final_period"),
                row.getObject("through_date", LocalDate.class), row.getInt("version")), ids.getFirst(), residentId, periodId)
                .stream().findFirst();
    }

    public DraftCostStatement createRegular(long residentId, long periodId, long actorId, Instant createdAt) {
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO cost_statement
                        (kind,period_id,resident_id,statement_type,status,unique_key,created_at,created_by)
                    VALUES ('STATEMENT',?,?,'REGULAR','OPEN',?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, periodId);
            statement.setLong(2, residentId);
            statement.setString(3, "STATEMENT:" + periodId + ":" + residentId + ":REGULAR");
            statement.setTimestamp(4, Timestamp.from(createdAt));
            statement.setLong(5, actorId);
            return statement;
        }, key);
        var id = key.getKey();
        if (id == null) throw new IllegalStateException("Không nhận được id bảng chi phí vừa lập");
        return regularForUpdate(residentId, periodId).filter(row -> row.id() == id.longValue()).orElseThrow();
    }

    public void reopen(long residentId, long statementId) {
        int changed = jdbc.update("""
                UPDATE cost_statement SET status='OPEN',version=version+1
                WHERE id=? AND resident_id=? AND kind='STATEMENT' AND status='AWAITING_FINALIZATION'
                """, statementId, residentId);
        if (changed != 1) throw new IllegalStateException("Bảng chi phí không còn ở trạng thái Chờ chốt");
    }
}
