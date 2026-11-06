package com.eldercare.backend.billing.internal.item.persistence;

import com.eldercare.backend.billing.internal.item.domain.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** T014: chỉ khoản theo sự kiện chưa khóa; khóa người/bảng ở service trước khi khóa khoản. */
@Repository
public class CostRepricingStore {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CostRepricingStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<RepriceCandidate> missingPrice(long serviceId, LocalDate from, LocalDate to) {
        var params = new ArrayList<Object>(List.of(serviceId, from));
        String condition = "c.service_id=? AND c.incurred_on>=? AND FIND_IN_SET('MISSING_UNIT_PRICE',c.flags)>0";
        if (to != null) {
            condition += " AND c.incurred_on<=?";
            params.add(to);
        }
        return candidates(condition, params.toArray());
    }

    public List<RepriceCandidate> addendum(long residentId, LocalDate effectiveDate) {
        return candidates(
                "c.resident_id=? AND (c.incurred_on>=? OR (c.source_type='DAY_CARE_SESSION' AND c.incurred_on>=?))",
                residentId,
                effectiveDate,
                effectiveDate.withDayOfMonth(1));
    }

    private List<RepriceCandidate> candidates(String condition, Object... params) {
        return jdbc.query(
                """
                SELECT c.id,c.resident_id,c.statement_id FROM cost_item c
                JOIN cost_statement s ON s.id=c.statement_id
                WHERE c.origin='EVENT_BASED' AND c.service_id IS NOT NULL
                    AND c.status NOT IN ('FINALIZED','CANCELLED') AND c.locked_at IS NULL AND s.locked_at IS NULL
                    AND s.status<>'FINALIZED' AND
                """ + condition + " ORDER BY c.resident_id,c.statement_id,c.id",
                (row, number) -> new RepriceCandidate(
                        row.getLong("id"), row.getLong("resident_id"), row.getLong("statement_id")),
                params);
    }

    public Optional<RepriceableCost> lock(long itemId, long residentId) {
        return jdbc
                .query(
                        """
                SELECT c.*,s.status AS statement_status FROM cost_item c JOIN cost_statement s ON s.id=c.statement_id
                WHERE c.id=? AND c.resident_id=? AND c.origin='EVENT_BASED' AND c.service_id IS NOT NULL
                    AND c.status NOT IN ('FINALIZED','CANCELLED') AND c.locked_at IS NULL AND s.locked_at IS NULL
                    AND s.status<>'FINALIZED' FOR UPDATE
                """,
                        (row, number) -> new RepriceableCost(
                                row.getLong("id"),
                                row.getLong("resident_id"),
                                row.getLong("statement_id"),
                                row.getLong("period_id"),
                                row.getLong("service_id"),
                                row.getString("source_type"),
                                row.getDate("incurred_on").toLocalDate(),
                                row.getString("status"),
                                row.getString("statement_status"),
                                row.getInt("version"),
                                new CostRecalculation.Calculation(
                                        row.getBigDecimal("quantity"),
                                        row.getBigDecimal("unit_price"),
                                        row.getBigDecimal("factor_percent"),
                                        row.getBigDecimal("amount")),
                                flags(row.getString("flags")),
                                map(row.getString("unit_price_basis")),
                                map(row.getString("source_values"))),
                        itemId,
                        residentId)
                .stream()
                .findFirst();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void apply(RepriceableCost row, CostPricingSnapshot next) {
        Long previous = jdbc.queryForObject("SELECT @m11_recalculating", Long.class);
        jdbc.execute("SET @m11_recalculating = 1");
        try {
            int changed = jdbc.update(
                    """
                    UPDATE cost_item SET service_id=?,quantity=?,unit_price=?,factor_percent=?,amount=?,
                        unit_price_basis=CAST(? AS JSON),source_values=CAST(? AS JSON),flags=?,status='DRAFT',
                        checked_by=NULL,checked_at=NULL,approved_by=NULL,approved_at=NULL,excess_approval_reason=NULL,
                        version=version+1
                    WHERE id=? AND resident_id=? AND version=? AND locked_at IS NULL
                        AND status NOT IN ('FINALIZED','CANCELLED')
                    """,
                    ((Number) next.unitPriceBasis().get("serviceId")).longValue(),
                    next.calculation().quantity(),
                    next.calculation().unitPrice(),
                    next.calculation().factorPercent(),
                    next.calculation().amount(),
                    json.writeValueAsString(next.unitPriceBasis()),
                    json.writeValueAsString(next.sourceValues()),
                    String.join(",", new TreeSet<>(next.mergeFlags(row.flags()))),
                    row.id(),
                    row.residentId(),
                    row.version());
            if (changed != 1) throw new IllegalStateException("Khoản đã thay đổi hoặc bị khóa khi tính lại giá");
        } finally {
            jdbc.update("SET @m11_recalculating = ?", previous);
        }
        if (row.statementStatus().equals("AWAITING_FINALIZATION")) {
            int changed = jdbc.update("""
                    UPDATE cost_statement SET status='OPEN',version=version+1
                    WHERE id=? AND resident_id=? AND status='AWAITING_FINALIZATION' AND locked_at IS NULL
                    """, row.statementId(), row.residentId());
            if (changed != 1) throw new IllegalStateException("Bảng không còn ở trạng thái Chờ chốt khi tính lại giá");
        }
    }

    public boolean sameJson(Map<String, Object> first, Map<String, Object> second) {
        return json.readTree(json.writeValueAsString(first)).equals(json.readTree(json.writeValueAsString(second)));
    }

    private Map<String, Object> map(String value) {
        Map<String, Object> parsed = value == null ? null : json.readValue(value, MAP);
        return parsed == null ? Map.of() : parsed;
    }

    private Set<String> flags(String value) {
        return value == null || value.isEmpty() ? Set.of() : Set.copyOf(Arrays.asList(value.split(",")));
    }
}
