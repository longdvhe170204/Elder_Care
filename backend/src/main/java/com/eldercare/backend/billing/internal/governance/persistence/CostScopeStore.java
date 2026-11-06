package com.eldercare.backend.billing.internal.governance.persistence;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Chỉ đọc liên kết NCT trong bảng M11 đã có; không suy NCT từ id bảng hoặc khoản. */
@Repository
public class CostScopeStore {
    private final JdbcTemplate jdbc;

    public CostScopeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Long> resident(String type, long id) {
        String query = switch (type) {
            case "M11.COST_STATEMENT" -> "SELECT resident_id FROM cost_statement WHERE id=? AND kind='STATEMENT'";
            case "M11.COST_ITEM" -> "SELECT resident_id FROM cost_item WHERE id=?";
            default -> throw new IllegalArgumentException("Loại chi phí chưa có phạm vi: " + type);
        };
        return jdbc.queryForList(query, Long.class, id).stream().findFirst();
    }
}
