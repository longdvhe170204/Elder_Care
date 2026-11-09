package com.eldercare.backend.billing.internal.provision.persistence;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.provision.domain.ProvisionalItem;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CostProvisionStore {
    private final JdbcTemplate jdbc;

    public CostProvisionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProvisionalItem> provisional(long resident) {
        return jdbc.query(
                """
            SELECT i.*,EXISTS(SELECT 1 FROM cost_statement original
              WHERE original.period_id=i.original_period_id AND original.resident_id=i.resident_id
                AND original.statement_type='REGULAR' AND original.status='FINALIZED') AS original_finalized FROM cost_item i
            JOIN cost_statement s ON s.id=i.statement_id
            WHERE i.resident_id=? AND s.status<>'FINALIZED' ORDER BY i.id
            """,
                (r, n) -> new ProvisionalItem(
                        CostType.valueOf(r.getString("cost_type")),
                        r.getString("origin"),
                        r.getString("status"),
                        r.getObject("amount", Long.class),
                        "ADJUSTMENT".equals(r.getString("origin")) && r.getBoolean("original_finalized")
                                ? r.getTimestamp("created_at")
                                        .toInstant()
                                        .atZone(BusinessTime.ZONE)
                                        .toLocalDate()
                                : r.getObject("incurred_on", LocalDate.class)),
                resident);
    }

    public List<CostItemReportView> reports(LocalDate from, LocalDate to) {
        return jdbc.query(
                """
            SELECT i.*,p.period_month AS original_month FROM cost_item i
            LEFT JOIN cost_statement p ON p.id=i.original_period_id
            WHERE i.incurred_on BETWEEN ? AND ? ORDER BY i.incurred_on,i.id
            """,
                (r, n) -> new CostItemReportView(
                        r.getLong("id"),
                        r.getLong("resident_id"),
                        CostType.valueOf(r.getString("cost_type")),
                        r.getString("origin"),
                        r.getLong("created_by"),
                        r.getObject("service_id", Long.class),
                        r.getString("item_code"),
                        r.getObject("incurred_on", LocalDate.class),
                        r.getBigDecimal("quantity"),
                        r.getObject("unit_price", Long.class),
                        r.getObject("amount", Long.class),
                        r.getString("flags").isBlank()
                                ? Set.of()
                                : Set.copyOf(List.of(r.getString("flags").split(","))),
                        r.getString("status"),
                        r.getObject("original_item_id", Long.class),
                        r.getString("original_month")),
                from,
                to);
    }

    public Instant finalizedAt(long id) {
        return jdbc.queryForObject("SELECT finalized_at FROM cost_statement WHERE id=?", java.sql.Timestamp.class, id)
                .toInstant();
    }

    public Optional<Long> medicationId(long itemId) {
        return jdbc.queryForList("""
            SELECT x.medication_id FROM cost_item i
            LEFT JOIN cost_item original ON original.id=i.original_item_id
            JOIN cost_source_index x ON x.source_type=COALESCE(original.source_type,i.source_type)
              AND x.source_id=COALESCE(original.source_id,i.source_id)
              AND x.billing_unit=COALESCE(original.billing_unit,i.billing_unit)
            WHERE i.id=? AND x.medication_id IS NOT NULL
            """, Long.class, itemId).stream().findFirst();
    }
}
