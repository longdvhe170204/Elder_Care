package com.eldercare.backend.billing.internal.finalperiod.persistence;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FinalPeriodStore {
    private final JdbcTemplate jdbc;

    public FinalPeriodStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void mark(long id, LocalDate through) {
        jdbc.update(
                "UPDATE cost_statement SET final_period=?,through_date=?,version=version+1 WHERE id=? AND status<>'FINALIZED'",
                through != null,
                through,
                id);
    }

    public Set<LocalDate> covered(long resident) {
        return new HashSet<>(jdbc.queryForList("""
                SELECT DISTINCT i.incurred_on FROM cost_item i JOIN cost_statement s ON s.id=i.statement_id
                WHERE i.resident_id=? AND i.source_type='RESIDENCY_DAY' AND i.status='FINALIZED'
                  AND s.status='FINALIZED' AND (i.origin='EVENT_BASED' OR i.adjustment_type='LATE_ADDITION')
                """, LocalDate.class, resident));
    }

    public boolean pendingAdjustment(long resident) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM cost_item WHERE resident_id=? AND status NOT IN ('FINALIZED','CANCELLED')
                  AND FIND_IN_SET('AFFECTS_TERMINATION',flags)>0
                """, Long.class, resident) > 0;
    }

    public List<Long> trackedResidents() {
        return jdbc.queryForList("""
                SELECT DISTINCT resident_id FROM cost_statement WHERE kind='STATEMENT'
                ORDER BY resident_id
                """, Long.class);
    }

    public Optional<Instant> lastSupplementReminder(long statement) {
        return jdbc.queryForList("""
                SELECT MAX(marked_at) FROM reminder_marker
                WHERE kind='M11.COST_REMINDER' AND entity_type='M11.COST_STATEMENT' AND entity_id=?
                  AND marker LIKE 'SUPPLEMENTARY:%'
                """, LocalDateTime.class, statement).stream()
                .filter(Objects::nonNull)
                .map(t -> t.toInstant(ZoneOffset.UTC))
                .findFirst();
    }
}
