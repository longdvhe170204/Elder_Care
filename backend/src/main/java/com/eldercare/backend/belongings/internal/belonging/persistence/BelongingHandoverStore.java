package com.eldercare.backend.belongings.internal.belonging.persistence;

import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.belonging.domain.NewHandover;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Thêm, đọc bản ghi bàn giao {@code belonging_handover} (nhóm 3: không sửa, không xóa; data-model mục 4). */
@Repository
public class BelongingHandoverStore {

    private static final String COLUMNS =
            "id, belonging_id, kind, target_status, giver_type, giver_name, recipient_type,"
                    + " recipient_name, occurred_at, quantity, cash_amount, condition_level_id, condition_description,"
                    + " storage_location_id, reason, photo_file_ids, incident_id, recorded_at";

    private final JdbcTemplate jdbc;

    BelongingHandoverStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(NewHandover h) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO belonging_handover (belonging_id, resident_id, kind, target_status, giver_type,"
                                    + " giver_id, giver_name, giver_id_document, recipient_type, recipient_id,"
                                    + " recipient_name, occurred_at, quantity, cash_amount, condition_level_id,"
                                    + " condition_description, storage_location_id, reason, holder_absent, valuable,"
                                    + " photo_file_ids, note, recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON),"
                                    + " ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    int i = 1;
                    ps.setLong(i++, h.belongingId());
                    ps.setLong(i++, h.residentId());
                    ps.setString(i++, h.kind().name());
                    ps.setString(i++, h.targetStatus().name());
                    ps.setString(
                            i++, h.giverType() == null ? null : h.giverType().name());
                    ps.setObject(i++, h.giverId());
                    ps.setString(i++, h.giverName());
                    ps.setString(i++, h.giverIdDocument());
                    ps.setString(
                            i++,
                            h.recipientType() == null ? null : h.recipientType().name());
                    ps.setObject(i++, h.recipientId());
                    ps.setString(i++, h.recipientName());
                    ps.setObject(i++, utc(h.occurredAt()));
                    ps.setBigDecimal(i++, h.quantity());
                    ps.setBigDecimal(i++, h.cashAmount());
                    ps.setObject(i++, h.conditionLevelId());
                    ps.setString(i++, h.conditionDescription());
                    ps.setObject(i++, h.storageLocationId());
                    ps.setString(i++, h.reason());
                    ps.setBoolean(i++, h.holderAbsent());
                    ps.setBoolean(i++, h.valuable());
                    ps.setString(
                            i++,
                            h.photoFileIds().stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]")));
                    ps.setString(i++, h.note());
                    ps.setLong(i++, h.recordedBy());
                    ps.setObject(i, utc(h.recordedAt()));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Lịch sử của mọi đồ cùng lần tiếp nhận (gồm đồ tách; FR-012), theo thứ tự ghi. */
    public List<HandoverView> ofIntake(long intakeHandoverId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM belonging_handover WHERE belonging_id IN"
                        + " (SELECT id FROM belonging WHERE intake_handover_id = ?) ORDER BY id",
                BelongingHandoverStore::view,
                intakeHandoverId);
    }

    private static HandoverView view(ResultSet rs, int row) throws SQLException {
        String photos = rs.getString("photo_file_ids").replaceAll("[\\[\\]\\s]", "");
        return new HandoverView(
                rs.getLong("id"),
                rs.getLong("belonging_id"),
                rs.getString("kind"),
                rs.getString("target_status"),
                rs.getString("giver_type"),
                rs.getString("giver_name"),
                rs.getString("recipient_type"),
                rs.getString("recipient_name"),
                instant(rs, "occurred_at"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("cash_amount"),
                rs.getObject("condition_level_id", Long.class),
                rs.getString("condition_description"),
                rs.getObject("storage_location_id", Long.class),
                rs.getString("reason"),
                photos.isEmpty()
                        ? List.of()
                        : Arrays.stream(photos.split(",")).map(Long::valueOf).toList(),
                rs.getObject("incident_id", Long.class),
                instant(rs, "recorded_at"));
    }

    static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
