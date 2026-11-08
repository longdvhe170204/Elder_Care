package com.eldercare.backend.incident.internal.alert.persistence;

import com.eldercare.backend.incident.internal.alert.domain.HandoverItem;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Đọc và chuyển người phụ trách của cảnh báo, sự cố cho bàn giao ca (FR-037, Q-94). Hai bảng cùng module M05; cột
 * {@code handover_acknowledged} là dấu tạm nhận.
 */
@Repository
public class HandoverItemStore {
    private static final String ALERT_COLUMNS = """
            'ALERT' AS grp, id, resident_id, floor_area_id, status, level, COALESCE(description, type) AS description,
            first_at AS created_at, assignee_id, handover_acknowledged, version
            """;
    private static final String INCIDENT_COLUMNS = """
            'INCIDENT' AS grp, id, resident_id, floor_area_id, status, level, description,
            discovered_at AS created_at, assignee_id, handover_acknowledged, version
            """;
    private static final String ALERT_OPEN = "status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS')";
    private static final String INCIDENT_OPEN = "status IN ('NEW', 'IN_PROGRESS', 'MONITORING')";

    private final JdbcTemplate jdbc;

    public HandoverItemStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Cảnh báo, sự cố đang mở của tầng (tầng lưu lúc tạo theo research S-08, gồm người vắng mặt). */
    public List<HandoverItem> openByFloor(long floorAreaId) {
        return jdbc.query(
                "SELECT " + ALERT_COLUMNS + " FROM alert WHERE floor_area_id = ? AND " + ALERT_OPEN
                        + " UNION ALL SELECT " + INCIDENT_COLUMNS + " FROM incident WHERE floor_area_id = ? AND "
                        + INCIDENT_OPEN + " ORDER BY created_at, grp, id",
                HandoverItemStore::map,
                floorAreaId,
                floorAreaId);
    }

    /** Như {@link #openByFloor} nhưng khóa dòng, dùng trong transaction chuyển người phụ trách. */
    public List<HandoverItem> lockOpenByFloor(long floorAreaId) {
        var result = new ArrayList<HandoverItem>();
        result.addAll(jdbc.query(
                "SELECT " + ALERT_COLUMNS + " FROM alert WHERE floor_area_id = ? AND " + ALERT_OPEN
                        + " ORDER BY id FOR UPDATE",
                HandoverItemStore::map,
                floorAreaId));
        result.addAll(jdbc.query(
                "SELECT " + INCIDENT_COLUMNS + " FROM incident WHERE floor_area_id = ? AND " + INCIDENT_OPEN
                        + " ORDER BY id FOR UPDATE",
                HandoverItemStore::map,
                floorAreaId));
        return result;
    }

    /** Trạng thái hiện tại theo nhóm và mã, mọi trạng thái (008 đọc lại mục đã ghi vào bàn giao). */
    public List<HandoverItem> byIds(Collection<Long> alertIds, Collection<Long> incidentIds) {
        var result = new ArrayList<HandoverItem>();
        if (!alertIds.isEmpty()) {
            result.addAll(jdbc.query(
                    "SELECT " + ALERT_COLUMNS + " FROM alert WHERE id IN (" + marks(alertIds) + ")",
                    HandoverItemStore::map,
                    alertIds.toArray()));
        }
        if (!incidentIds.isEmpty()) {
            result.addAll(jdbc.query(
                    "SELECT " + INCIDENT_COLUMNS + " FROM incident WHERE id IN (" + marks(incidentIds) + ")",
                    HandoverItemStore::map,
                    incidentIds.toArray()));
        }
        return result;
    }

    /** Đổi người phụ trách; {@code provisional} đặt hoặc gỡ dấu tạm nhận; giữ trạng thái, cấp, hạn (FR-037). */
    public void reassign(HandoverItem item, long assigneeId, boolean provisional) {
        String table = HandoverItem.ALERT.equals(item.group()) ? "alert" : "incident";
        int changed = jdbc.update(
                "UPDATE " + table + " SET assignee_id = ?, handover_acknowledged = ?, version = version + 1"
                        + " WHERE id = ? AND version = ?",
                assigneeId,
                provisional,
                item.id(),
                item.version());
        if (changed != 1) throw new IllegalStateException("Mục bàn giao đã thay đổi đồng thời");
    }

    private static String marks(Collection<Long> ids) {
        return String.join(",", Collections.nCopies(ids.size(), "?"));
    }

    private static HandoverItem map(ResultSet rs, int row) throws SQLException {
        return new HandoverItem(
                rs.getString("grp"),
                rs.getLong("id"),
                (Long) rs.getObject("resident_id"),
                (Long) rs.getObject("floor_area_id"),
                rs.getString("status"),
                rs.getString("level"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant(),
                (Long) rs.getObject("assignee_id"),
                rs.getBoolean("handover_acknowledged"),
                rs.getInt("version"));
    }
}
