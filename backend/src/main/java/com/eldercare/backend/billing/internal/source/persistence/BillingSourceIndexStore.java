package com.eldercare.backend.billing.internal.source.persistence;

import com.eldercare.backend.billing.internal.source.domain.BillingSourceKey;
import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.billing.internal.source.domain.IndexedSourceMatch;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T010: chỉ ghi lần đầu; sự kiện ban đầu đến muộn không ghi đè kết quả đính chính của T021. */
@Repository
public class BillingSourceIndexStore {
    private final JdbcTemplate jdbc;

    public BillingSourceIndexStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IndexedSourceMatch> find(long residentId, String sourceType, LocalDate from, LocalDate to) {
        return findMatching("s.incurred_on BETWEEN ? AND ?", residentId, sourceType, from, to);
    }

    public List<IndexedSourceMatch> findBySourceIds(long residentId, String sourceType, List<String> sourceIds) {
        if (sourceIds.isEmpty()) {
            return List.of();
        }
        var parameters = new ArrayList<Object>();
        parameters.add(residentId);
        parameters.add(sourceType);
        parameters.addAll(sourceIds);
        String placeholders = String.join(",", Collections.nCopies(sourceIds.size(), "?"));
        return findMatching("s.source_id IN (" + placeholders + ")", parameters.toArray());
    }

    public Map<BillingSourceKey, Long> effectiveItems(long residentId, String sourceType, List<String> sourceIds) {
        if (sourceIds.isEmpty()) return Map.of();
        var parameters = new ArrayList<Object>();
        parameters.add(residentId);
        parameters.add(sourceType);
        parameters.addAll(sourceIds);
        var result = new java.util.LinkedHashMap<BillingSourceKey, Long>();
        jdbc.query(
                "SELECT source_id,billing_unit,id FROM cost_item WHERE resident_id=? AND source_type=?"
                        + " AND status<>'CANCELLED' AND (origin='EVENT_BASED' OR adjustment_type='LATE_ADDITION')"
                        + " AND source_id IN (" + String.join(",", Collections.nCopies(sourceIds.size(), "?")) + ")",
                row -> {
                    result.put(
                            new BillingSourceKey(row.getString("source_id"), row.getString("billing_unit")),
                            row.getLong("id"));
                },
                parameters.toArray());
        return result;
    }

    private List<IndexedSourceMatch> findMatching(String condition, Object... parameters) {
        return jdbc.query(
                """
                SELECT s.*,i.id AS existing_item_id
                FROM cost_source_index s
                LEFT JOIN cost_item i ON i.resident_id=s.resident_id
                    AND i.origin='EVENT_BASED' AND i.status<>'CANCELLED'
                    AND i.source_type=s.source_type AND i.source_id=s.source_id AND i.billing_unit=s.billing_unit
                WHERE s.resident_id=? AND s.source_type=? AND
                """ + condition + " ORDER BY s.incurred_on,s.occurred_at,s.source_id,s.billing_unit",
                (row, number) -> new IndexedSourceMatch(
                        new IndexedBillingSource(
                                row.getString("source_type"),
                                row.getString("source_id"),
                                row.getString("billing_unit"),
                                row.getLong("resident_id"),
                                row.getDate("incurred_on").toLocalDate(),
                                row.getTimestamp("occurred_at").toInstant(),
                                row.getObject("service_id", Long.class),
                                row.getObject("medication_id", Long.class),
                                row.getBigDecimal("current_quantity")),
                        row.getBoolean("cancelled"),
                        row.getLong("version"),
                        row.getObject("existing_item_id", Long.class)),
                parameters);
    }

    public void insertIfAbsent(IndexedBillingSource source, Instant indexedAt) {
        try {
            jdbc.update(
                    "INSERT INTO cost_source_index (source_type,source_id,billing_unit,resident_id,incurred_on,"
                            + "occurred_at,service_id,medication_id,current_quantity,cancelled,updated_at,version)"
                            + " VALUES (?,?,?,?,?,?,?,?,?,FALSE,?,0)",
                    source.sourceType(),
                    source.sourceId(),
                    source.unit(),
                    source.residentId(),
                    source.incurredOn(),
                    Timestamp.from(source.occurredAt()),
                    source.serviceId(),
                    source.medicationId(),
                    source.quantity(),
                    Timestamp.from(indexedAt));
        } catch (DuplicateKeyException duplicate) {
            // Khóa nguồn đã có: giữ nguyên dữ liệu hiện hành, kể cả đã hủy hoặc đã đính chính.
        }
    }

    public List<IndexedSourceMatch> byKey(String type, String id, String unit) {
        var residents = jdbc.queryForList(
                "SELECT DISTINCT resident_id FROM cost_source_index WHERE source_type=? AND source_id=? AND billing_unit=?",
                Long.class,
                type,
                id,
                unit);
        return residents.stream()
                .flatMap(resident -> findBySourceIds(resident, type, List.of(id)).stream())
                .filter(row -> row.source().unit().equals(unit))
                .toList();
    }

    public void change(
            String type, String id, String unit, java.math.BigDecimal quantity, boolean cancelled, Instant at) {
        jdbc.update(
                "UPDATE cost_source_index SET current_quantity=COALESCE(?,current_quantity),cancelled=?,updated_at=?,version=version+1"
                        + " WHERE source_type=? AND source_id=? AND billing_unit=?",
                quantity,
                cancelled,
                Timestamp.from(at),
                type,
                id,
                unit);
    }
}
