package com.cambae.backend.m02.internal.death.persistence;

import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.domain.PostDeathTask;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Truy cập các bảng {@code death_record}, {@code post_death_task} và dấu Đã đóng của {@code resident_residency} (data-model
 * mục 10). Thời điểm lưu theo UTC. Mọi hàm ghi chạy trong transaction của lệnh, dưới khóa {@code resident_residency} của
 * người cao tuổi (research T-03).
 */
@Repository
public class DeathStore {

    private static final String COLUMNS = "id, resident_id, death_at, place, discovered_by, confirmed_by_id,"
            + " cause_of_death, evidence_file_id, history_id, recorded_by, recorded_at";

    private final JdbcTemplate jdbc;

    DeathStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(DeathRecord r) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO death_record (resident_id, death_at, place, discovered_by, confirmed_by_id,"
                                    + " cause_of_death, evidence_file_id, history_id, recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, r.residentId());
                    s.setObject(2, utc(r.deathAt()));
                    s.setString(3, r.place());
                    s.setString(4, r.discoveredBy());
                    s.setLong(5, r.confirmedById());
                    s.setString(6, r.causeOfDeath());
                    s.setObject(7, r.evidenceFileId());
                    s.setLong(8, r.historyId());
                    s.setLong(9, r.recordedBy());
                    s.setObject(10, utc(r.recordedAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<DeathRecord> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM death_record WHERE id = ?", this::map, id).stream()
                .findFirst();
    }

    public Optional<DeathRecord> ofResident(long residentId) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM death_record WHERE resident_id = ?", this::map, residentId)
                .stream()
                .findFirst();
    }

    /** Bản ghi qua đời mà hồ sơ lưu trú chưa Đã đóng, tức danh sách việc còn mở (FR-072). */
    public List<DeathRecord> open() {
        return jdbc.query(
                "SELECT d.id, d.resident_id, d.death_at, d.place, d.discovered_by, d.confirmed_by_id,"
                        + " d.cause_of_death, d.evidence_file_id, d.history_id, d.recorded_by, d.recorded_at"
                        + " FROM death_record d JOIN resident_residency r ON r.resident_id = d.resident_id"
                        + " WHERE r.closed_at IS NULL ORDER BY d.id",
                this::map);
    }

    /** Thời điểm hồ sơ lưu trú Đã đóng; rỗng khi chưa đóng (FR-072). */
    public Optional<Instant> closedAt(long residentId) {
        return jdbc
                .query(
                        "SELECT closed_at FROM resident_residency WHERE resident_id = ? AND closed_at IS NOT NULL",
                        (rs, row) -> instant(rs, "closed_at"),
                        residentId)
                .stream()
                .findFirst();
    }

    /** Đặt dấu Đã đóng một lần; dấu đã có thì không đổi (FR-072). */
    public boolean close(long residentId, Instant at) {
        return jdbc.update(
                        "UPDATE resident_residency SET closed_at = ?, version = version + 1 WHERE resident_id = ? AND"
                                + " closed_at IS NULL",
                        utc(at),
                        residentId)
                == 1;
    }

    public List<PostDeathTask> tasks(long recordId) {
        return jdbc.query(
                "SELECT death_record_id, item_code, status, basis, updated_at FROM post_death_task WHERE"
                        + " death_record_id = ? ORDER BY item_code",
                (rs, row) -> new PostDeathTask(
                        rs.getLong("death_record_id"),
                        PostDeathItemCode.valueOf(rs.getString("item_code")),
                        PostDeathItemStatus.valueOf(rs.getString("status")),
                        rs.getString("basis"),
                        instant(rs, "updated_at")),
                recordId);
    }

    /** Thêm hoặc cập nhật kết quả tính của một mục. */
    public void upsertTask(
            long recordId, PostDeathItemCode code, PostDeathItemStatus status, String basis, Instant at) {
        jdbc.update(
                "INSERT INTO post_death_task (death_record_id, item_code, status, basis, updated_at) VALUES (?, ?, ?,"
                        + " ?, ?) ON DUPLICATE KEY UPDATE status = VALUES(status), basis = VALUES(basis), updated_at ="
                        + " VALUES(updated_at)",
                recordId,
                code.name(),
                status.name(),
                basis,
                utc(at));
    }

    private DeathRecord map(ResultSet rs, int row) throws SQLException {
        return new DeathRecord(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                instant(rs, "death_at"),
                rs.getString("place"),
                rs.getString("discovered_by"),
                rs.getLong("confirmed_by_id"),
                rs.getString("cause_of_death"),
                (Long) rs.getObject("evidence_file_id"),
                rs.getLong("history_id"),
                rs.getLong("recorded_by"),
                instant(rs, "recorded_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
