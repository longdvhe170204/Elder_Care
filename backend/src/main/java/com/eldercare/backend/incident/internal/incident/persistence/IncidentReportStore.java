package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.internal.incident.domain.IncidentReportRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Đọc cảnh báo, sự cố cho thống kê của 016 (FR-081; module-interfaces mục 1.10; T061). Chỉ đọc; khoảng {@code [from, to)}
 * theo lần đầu của cảnh báo, thời điểm tạo của sự cố. Lọc phạm vi ở bên gọi.
 */
@Repository
public class IncidentReportStore {
    /** Kiểu cảnh báo của nút SOS và nút hỗ trợ khẩn cấp (SosService, EmergencyAssistService). */
    private static final String SOS_TYPE = "RESIDENT_HELP_REQUEST";

    private static final String ASSIST_TYPE = "STAFF_ASSIST_REQUEST";

    private final JdbcTemplate jdbc;

    public IncidentReportStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AlertReportView> alerts(Instant from, Instant to) {
        return jdbc.query("""
                SELECT * FROM alert WHERE first_at >= ? AND first_at < ? ORDER BY first_at, id
                """, (rs, i) -> alert(rs), Timestamp.from(from), Timestamp.from(to));
    }

    /** Sự cố chưa hủy (FR-081 bỏ Đã hủy) kèm mã loại hiện hành để bên gọi lấy dấu "không thuộc sức khỏe". */
    public List<IncidentReportRow> incidents(Instant from, Instant to) {
        return jdbc.query(
                """
                SELECT * FROM incident
                WHERE created_at >= ? AND created_at < ? AND status <> 'CANCELLED'
                ORDER BY created_at, id
                """,
                (rs, i) -> new IncidentReportRow(
                        rs.getLong("id"),
                        nullableLong(rs, "resident_id"),
                        rs.getString("level"),
                        rs.getLong("current_type_id"),
                        rs.getString("type_code"),
                        nullableLong(rs, "floor_area_id"),
                        instant(rs, "created_at"),
                        instant(rs, "received_at"),
                        rs.getInt("escalation_level")),
                Timestamp.from(from),
                Timestamp.from(to));
    }

    private static AlertReportView alert(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        return new AlertReportView(
                rs.getLong("id"),
                nullableLong(rs, "resident_id"),
                rs.getString("level"),
                rs.getString("level_at_received"),
                instant(rs, "first_at"),
                instant(rs, "received_at"),
                rs.getInt("escalation_level"),
                rs.getTimestamp("max_escalation_at") != null,
                type,
                nullableLong(rs, "floor_area_id"),
                SOS_TYPE.equals(type) ? "SOS" : ASSIST_TYPE.equals(type) ? "EMERGENCY_ASSIST" : "OTHER",
                instant(rs, "arrived_at"),
                rs.getBoolean("skip_arrival_time"));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
