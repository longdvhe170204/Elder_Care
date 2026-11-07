package com.eldercare.backend.nutrition.internal.assignment.persistence;

import com.eldercare.backend.nutrition.api.dto.DietAssignmentStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.assignment.domain.AssignmentBasis;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.domain.NewDietAssignment;
import java.sql.PreparedStatement;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc, ghi {@code diet_assignment} (V011_03); lệnh ghi chạy trong transaction của service, dưới khóa người cao tuổi
 * ({@link ResidentNutritionLock}). Mọi lệnh đổi trạng thái tăng {@code version}; nội dung do trigger khóa (FR-010).
 */
@Repository
public class DietAssignmentStore {

    private static final String SELECT = "SELECT id, resident_id, diet_id, texture, personal_restrictions,"
            + " personal_instruction, basis, basis_id, review_notice_id, requested_effective_from, actual_effective_at,"
            + " status, professional_reason, version FROM diet_assignment";
    private static final TypeReference<List<Long>> IDS = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    DietAssignmentStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long insert(long residentId, NewDietAssignment content, long createdBy, Instant at) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement s = c.prepareStatement(
                            "INSERT INTO diet_assignment (resident_id, diet_id, texture, personal_restrictions,"
                                    + " personal_instruction, basis, basis_id, review_notice_id,"
                                    + " requested_effective_from, status, created_by, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'PROPOSED', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, residentId);
                    s.setLong(2, content.dietId());
                    s.setString(3, content.texture().name());
                    s.setString(4, json.writeValueAsString(content.personalRestrictions()));
                    s.setString(5, content.personalInstruction());
                    s.setString(6, content.basis().name());
                    s.setObject(7, content.basisId());
                    s.setObject(8, content.reviewNoticeId());
                    s.setObject(9, utc(content.effectiveFrom()));
                    s.setLong(10, createdBy);
                    s.setObject(11, utc(at));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<DietAssignment> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", this::row, id).stream().findFirst();
    }

    /** Mới nhất trước (bản hiện hành và lịch sử, api-011 GET). */
    public List<DietAssignment> ofResident(long residentId) {
        return jdbc.query(SELECT + " WHERE resident_id = ? ORDER BY id DESC", this::row, residentId);
    }

    public Optional<DietAssignment> active(long residentId) {
        return jdbc.query(SELECT + " WHERE active_key = ?", this::row, residentId).stream()
                .findFirst();
    }

    public Optional<DietAssignment> waiting(long residentId) {
        return jdbc.query(SELECT + " WHERE pending_key = ?", this::row, residentId).stream()
                .findFirst();
    }

    /** Bản Chờ hiệu lực đã tới thời điểm hiệu lực mong muốn (việc theo phút, FR-017). */
    public List<DietAssignment> duePending(Instant now) {
        return jdbc.query(
                SELECT + " WHERE status = 'PENDING_EFFECTIVE' AND requested_effective_from <= ? ORDER BY id",
                this::row,
                utc(now));
    }

    /**
     * Bản Hiệu lực và bản Chờ hiệu lực có hiệu lực trước {@code before} (FR-027: "có hiệu lực giao với tuần"); người cao
     * tuổi ở trạng thái cuối do service lọc.
     */
    public List<DietAssignment> effectiveBefore(Instant before) {
        return jdbc.query(
                SELECT + " WHERE status = 'ACTIVE' OR (status = 'PENDING_EFFECTIVE' AND requested_effective_from < ?)"
                        + " ORDER BY resident_id, id",
                this::row,
                utc(before));
    }

    /** Chế độ ăn còn bản gán Hiệu lực hoặc Chờ hiệu lực (FR-002). */
    public boolean dietInUse(long dietId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM diet_assignment WHERE diet_id = ? AND status IN ('ACTIVE', 'PENDING_EFFECTIVE')",
                Integer.class,
                dietId);
        return n != null && n > 0;
    }

    /** Áp dụng: Hiệu lực ngay ({@code actualEffectiveAt} khác rỗng) hoặc Chờ hiệu lực; lý do, cảnh báo đặt một lần. */
    public void apply(
            long id,
            DietAssignmentStatus status,
            Instant actualEffectiveAt,
            String professionalReason,
            List<String> confirmedWarnings,
            long appliedBy,
            Instant at) {
        jdbc.update(
                "UPDATE diet_assignment SET status = ?, actual_effective_at = ?, professional_reason = ?,"
                        + " confirmed_warnings = ?, applied_by = ?, applied_at = ?, version = version + 1 WHERE id = ?",
                status.name(),
                utc(actualEffectiveAt),
                professionalReason,
                json.writeValueAsString(confirmedWarnings),
                appliedBy,
                utc(at),
                id);
    }

    /** Chờ hiệu lực tới thời điểm hiệu lực (Bộ lập lịch). */
    public void activate(long id, Instant actualEffectiveAt) {
        jdbc.update(
                "UPDATE diet_assignment SET status = 'ACTIVE', actual_effective_at = ?, version = version + 1"
                        + " WHERE id = ?",
                utc(actualEffectiveAt),
                id);
    }

    public void discontinue(long id, Instant at) {
        jdbc.update(
                "UPDATE diet_assignment SET status = 'DISCONTINUED', discontinued_at = ?, version = version + 1"
                        + " WHERE id = ?",
                utc(at),
                id);
    }

    /** {@code cancelledBy} rỗng khi hệ thống hủy theo trạng thái cuối (FR-015). */
    public void cancel(long id, String reason, Long cancelledBy, Instant at) {
        jdbc.update(
                "UPDATE diet_assignment SET status = 'CANCELLED', cancel_reason = ?, cancelled_by = ?,"
                        + " cancelled_at = ?, version = version + 1 WHERE id = ?",
                reason,
                cancelledBy,
                utc(at),
                id);
    }

    private DietAssignment row(ResultSet rs, int n) throws SQLException {
        return new DietAssignment(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("diet_id"),
                Texture.valueOf(rs.getString("texture")),
                json.readValue(rs.getString("personal_restrictions"), IDS),
                rs.getString("personal_instruction"),
                AssignmentBasis.valueOf(rs.getString("basis")),
                rs.getObject("basis_id", Long.class),
                rs.getObject("review_notice_id", Long.class),
                instant(rs.getObject("requested_effective_from", LocalDateTime.class)),
                instant(rs.getObject("actual_effective_at", LocalDateTime.class)),
                DietAssignmentStatus.valueOf(rs.getString("status")),
                rs.getString("professional_reason"),
                rs.getInt("version"));
    }

    private static LocalDateTime utc(Instant at) {
        return at == null ? null : LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    private static Instant instant(LocalDateTime at) {
        return at == null ? null : at.toInstant(ZoneOffset.UTC);
    }
}
