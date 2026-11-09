package com.eldercare.backend.billing.internal.period.persistence;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.period.domain.*;
import com.eldercare.backend.common.error.NotFoundException;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class CostStatementStore {
    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public CostStatementStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    private static final String SELECT = """
            SELECT s.*,p.period_month AS billing_month FROM cost_statement s JOIN cost_statement p ON p.id=s.period_id
            WHERE s.kind='STATEMENT'
            """;

    public CostStatementRecord statement(long id) {
        return jdbc.query(SELECT + " AND s.id=?", this::map, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public CostStatementRecord lockedStatement(long id) {
        return jdbc.query(SELECT + " AND s.id=? FOR UPDATE OF s", this::map, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<CostStatementRecord> list(Long period, Long resident, String status) {
        return jdbc.query(
                SELECT + " AND (? IS NULL OR s.period_id=?) AND (? IS NULL OR s.resident_id=?)"
                        + " AND (? IS NULL OR s.status=?) ORDER BY p.period_month DESC,s.id",
                this::map,
                period,
                period,
                resident,
                resident,
                status,
                status);
    }

    public Optional<CostStatementRecord> previous(CostStatementRecord statement) {
        return jdbc
                .query(
                        SELECT + " AND s.resident_id=? AND s.statement_type='REGULAR' AND p.period_month=?",
                        this::map,
                        statement.residentId(),
                        statement.month().minusMonths(1).toString())
                .stream()
                .findFirst();
    }

    public void touch(long id, boolean draft) {
        jdbc.update("""
                UPDATE cost_statement SET version=version+1,
                  status=CASE WHEN ? THEN 'OPEN' ELSE status END WHERE id=?
                """, draft, id);
    }

    public void submit(long id, long actor, Instant at, PeriodCostChange.Warning warning) {
        jdbc.update("""
                UPDATE cost_statement SET status='AWAITING_FINALIZATION',version=version+1,
                  submitted_by=?,submitted_at=?,change_warning=? WHERE id=?
                """, actor, Timestamp.from(at), warning == null ? null : json.writeValueAsString(warning), id);
    }

    public void finalizeStatement(long id, long actor, Instant at, Map<CostType, Long> totals) {
        long total = totals.values().stream().reduce(0L, Math::addExact);
        var byType = totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new CostTypeTotal(e.getKey(), e.getValue()))
                .toList();
        jdbc.update("""
                UPDATE cost_statement SET status='FINALIZED',version=version+1,total=?,totals_by_type=?,
                  finalized_by=?,finalized_at=?,locked_at=? WHERE id=?
                """, total, json.writeValueAsString(byType), actor, Timestamp.from(at), Timestamp.from(at), id);
    }

    public Map<CostType, Long> totals(long statement, boolean comparison) {
        String condition = comparison ? " AND (i.origin<>'ADJUSTMENT' OR i.original_period_id=i.period_id)" : "";
        var result = new EnumMap<CostType, Long>(CostType.class);
        jdbc.query(
                "SELECT i.cost_type,SUM(i.amount) total FROM cost_item i WHERE i.statement_id=?"
                        + " AND i.status<>'CANCELLED'" + condition + " GROUP BY i.cost_type",
                r -> {
                    result.put(CostType.valueOf(r.getString("cost_type")), r.getLong("total"));
                },
                statement);
        return result;
    }

    public void lockPeriod(long period) {
        if (jdbc.queryForList(
                        "SELECT id FROM cost_statement WHERE id=? AND kind='PERIOD' FOR UPDATE", Long.class, period)
                .isEmpty()) throw new NotFoundException();
    }

    public boolean finalizePeriodIfComplete(long period, Set<Long> expected, Instant at) {
        var rows = jdbc.query(SELECT + " AND s.period_id=? ORDER BY s.id FOR UPDATE OF s", this::map, period);
        boolean complete = rows.stream().allMatch(s -> s.status().equals("FINALIZED"));
        var covered = new HashSet<Long>();
        rows.stream().filter(s -> s.statementType().equals("REGULAR")).forEach(s -> covered.add(s.residentId()));
        if (complete && covered.containsAll(expected))
            return jdbc.update("""
                UPDATE cost_statement SET status='FINALIZED',version=version+1,finalized_at=?,locked_at=?
                WHERE id=? AND status='AWAITING_FINALIZATION'
                """, Timestamp.from(at), Timestamp.from(at), period) == 1;
        return false;
    }

    private CostStatementRecord map(ResultSet r, int row) throws SQLException {
        return new CostStatementRecord(
                r.getLong("id"),
                r.getLong("period_id"),
                r.getLong("resident_id"),
                YearMonth.parse(r.getString("billing_month")),
                r.getString("statement_type"),
                r.getBoolean("final_period"),
                r.getObject("through_date", LocalDate.class),
                r.getString("status"),
                r.getObject("total", Long.class),
                r.getInt("version"));
    }
}
