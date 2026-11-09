package com.eldercare.backend.billing.internal.period.persistence;

import com.eldercare.backend.billing.internal.item.domain.CostItemRecord;
import com.eldercare.backend.billing.internal.item.persistence.CostItemStore;
import com.eldercare.backend.billing.internal.period.domain.CostPeriodRecord;
import com.eldercare.backend.common.data.TechnicalLocks;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CostPeriodStore {
    private final JdbcTemplate jdbc;
    private final TechnicalLocks locks;
    private final CostItemStore items;

    public CostPeriodStore(JdbcTemplate jdbc, TechnicalLocks locks, CostItemStore items) {
        this.jdbc = jdbc;
        this.locks = locks;
        this.items = items;
    }

    public List<CostPeriodRecord> periods() {
        return jdbc.query(
                "SELECT * FROM cost_statement WHERE kind='PERIOD' ORDER BY period_month,id",
                (r, n) -> new CostPeriodRecord(
                        r.getLong("id"),
                        YearMonth.parse(r.getString("period_month")),
                        r.getString("status"),
                        r.getObject("finalization_due_on", LocalDate.class)));
    }

    public List<com.eldercare.backend.billing.api.dto.CostPeriodView> views() {
        return jdbc.query(
                "SELECT * FROM cost_statement WHERE kind='PERIOD' ORDER BY period_month DESC,id DESC",
                (r, n) -> new com.eldercare.backend.billing.api.dto.CostPeriodView(
                        r.getLong("id"),
                        r.getString("period_month"),
                        r.getObject("period_starts_on", LocalDate.class),
                        r.getObject("period_ends_on", LocalDate.class),
                        r.getString("status"),
                        r.getObject("finalization_due_on", LocalDate.class),
                        r.getTimestamp("finalized_at") == null
                                ? null
                                : r.getTimestamp("finalized_at").toInstant()));
    }

    public boolean ensurePeriod(YearMonth month, Instant at) {
        locks.lock("M11.COST_PERIOD:" + month);
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM cost_statement WHERE unique_key=?)", Boolean.class, "PERIOD:" + month)))
            return false;
        jdbc.update("""
                INSERT INTO cost_statement(kind,period_month,period_starts_on,period_ends_on,unique_key,created_at)
                VALUES ('PERIOD',?,?,?,?,?)
                """, month.toString(), month.atDay(1), month.atEndOfMonth(), "PERIOD:" + month, Timestamp.from(at));
        return true;
    }

    public long periodId(YearMonth month) {
        return jdbc.queryForObject(
                "SELECT id FROM cost_statement WHERE kind='PERIOD' AND period_month=?", Long.class, month.toString());
    }

    public boolean closePeriod(long id, LocalDate due, Instant at) {
        return jdbc.update("""
                UPDATE cost_statement SET status='AWAITING_FINALIZATION',finalization_due_on=?,version=version+1
                WHERE id=? AND kind='PERIOD' AND status='OPEN'
                """, due, id) == 1;
    }

    public boolean hasRegularStatement(long period, long resident) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM cost_statement WHERE kind='STATEMENT' AND period_id=?
                  AND resident_id=? AND statement_type='REGULAR')
                """, Boolean.class, period, resident));
    }

    public List<CostItemRecord> advanceItems(LocalDate before) {
        return jdbc.queryForList("""
                SELECT id FROM cost_item WHERE FIND_IN_SET('CREATED_IN_ADVANCE',flags)>0
                  AND incurred_on<? AND status NOT IN ('FINALIZED','CANCELLED') ORDER BY resident_id,period_id,statement_id,id
                """, Long.class, before).stream()
                .map(items::item)
                .toList();
    }

    public void releaseAdvanceFlag(long id) {
        jdbc.update("""
                UPDATE cost_item SET flags=TRIM(BOTH ',' FROM REPLACE(CONCAT(',',flags,','),',CREATED_IN_ADVANCE,',',')),
                  version=version+1 WHERE id=? AND status NOT IN ('FINALIZED','CANCELLED')
                """, id);
    }
}
