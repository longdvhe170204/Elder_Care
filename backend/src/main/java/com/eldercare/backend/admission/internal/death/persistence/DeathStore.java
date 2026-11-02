package com.eldercare.backend.admission.internal.death.persistence;

import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.internal.death.domain.DeathRecord;
import com.eldercare.backend.admission.internal.death.domain.PostDeathTask;
import com.eldercare.backend.admission.internal.domain.ResidencyState;
import com.eldercare.backend.admission.internal.domain.ResidencyState.PostDeathItem;
import com.eldercare.backend.admission.internal.persistence.ResidencyStateStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Bản ghi qua đời ở dòng {@code DEATH} của bảng gộp {@code residency_ending}, kết quả tính danh sách việc và dấu Đã đóng ở trạng thái
 * theo người (data-model mục 0, 10; R-15). Thời điểm lưu theo UTC. Mọi hàm ghi chạy trong transaction của lệnh, dưới khóa theo người
 * (research T-03). Bản ghi qua đời là nhóm 3, khóa ngay khi thêm.
 */
@Repository
public class DeathStore {

    private static final String SELECT = "SELECT id, resident_id, death_at, place, discovered_by, confirmed_by_id,"
            + " cause_of_death, evidence_file_id, history_id, created_by, created_at FROM residency_ending WHERE kind = 'DEATH'";

    private final JdbcTemplate jdbc;
    private final ResidencyStateStore state;

    DeathStore(JdbcTemplate jdbc, ResidencyStateStore state) {
        this.jdbc = jdbc;
        this.state = state;
    }

    public long insert(DeathRecord r) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO residency_ending (kind, resident_id, death_at, place, discovered_by, confirmed_by_id,"
                                    + " cause_of_death, evidence_file_id, history_id, created_by, created_at, locked_at)"
                                    + " VALUES ('DEATH', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
                    s.setObject(11, utc(r.recordedAt()));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<DeathRecord> find(long id) {
        return jdbc.query(SELECT + " AND id = ?", this::map, id).stream().findFirst();
    }

    public Optional<DeathRecord> ofResident(long residentId) {
        return jdbc.query(SELECT + " AND resident_id = ?", this::map, residentId).stream()
                .findFirst();
    }

    /** Bản ghi qua đời mà hồ sơ lưu trú chưa Đã đóng, tức danh sách việc còn mở (FR-072). */
    public List<DeathRecord> open() {
        return jdbc.query(SELECT + " ORDER BY id", this::map).stream()
                .filter(record -> state.closedAt(record.residentId()).isEmpty())
                .toList();
    }

    /** Thời điểm hồ sơ lưu trú Đã đóng; rỗng khi chưa đóng (FR-072). */
    public Optional<Instant> closedAt(long residentId) {
        return state.closedAt(residentId);
    }

    /** Đặt dấu Đã đóng một lần; dấu đã có thì không đổi (FR-072). */
    public boolean close(long residentId, Instant at) {
        return state.close(residentId, at);
    }

    public List<PostDeathTask> tasks(long recordId) {
        long residentId = find(recordId).orElseThrow().residentId();
        Map<String, PostDeathItem> items = state.get(residentId).postDeathItems();
        return items.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PostDeathTask(
                        recordId,
                        PostDeathItemCode.valueOf(entry.getKey()),
                        PostDeathItemStatus.valueOf(entry.getValue().status()),
                        entry.getValue().basis(),
                        entry.getValue().updatedAt()))
                .toList();
    }

    /** Thêm hoặc cập nhật kết quả tính của một mục. */
    public void upsertTask(
            long recordId, PostDeathItemCode code, PostDeathItemStatus status, String basis, Instant at) {
        long residentId = find(recordId).orElseThrow().residentId();
        ResidencyState current = state.get(residentId);
        Map<String, PostDeathItem> items = new TreeMap<>(current.postDeathItems());
        items.put(code.name(), new PostDeathItem(status.name(), basis, at));
        state.save(residentId, current.withPostDeathItems(items));
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
                rs.getLong("created_by"),
                instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
