package com.cambae.backend.m02.internal.waitinglist.persistence;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.waitinglist.domain.Circumstance;
import com.cambae.backend.m02.internal.waitinglist.domain.CircumstanceStatus;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreBreakdown;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreCause;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreHistoryRow;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListStatus;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Hồ sơ chờ, tình huống, lịch sử điểm và lần liên hệ giường ở các bảng {@code waiting_list_entry}, {@code
 * waiting_list_circumstance}, {@code score_history}, {@code bed_contact} (data-model mục 4). Cột sinh {@code
 * open_resident_id} có {@code UNIQUE} nên mỗi người tối đa một hồ sơ chờ chưa ở trạng thái cuối (FR-007); lịch sử điểm và
 * lần liên hệ chỉ thêm (trigger).
 */
@Repository
public class WaitingListStore {

    private static final String SELECT = "SELECT e.id, e.application_id, e.resident_id, e.care_level,"
            + " e.care_level_not_assessed, e.score_breakdown, e.held_bed_id, e.last_confirmed_at, e.status, e.version,"
            + " e.created_by, e.created_at, a.application_date, a.desired_residence_type FROM waiting_list_entry e"
            + " JOIN admission_application a ON a.id = e.application_id";

    /** Thứ tự danh sách chờ và danh sách phù hợp: điểm giảm dần, ngày đăng ký, thời điểm tạo (FR-012). */
    private static final String ORDER =
            " ORDER BY e.current_score DESC, a.application_date ASC, e.created_at ASC, e.id ASC";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    WaitingListStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long insert(
            long applicationId,
            long residentId,
            CareLevel careLevel,
            boolean notAssessed,
            ScoreBreakdown breakdown,
            long createdBy,
            Instant now) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO waiting_list_entry (application_id, resident_id, care_level,"
                                    + " care_level_not_assessed, current_score, score_breakdown, last_confirmed_at,"
                                    + " status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'WAITING', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, applicationId);
                    statement.setLong(2, residentId);
                    statement.setString(3, careLevel.name());
                    statement.setBoolean(4, notAssessed);
                    statement.setInt(5, breakdown.total());
                    statement.setString(6, json.writeValueAsString(breakdown));
                    statement.setObject(7, utc(now));
                    statement.setLong(8, createdBy);
                    statement.setObject(9, utc(now));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<WaitingListEntry> find(long id) {
        return jdbc.query(SELECT + " WHERE e.id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    public Optional<WaitingListEntry> lock(long id) {
        return jdbc.query(SELECT + " WHERE e.id = ? FOR UPDATE OF e", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    /** Hồ sơ chờ chưa ở trạng thái cuối của người cao tuổi, nếu có. */
    public Optional<WaitingListEntry> findOpenByResident(long residentId) {
        return jdbc.query(SELECT + " WHERE e.open_resident_id = ?", (rs, i) -> map(rs), residentId).stream()
                .findFirst();
    }

    public Optional<WaitingListEntry> lockOpenByResident(long residentId) {
        return jdbc
                .query(SELECT + " WHERE e.open_resident_id = ? FOR UPDATE OF e", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Hồ sơ chờ mới nhất của một lượt đăng ký, nếu có. */
    public Optional<Long> findIdByApplication(long applicationId) {
        return jdbc
                .query(
                        "SELECT id FROM waiting_list_entry WHERE application_id = ? ORDER BY id DESC LIMIT 1",
                        (rs, i) -> rs.getLong(1),
                        applicationId)
                .stream()
                .findFirst();
    }

    public List<WaitingListEntry> list(Set<WaitingListStatus> statuses) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
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

    /** Hồ sơ chưa ở trạng thái cuối, đã lập trước {@code createdBefore} (việc tính điểm của một ngày). */
    public List<WaitingListEntry> listOpen(Instant createdBefore) {
        return jdbc.query(
                SELECT + " WHERE e.open_resident_id IS NOT NULL AND e.created_at < ? ORDER BY e.id",
                (rs, i) -> map(rs),
                utc(createdBefore));
    }

    /** Hồ sơ Đang chờ có mốc cập nhật không muộn hơn {@code staleBefore} (nhắc FR-017). */
    public List<WaitingListEntry> listWaitingNotConfirmedSince(Instant staleBefore) {
        return jdbc.query(
                SELECT + " WHERE e.status = 'WAITING' AND e.last_confirmed_at <= ? ORDER BY e.id",
                (rs, i) -> map(rs),
                utc(staleBefore));
    }

    /**
     * Hồ sơ Đang chờ thỏa loại lưu trú, mức chăm sóc, chưa liên hệ, từ chối hay hết hạn với {@code bedId} từ {@code
     * vacancyStart} (FR-012). Giới tính do dịch vụ lọc vì thuộc hồ sơ người cao tuổi của 001.
     */
    public List<WaitingListEntry> matching(
            long bedId, Instant vacancyStart, Set<ResidenceType> residenceTypes, Set<CareLevel> careLevels) {
        if (residenceTypes.isEmpty() || careLevels.isEmpty()) {
            return List.of();
        }
        List<Object> args = new ArrayList<>();
        String types = residenceTypes.stream().map(t -> "?").collect(Collectors.joining(","));
        residenceTypes.forEach(t -> args.add(t.name()));
        String levels = careLevels.stream().map(l -> "?").collect(Collectors.joining(","));
        careLevels.forEach(l -> args.add(l.name()));
        args.add(bedId);
        args.add(utc(vacancyStart));
        return jdbc.query(
                SELECT + " WHERE e.status = 'WAITING' AND a.desired_residence_type IN (" + types + ")"
                        + " AND e.care_level IN (" + levels + ")"
                        + " AND NOT EXISTS (SELECT 1 FROM bed_contact c WHERE c.entry_id = e.id AND c.bed_id = ?"
                        + " AND c.at >= ?)" + ORDER,
                (rs, i) -> map(rs),
                args.toArray());
    }

    public List<Circumstance> circumstances(long entryId) {
        return jdbc.query(
                "SELECT circumstance_code, status FROM waiting_list_circumstance WHERE entry_id = ? ORDER BY"
                        + " circumstance_code",
                (rs, i) -> new Circumstance(rs.getString(1), CircumstanceStatus.valueOf(rs.getString(2))),
                entryId);
    }

    public void insertCircumstance(long entryId, String code, CircumstanceStatus status) {
        jdbc.update(
                "INSERT INTO waiting_list_circumstance (entry_id, circumstance_code, status) VALUES (?, ?, ?)",
                entryId,
                code,
                status.name());
    }

    public void confirmCircumstance(long entryId, String code) {
        jdbc.update(
                "UPDATE waiting_list_circumstance SET status = 'CONFIRMED' WHERE entry_id = ? AND circumstance_code = ?",
                entryId,
                code);
    }

    public void deleteCircumstance(long entryId, String code) {
        jdbc.update(
                "DELETE FROM waiting_list_circumstance WHERE entry_id = ? AND circumstance_code = ?", entryId, code);
    }

    /** Ghi điểm mới (tổng và thành phần). */
    public void updateScore(long id, ScoreBreakdown breakdown) {
        jdbc.update(
                "UPDATE waiting_list_entry SET current_score = ?, score_breakdown = ?, version = version + 1 WHERE id = ?",
                breakdown.total(),
                json.writeValueAsString(breakdown),
                id);
    }

    public void updateCareLevel(long id, CareLevel level, boolean notAssessed) {
        jdbc.update(
                "UPDATE waiting_list_entry SET care_level = ?, care_level_not_assessed = ?, version = version + 1 WHERE"
                        + " id = ?",
                level.name(),
                notAssessed,
                id);
    }

    /** Đặt lại mốc cập nhật (CFG-M02-03): xác nhận còn nhu cầu hoặc sửa tình huống. */
    public void touch(long id, Instant now) {
        jdbc.update(
                "UPDATE waiting_list_entry SET last_confirmed_at = ?, version = version + 1 WHERE id = ?",
                utc(now),
                id);
    }

    public void updateStatus(long id, WaitingListStatus status) {
        jdbc.update("UPDATE waiting_list_entry SET status = ?, version = version + 1 WHERE id = ?", status.name(), id);
    }

    public void addHistory(
            long entryId,
            int oldScore,
            ScoreBreakdown components,
            ScoreCause cause,
            Long approvalRequestId,
            Instant at) {
        jdbc.update(
                "INSERT INTO score_history (entry_id, old_score, new_score, components, cause, approval_request_id,"
                        + " at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                entryId,
                oldScore,
                components.total(),
                json.writeValueAsString(components),
                cause.name(),
                approvalRequestId,
                utc(at));
    }

    public List<ScoreHistoryRow> history(long entryId) {
        return jdbc.query(
                "SELECT id, entry_id, old_score, new_score, components, cause, approval_request_id, at FROM"
                        + " score_history WHERE entry_id = ? ORDER BY id",
                (rs, i) -> new ScoreHistoryRow(
                        rs.getLong("id"),
                        rs.getLong("entry_id"),
                        rs.getInt("old_score"),
                        rs.getInt("new_score"),
                        json.readValue(rs.getString("components"), ScoreBreakdown.class),
                        ScoreCause.valueOf(rs.getString("cause")),
                        rs.getObject("approval_request_id", Long.class),
                        rs.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                entryId);
    }

    private WaitingListEntry map(ResultSet rs) throws SQLException {
        return new WaitingListEntry(
                rs.getLong("id"),
                rs.getLong("application_id"),
                rs.getLong("resident_id"),
                CareLevel.valueOf(rs.getString("care_level")),
                rs.getBoolean("care_level_not_assessed"),
                json.readValue(rs.getString("score_breakdown"), ScoreBreakdown.class),
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
