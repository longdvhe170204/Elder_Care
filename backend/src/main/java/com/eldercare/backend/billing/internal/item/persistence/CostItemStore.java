package com.eldercare.backend.billing.internal.item.persistence;

import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.item.domain.*;
import com.eldercare.backend.common.error.NotFoundException;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CostItemStore {
    private final JdbcTemplate jdbc;

    public CostItemStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public CostItemRecord item(long id) {
        return jdbc.query("SELECT * FROM cost_item WHERE id=?", this::map, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public CostItemRecord lockedItem(long id) {
        return jdbc.query("SELECT * FROM cost_item WHERE id=? FOR UPDATE", this::map, id).stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<CostItemRecord> lockItems(long statement) {
        return jdbc.query("SELECT * FROM cost_item WHERE statement_id=? ORDER BY id FOR UPDATE", this::map, statement);
    }

    public List<CostItemRecord> ofStatement(long id) {
        return jdbc.query("SELECT * FROM cost_item WHERE statement_id=? ORDER BY id", this::map, id);
    }

    public List<CostItemRecord> selected(CostItemSelection selection) {
        if (selection.filter() == null) {
            return selection.itemIds().stream().map(this::item).toList();
        }
        var filter = selection.filter();
        return ofStatement(filter.statementId()).stream()
                .filter(i -> filter.costType() == null || filter.costType() == i.costType())
                .filter(i -> filter.origin() == null || filter.origin().equals(i.origin()))
                .filter(i -> filter.flags() == null
                        || filter.flags().isBlank()
                        || i.flags().contains(filter.flags()))
                .toList();
    }

    public List<CostItemRecord> selectedUnderLock(CostItemSelection selection) {
        if (selection.filter() == null)
            return selection.itemIds().stream().map(this::lockedItem).toList();
        var filter = selection.filter();
        return lockItems(filter.statementId()).stream()
                .filter(i -> filter.costType() == null || filter.costType() == i.costType())
                .filter(i -> filter.origin() == null || filter.origin().equals(i.origin()))
                .filter(i -> filter.flags() == null
                        || filter.flags().isBlank()
                        || i.flags().contains(filter.flags()))
                .toList();
    }

    public void transition(CostItemRecord item, String status, long actor, Instant at, String reason) {
        jdbc.update(
                """
                UPDATE cost_item SET status=?,version=version+1,
                  checked_by=CASE WHEN ?='CHECKED' THEN ? ELSE checked_by END,
                  checked_at=CASE WHEN ?='CHECKED' THEN ? ELSE checked_at END,
                  approved_by=CASE WHEN ?='APPROVED' THEN ? ELSE approved_by END,
                  approved_at=CASE WHEN ?='APPROVED' THEN ? ELSE approved_at END,
                  excess_approval_reason=CASE WHEN ?='APPROVED' THEN ? ELSE excess_approval_reason END,
                  cancellation_reason=CASE WHEN ?='CANCELLED' THEN ? ELSE cancellation_reason END,
                  cancelled_by=CASE WHEN ?='CANCELLED' THEN ? ELSE cancelled_by END,
                  unique_key=CASE WHEN ?='CANCELLED' THEN NULL ELSE unique_key END,
                  locked_at=CASE WHEN ? IN ('CANCELLED','FINALIZED') THEN ? ELSE NULL END
                WHERE id=?
                """,
                status,
                status,
                actor,
                status,
                Timestamp.from(at),
                status,
                actor,
                status,
                Timestamp.from(at),
                status,
                reason,
                status,
                reason,
                status,
                actor,
                status,
                status,
                Timestamp.from(at),
                item.id());
    }

    private CostItemRecord map(ResultSet r, int row) throws SQLException {
        String flags = r.getString("flags");
        return new CostItemRecord(
                r.getLong("id"),
                r.getLong("statement_id"),
                r.getLong("resident_id"),
                r.getLong("period_id"),
                CostType.valueOf(r.getString("cost_type")),
                r.getString("origin"),
                r.getString("status"),
                r.getObject("amount", Long.class),
                flags.isBlank() ? Set.of() : Set.copyOf(Arrays.asList(flags.split(","))),
                r.getObject("checked_by", Long.class),
                r.getLong("created_by"),
                r.getObject("original_period_id", Long.class));
    }
}
