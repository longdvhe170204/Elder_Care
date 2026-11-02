package com.eldercare.backend.admission.internal.waitinglist.persistence;

import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListEntry;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListStatus;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Hồ sơ chờ ở bảng gộp {@code admission_record} (data-model mục 0, 4). Cột sinh {@code open_entry_resident} có {@code UNIQUE} nên
 * mỗi người tối đa một hồ sơ chờ chưa ở trạng thái cuối (FR-007); {@code held_bed_unique} chặn hai hồ sơ giữ một giường (FR-014).
 */
@Repository
public class WaitingListStore {

    private static final String SELECT = "SELECT e.id, e.application_id, e.resident_id, e.care_level,"
            + " e.care_level_not_assessed, e.priority, e.priority_reason, e.held_bed_id, e.last_confirmed_at, e.status,"
            + " e.version, e.created_by, e.created_at, a.application_date, a.desired_residence_type FROM admission_record e"
            + " JOIN admission_record a ON a.id = e.application_id WHERE e.kind = 'WAITING_ENTRY'";

    /** Thứ tự danh sách chờ và danh sách phù hợp: Ưu tiên trước, ngày đăng ký, thời điểm tạo (FR-010, Q-394). */
    private static final String ORDER = " ORDER BY e.priority DESC, a.application_date ASC, e.created_at ASC, e.id ASC";

    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    WaitingListStore(JdbcTemplate jdbc, BusinessTime time) {
        this.jdbc = jdbc;
        this.time = time;
    }

