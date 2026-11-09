package com.eldercare.backend.billing.internal.item.persistence;

import com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost;
import com.eldercare.backend.billing.internal.source.domain.CostSourceCandidate;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Ghi khoản trong transaction đã khóa người/kỳ/bảng; không quyết định nghiệp vụ ở tầng lưu. */
@Repository
public class CostCreationStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CostCreationStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long statement(long period, long resident, String type, long actor, Instant at) {
        String key = "STATEMENT:" + period + ":" + resident + ":" + type;
        var ids = jdbc.queryForList("SELECT id FROM cost_statement WHERE unique_key=? FOR UPDATE", Long.class, key);
        if (!ids.isEmpty()) return ids.getFirst();
        var values = new LinkedHashMap<String, Object>();
        values.put("kind", "STATEMENT");
        values.put("period_id", period);
        values.put("resident_id", resident);
        values.put("statement_type", type);
        values.put("unique_key", key);
        values.put("created_by", actor);
        values.put("created_at", Timestamp.from(at));
        return new SimpleJdbcInsert(jdbc)
                .withTableName("cost_statement")
                .usingGeneratedKeyColumns("id")
                .usingColumns(values.keySet().toArray(String[]::new))
                .executeAndReturnKey(values)
                .longValue();
    }

    public long source(
            CostSourceCandidate source,
            CalculatedCost cost,
            long statement,
            long period,
            Long originalPeriod,
            long actor,
            Instant at) {
        var v = new LinkedHashMap<String, Object>();
        v.put("resident_id", source.residentId());
        v.put("statement_id", statement);
        v.put("period_id", period);
        v.put("cost_type", source.costType().name());
        v.put("origin", originalPeriod == null ? "EVENT_BASED" : "ADJUSTMENT");
        v.put("source_type", source.sourceType());
        v.put("source_id", source.sourceId());
        v.put("billing_unit", source.billingUnit());
        v.put("service_id", source.serviceId());
        v.put("description", source.description());
        v.put("incurred_on", source.date());
        v.put("occurred_at", Timestamp.from(source.occurredAt()));
        v.put("quantity", cost.quantity());
        v.put("unit", source.unit());
        v.put("unit_price", cost.unitPrice());
        v.put("factor_percent", cost.factor());
        v.put("amount", cost.amount());
        v.put("unit_price_basis", json.writeValueAsString(cost.basis()));
        v.put("source_values", json.writeValueAsString(cost.sourceValues()));
        v.put("flags", String.join(",", cost.flags()));
        v.put("created_by", actor);
        v.put("created_at", Timestamp.from(at));
        v.put(
                "unique_key",
                originalPeriod == null
                        ? source.sourceType() + ":" + source.sourceId() + ":" + source.billingUnit()
                        : null);
        if (originalPeriod != null) {
            v.put("original_period_id", originalPeriod);
            v.put("adjustment_type", "LATE_ADDITION");
            v.put("reason", "Lập bổ sung nguồn thuộc bảng đã chốt");
        }
        return new SimpleJdbcInsert(jdbc)
                .withTableName("cost_item")
                .usingGeneratedKeyColumns("id")
                .usingColumns(v.keySet().toArray(String[]::new))
                .executeAndReturnKey(v)
                .longValue();
    }

    public long adjustment(
            long original,
            long statement,
            long period,
            long amount,
            String type,
            String reason,
            Long change,
            long actor,
            Instant at) {
        jdbc.update(
                """
                INSERT INTO cost_item (resident_id,statement_id,period_id,cost_type,origin,description,
                  incurred_on,quantity,unit,amount,reason,original_item_id,original_period_id,adjustment_type,
                  source_change_id,created_by,created_at)
                SELECT resident_id,?,?,cost_type,'ADJUSTMENT','Khoản điều chỉnh',?,1,'khoản',?,?,id,period_id,?,?,?,?
                FROM cost_item WHERE id=?
                """,
                statement,
                period,
                LocalDate.ofInstant(at, com.eldercare.backend.common.time.BusinessTime.ZONE),
                amount,
                reason,
                type,
                change,
                actor,
                Timestamp.from(at),
                original);
        // Caller giữ transaction, cùng kết nối theo quy ước mục 8 team-plan.
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public java.math.BigDecimal adjustmentTotal(long original) {
        // Đọc khóa hiện hành, tránh snapshot cũ trong transaction chờ khóa người.
        return jdbc
                .queryForList(
                        "SELECT amount FROM cost_item WHERE original_item_id=? AND status<>'CANCELLED' FOR UPDATE",
                        java.math.BigDecimal.class,
                        original)
                .stream()
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
