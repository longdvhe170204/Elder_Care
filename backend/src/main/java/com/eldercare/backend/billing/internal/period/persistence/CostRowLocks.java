package com.eldercare.backend.billing.internal.period.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Khóa bảng chi phí của đúng người trong transaction đã khóa trạng thái M11. */
@Repository
public class CostRowLocks {
    private final JdbcTemplate jdbc;

    public CostRowLocks(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean statement(long residentId, long statementId) {
        return !jdbc.queryForList(
                        "SELECT id FROM cost_statement WHERE id=? AND resident_id=? AND kind='STATEMENT' FOR UPDATE",
                        Long.class,
                        statementId,
                        residentId)
                .isEmpty();
    }
}