    public long insert(
            long applicationId,
            long residentId,
            CareLevel careLevel,
            boolean notAssessed,
            long createdBy,
            Instant now) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO admission_record (kind, application_id, resident_id, care_level,"
                                    + " care_level_not_assessed, last_confirmed_at, status, created_by, created_at)"
                                    + " VALUES ('WAITING_ENTRY', ?, ?, ?, ?, ?, 'WAITING', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, applicationId);
                    statement.setLong(2, residentId);
                    statement.setString(3, careLevel.name());
                    statement.setBoolean(4, notAssessed);
                    statement.setObject(5, utc(now));
                    statement.setLong(6, createdBy);
                    statement.setObject(7, utc(now));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<WaitingListEntry> find(long id) {
        return jdbc.query(SELECT + " AND e.id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    public Optional<WaitingListEntry> lock(long id) {
        return jdbc.query(SELECT + " AND e.id = ? FOR UPDATE OF e", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    /** Hồ sơ chờ chưa ở trạng thái cuối của người cao tuổi, nếu có. */
    public Optional<WaitingListEntry> findOpenByResident(long residentId) {
        return jdbc.query(SELECT + " AND e.open_entry_resident = ?", (rs, i) -> map(rs), residentId).stream()
                .findFirst();
    }

    public Optional<WaitingListEntry> lockOpenByResident(long residentId) {
        return jdbc
                .query(SELECT + " AND e.open_entry_resident = ? FOR UPDATE OF e", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Hồ sơ chờ mới nhất của một lượt đăng ký, nếu có. */
    public Optional<Long> findIdByApplication(long applicationId) {
        return jdbc
                .query(
                        "SELECT id FROM admission_record WHERE kind = 'WAITING_ENTRY' AND application_id = ? ORDER BY id DESC"
                                + " LIMIT 1",
                        (rs, i) -> rs.getLong(1),
                        applicationId)
                .stream()
                .findFirst();
    }

    public List<WaitingListEntry> list(Set<WaitingListStatus> statuses) {
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>();
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND e.status IN (")
                    .append(statuses.stream().map(s -> "?").collect(Collectors.joining(",")))
                    .append(")");
            statuses.forEach(s -> args.add(s.name()));
        }
        sql.append(ORDER);
        return jdbc.query(sql.toString(), (rs, i) -> map(rs), args.toArray());
    }

    /** Hồ sơ chưa ở trạng thái cuối, theo mã. */
    public List<WaitingListEntry> listOpen() {
        return jdbc.query(SELECT + " AND e.open_entry_resident IS NOT NULL ORDER BY e.id", (rs, i) -> map(rs));
    }

    /** Hồ sơ Đang chờ có mốc cập nhật không muộn hơn {@code staleBefore} (nhắc FR-017). */
    public List<WaitingListEntry> listWaitingNotConfirmedSince(Instant staleBefore) {
        return jdbc.query(
                SELECT + " AND e.status = 'WAITING' AND e.last_confirmed_at <= ? ORDER BY e.id",
                (rs, i) -> map(rs),
                utc(staleBefore));
    }

    /**
     * Hồ sơ Đang chờ thỏa loại lưu trú, mức chăm sóc (FR-012). Giới tính, lần liên hệ với giường do dịch vụ lọc vì thuộc hồ sơ
     * người cao tuổi của 001 và bảng dùng chung {@code record_event}. Đã xếp theo FR-010.
     */
    public List<WaitingListEntry> waitingOf(Set<ResidenceType> residenceTypes, Set<CareLevel> careLevels) {
        if (residenceTypes.isEmpty() || careLevels.isEmpty()) {
            return List.of();
        }
        List<Object> args = new ArrayList<>();
        String types = residenceTypes.stream().map(t -> "?").collect(Collectors.joining(","));
        residenceTypes.forEach(t -> args.add(t.name()));
        String levels = careLevels.stream().map(l -> "?").collect(Collectors.joining(","));
        careLevels.forEach(l -> args.add(l.name()));
        return jdbc.query(
                SELECT + " AND e.status = 'WAITING' AND a.desired_residence_type IN (" + types + ")"
                        + " AND e.care_level IN (" + levels + ")" + ORDER,
                (rs, i) -> map(rs),
                args.toArray());
    }

    public void updateCareLevel(long id, CareLevel level, boolean notAssessed) {
        jdbc.update(
                "UPDATE admission_record SET care_level = ?, care_level_not_assessed = ?, version = version + 1 WHERE"
                        + " id = ?",
                level.name(),
                notAssessed,
                id);
    }

    /** Dấu Ưu tiên (Q-394): đặt kèm lý do; gỡ thì bỏ lý do. */
    public void updatePriority(long id, boolean priority, String reason) {
        jdbc.update(
                "UPDATE admission_record SET priority = ?, priority_reason = ?, version = version + 1 WHERE id = ?",
                priority,
                priority ? reason : null,
                id);
    }

    /** Đặt lại mốc cập nhật (CFG-M02-03): xác nhận còn nhu cầu. */
    public void touch(long id, Instant now) {
        jdbc.update(
                "UPDATE admission_record SET last_confirmed_at = ?, version = version + 1 WHERE id = ?", utc(now), id);
    }

    /** Đổi trạng thái; {@code heldBedId} là giường đang giữ sau lệnh (FR-014). Trạng thái cuối thì khóa dòng. */
    public void updateStatus(long id, WaitingListStatus status, Long heldBedId) {
        boolean closing = !status.isOpen();
        jdbc.update(
                "UPDATE admission_record SET status = ?, held_bed_id = ?, locked_at = ?, version = version + 1 WHERE id = ?",
                status.name(),
                heldBedId,
                closing ? utc(time.now()) : null,
                id);
    }

    private WaitingListEntry map(ResultSet rs) throws SQLException {
        return new WaitingListEntry(
                rs.getLong("id"),
                rs.getLong("application_id"),
                rs.getLong("resident_id"),
                CareLevel.valueOf(rs.getString("care_level")),
                rs.getBoolean("care_level_not_assessed"),
                rs.getBoolean("priority"),
                rs.getString("priority_reason"),
                rs.getObject("held_bed_id", Long.class),
                rs.getObject("last_confirmed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                WaitingListStatus.valueOf(rs.getString("status")),
                rs.getInt("version"),
                rs.getLong("created_by"),
                rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getObject("application_date", LocalDate.class),
                ResidenceType.valueOf(rs.getString("desired_residence_type")));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
