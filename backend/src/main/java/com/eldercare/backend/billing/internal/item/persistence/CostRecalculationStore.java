package com.eldercare.backend.billing.internal.item.persistence;

import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.billing.internal.pricing.domain.CalculatedCost;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class CostRecalculationStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CostRecalculationStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<StoredCostCalculation> source(String type, String source, String unit) {
        return jdbc.query(
                "SELECT * FROM cost_item WHERE source_type=? AND source_id=? AND billing_unit=? AND status<>'CANCELLED' ORDER BY resident_id,period_id,statement_id,id",
                this::map,
                type,
                source,
                unit);
    }

    public List<StoredCostCalculation> affected(
            Long resident, Long service, LocalDate from, LocalDate to, boolean missingOnly) {
        return jdbc.query(
                """
                SELECT * FROM cost_item WHERE source_type IS NOT NULL AND status<>'CANCELLED'
                  AND (? IS NULL OR resident_id=?) AND (? IS NULL OR service_id=?) AND incurred_on>=?
                  AND (? IS NULL OR incurred_on<=?)
                """ + (missingOnly ? " AND status<>'FINALIZED' AND FIND_IN_SET('MISSING_UNIT_PRICE',flags)>0" : "")
                        + " ORDER BY resident_id,period_id,statement_id,id",
                this::map,
                resident,
                resident,
                service,
                service,
                from,
                to,
                to);
    }

    public StoredCostCalculation lock(long id) {
        return jdbc.query("SELECT * FROM cost_item WHERE id=? FOR UPDATE", this::map, id)
                .getFirst();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recalculate(long id, CalculatedCost cost) {
        // Biến phiên phải được trả về 0 cả khi UPDATE lỗi, vì pool tái sử dụng connection.
        jdbc.execute("SET @m11_recalculating=1");
        try {
            jdbc.update(
                    """
                    UPDATE cost_item SET quantity=?,unit_price=?,factor_percent=?,amount=?,unit_price_basis=?,
                      source_values=?,flags=?,status='DRAFT',checked_by=NULL,checked_at=NULL,approved_by=NULL,
                      approved_at=NULL,excess_approval_reason=NULL,version=version+1 WHERE id=?
                    """,
                    cost.quantity(),
                    cost.unitPrice(),
                    cost.factor(),
                    cost.amount(),
                    json.writeValueAsString(cost.basis()),
                    json.writeValueAsString(cost.sourceValues()),
                    String.join(",", cost.flags()),
                    id);
        } finally {
            jdbc.execute("SET @m11_recalculating=0");
        }
    }

    public void cancel(long id, String reason, Instant at) {
        jdbc.update("""
                UPDATE cost_item SET status='CANCELLED',cancellation_reason=?,unique_key=NULL,locked_at=?,version=version+1
                WHERE id=? AND status NOT IN ('FINALIZED','CANCELLED')
                """, reason, Timestamp.from(at), id);
    }

    public List<Long> changeItems(Long resident) {
        return jdbc.queryForList("""
                SELECT DISTINCT i.id FROM cost_item i JOIN record_event e
                  ON e.entity_id=i.id AND e.entity_type='M11.COST_ITEM' AND e.kind='M11.POST_FINALIZATION_CHANGE'
                WHERE (? IS NULL OR i.resident_id=?) ORDER BY i.id
                """, Long.class, resident, resident);
    }

    private StoredCostCalculation map(ResultSet r, int n) throws SQLException {
        String raw = r.getString("source_values");
        var stamp = r.getTimestamp("occurred_at");
        return new StoredCostCalculation(
                r.getLong("id"),
                r.getLong("resident_id"),
                r.getLong("statement_id"),
                r.getLong("period_id"),
                r.getString("status"),
                r.getString("source_type"),
                r.getString("source_id"),
                r.getString("billing_unit"),
                r.getObject("service_id", Long.class),
                r.getObject("incurred_on", LocalDate.class),
                stamp == null ? null : stamp.toInstant(),
                new CostRecalculation.Calculation(
                        r.getBigDecimal("quantity"),
                        r.getBigDecimal("unit_price"),
                        r.getBigDecimal("factor_percent"),
                        r.getBigDecimal("amount")),
                raw == null ? Map.of() : json.readValue(raw, new TypeReference<Map<String, Object>>() {}));
    }
}
