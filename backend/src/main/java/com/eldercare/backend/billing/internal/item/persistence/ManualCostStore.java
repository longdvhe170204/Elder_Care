package com.eldercare.backend.billing.internal.item.persistence;

import com.eldercare.backend.billing.internal.item.domain.ManualCost;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Khoản nhập tay (FR-021): nguồn gốc MANUAL, loại "Khoản khác", lý do bắt buộc (ràng buộc chk_cost_item_origin). */
@Repository
public class ManualCostStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public ManualCostStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long create(ManualCost cost, long actor, Instant at) {
        var values = new LinkedHashMap<String, Object>();
        values.put("resident_id", cost.residentId());
        values.put("statement_id", cost.statementId());
        values.put("period_id", cost.periodId());
        values.put("cost_type", "OTHER");
        values.put("origin", "MANUAL");
        values.put("description", cost.description());
        values.put("incurred_on", cost.incurredOn());
        values.put("quantity", cost.quantity());
        values.put("unit", cost.unit());
        values.put("unit_price", cost.unitPrice());
        values.put("amount", cost.amount());
        values.put("reason", cost.reason());
        values.put("source_values", json.writeValueAsString(Map.of("fileIds", cost.fileIds())));
        values.put("created_by", actor);
        values.put("created_at", Timestamp.from(at));
        return new SimpleJdbcInsert(jdbc)
                .withTableName("cost_item")
                .usingGeneratedKeyColumns("id")
                .usingColumns(values.keySet().toArray(String[]::new))
                .executeAndReturnKey(values)
                .longValue();
    }
}
