package com.cambae.backend.m02.internal.application.persistence;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.cambae.backend.m02.internal.application.domain.OpinionChoice;
import com.cambae.backend.m02.internal.application.domain.OpinionStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Lượt đăng ký và ý kiến về việc vào ở ở bảng {@code admission_application}, {@code admission_opinion} (data-model mục
 * 3). Cột sinh {@code open_resident_id} có {@code UNIQUE} nên tối đa một lượt mở mỗi người (FR-001); trigger chỉ cho
 * ý kiến đổi theo chiều Hiệu lực → Được thay thế và không cho sửa lượt đã đóng.
 */
@Repository
public class AdmissionApplicationStore {

    private static final String SELECT = "SELECT id, resident_id, application_date, contact_person, needs, note,"
            + " desired_residence_type, expected_care_level, desired_admission_date, status, opinion_at_admission_id,"
            + " version, created_by, created_at FROM admission_application";

    private static final String SELECT_OPINION = "SELECT id, application_id, opinion, source_type,"
            + " source_family_member_id, note, recorded_by, recorded_at, status FROM admission_opinion";

    private final JdbcTemplate jdbc;

    AdmissionApplicationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Thêm lượt Đang xử lý; trùng lượt mở của cùng người thì {@code DuplicateKeyException}. */
    public long insert(long residentId, AdmissionApplication.Content content, long createdBy, Instant createdAt) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO admission_application (resident_id, application_date, contact_person, needs,"
                                    + " note, desired_residence_type, expected_care_level, desired_admission_date,"
                                    + " status, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'IN_PROGRESS',"
                                    + " ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, residentId);
                    statement.setObject(2, content.applicationDate());
                    statement.setObject(3, content.contactPerson());
                    statement.setObject(4, content.needs());
                    statement.setObject(5, content.note());
                    statement.setString(6, content.desiredResidenceType().name());
                    statement.setString(7, content.expectedCareLevel().name());
                    statement.setObject(8, content.desiredAdmissionDate());
                    statement.setLong(9, createdBy);
                    statement.setObject(10, utc(createdAt));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<AdmissionApplication> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    /** Khóa dòng lượt trước khi sửa (sau khi đã khóa {@code resident_residency}). */
    public Optional<AdmissionApplication> lock(long id) {
        return jdbc.query(SELECT + " WHERE id = ? FOR UPDATE", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    /** Lượt mở (Đang xử lý hoặc Đang chờ) của người cao tuổi, nếu có. */
    public Optional<AdmissionApplication> findOpen(long residentId) {
        return jdbc.query(SELECT + " WHERE open_resident_id = ?", (rs, i) -> map(rs), residentId).stream()
                .findFirst();
    }

    public Optional<AdmissionApplication> lockOpen(long residentId) {
        return jdbc.query(SELECT + " WHERE open_resident_id = ? FOR UPDATE", (rs, i) -> map(rs), residentId).stream()
                .findFirst();
    }

    /** Lượt mới nhất của người cao tuổi (mở hoặc đã đóng). */
    public Optional<AdmissionApplication> findLatest(long residentId) {
        return jdbc
                .query(SELECT + " WHERE resident_id = ? ORDER BY id DESC LIMIT 1", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Danh sách theo id giảm dần, sau con trỏ {@code afterId} (id nhỏ hơn), lọc theo trạng thái nếu có. */
    public List<AdmissionApplication> list(Set<ApplicationStatus> statuses, Long afterId, int limit) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND status IN (")
                    .append(String.join(",", statuses.stream().map(s -> "?").toList()))
                    .append(")");
            statuses.forEach(s -> args.add(s.name()));
        }
        if (afterId != null) {
            sql.append(" AND id < ?");
            args.add(afterId);
        }
        sql.append(" ORDER BY id DESC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), (rs, i) -> map(rs), args.toArray());
    }

    /** Cập nhật nội dung khi {@code version} còn khớp; trả số dòng đổi (0 là đã có người sửa). */
    public int updateContent(long id, int version, AdmissionApplication.Content content) {
        return jdbc.update(
                "UPDATE admission_application SET application_date = ?, contact_person = ?, needs = ?, note = ?,"
                        + " desired_residence_type = ?, expected_care_level = ?, desired_admission_date = ?,"
                        + " version = version + 1 WHERE id = ? AND version = ?",
                content.applicationDate(),
                content.contactPerson(),
                content.needs(),
                content.note(),
                content.desiredResidenceType().name(),
                content.expectedCareLevel().name(),
                content.desiredAdmissionDate(),
                id,
                version);
    }

    /** Hoàn tất tiếp nhận: Đã tiếp nhận, kèm ý kiến Hiệu lực lúc đó (rỗng nếu chưa có). */
    public void markAdmitted(long id, Long opinionId) {
        jdbc.update(
                "UPDATE admission_application SET status = 'ADMITTED', opinion_at_admission_id = ?, version ="
                        + " version + 1 WHERE id = ?",
                opinionId,
                id);
    }

    /** Đưa vào danh sách chờ: lượt Đang xử lý thành Đang chờ (FR-007); tăng phiên bản để sửa đồng thời bị từ chối. */
    public void markWaiting(long id) {
        jdbc.update(
                "UPDATE admission_application SET status = 'WAITING', version = version + 1 WHERE id = ? AND status ="
                        + " 'IN_PROGRESS'",
                id);
    }

    public void markCancelled(long id) {
        jdbc.update("UPDATE admission_application SET status = 'CANCELLED', version = version + 1 WHERE id = ?", id);
    }

    public long insertOpinion(
            long applicationId,
            OpinionChoice opinion,
            OpinionSource source,
            String note,
            long recordedBy,
            Instant recordedAt) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO admission_opinion (application_id, opinion, source_type,"
                                    + " source_family_member_id, note, recorded_by, recorded_at, status) VALUES (?, ?,"
                                    + " ?, ?, ?, ?, ?, 'ACTIVE')",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, applicationId);
                    statement.setString(2, opinion.name());
                    statement.setString(3, source.type().name());
                    statement.setObject(4, source.familyMemberId());
                    statement.setObject(5, note);
                    statement.setLong(6, recordedBy);
                    statement.setObject(7, utc(recordedAt));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<AdmissionOpinion> findOpinion(long id) {
        return jdbc.query(SELECT_OPINION + " WHERE id = ?", (rs, i) -> mapOpinion(rs), id).stream()
                .findFirst();
    }

    /** Lượt còn mở (Đang xử lý hoặc Đang chờ), cho việc nhắc hằng ngày. */
    public List<AdmissionApplication> listOpen() {
        return jdbc.query(SELECT + " WHERE open_resident_id IS NOT NULL ORDER BY id", (rs, i) -> map(rs));
    }

    public Optional<AdmissionOpinion> findActiveOpinion(long applicationId) {
        return jdbc
                .query(
                        SELECT_OPINION + " WHERE application_id = ? AND status = 'ACTIVE'",
                        (rs, i) -> mapOpinion(rs),
                        applicationId)
                .stream()
                .findFirst();
    }

    /** Ý kiến Hiệu lực trước, rồi các ý kiến đã bị thay thế từ mới tới cũ (FR-001). */
    public List<AdmissionOpinion> opinions(long applicationId) {
        return jdbc.query(
                SELECT_OPINION + " WHERE application_id = ? ORDER BY status = 'ACTIVE' DESC, id DESC",
                (rs, i) -> mapOpinion(rs),
                applicationId);
    }

    /** Chuyển ý kiến Hiệu lực sang Được thay thế trước khi thêm ý kiến mới (chỉ một ý kiến Hiệu lực mỗi lượt). */
    public int supersedeActive(long applicationId) {
        return jdbc.update(
                "UPDATE admission_opinion SET status = 'SUPERSEDED' WHERE application_id = ? AND status = 'ACTIVE'",
                applicationId);
    }

    /** Điền tham chiếu từ ý kiến cũ sang ý kiến thay thế, đúng một lần. */
    public void supersededBy(long applicationId, long newOpinionId) {
        jdbc.update(
                "UPDATE admission_opinion SET superseded_by_id = ? WHERE application_id = ? AND status ="
                        + " 'SUPERSEDED' AND superseded_by_id IS NULL AND id <> ?",
                newOpinionId,
                applicationId,
                newOpinionId);
    }

    private AdmissionApplication map(ResultSet rs) throws SQLException {
        return new AdmissionApplication(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getObject("application_date", LocalDate.class),
                rs.getString("contact_person"),
                rs.getString("needs"),
                ResidenceType.valueOf(rs.getString("desired_residence_type")),
                CareLevel.valueOf(rs.getString("expected_care_level")),
                rs.getObject("desired_admission_date", LocalDate.class),
                rs.getString("note"),
                ApplicationStatus.valueOf(rs.getString("status")),
                rs.getObject("opinion_at_admission_id", Long.class),
                rs.getInt("version"),
                rs.getLong("created_by"),
                rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }

    private AdmissionOpinion mapOpinion(ResultSet rs) throws SQLException {
        return new AdmissionOpinion(
                rs.getLong("id"),
                rs.getLong("application_id"),
                OpinionChoice.valueOf(rs.getString("opinion")),
                new OpinionSource(
                        OpinionSourceType.valueOf(rs.getString("source_type")),
                        rs.getObject("source_family_member_id", Long.class)),
                rs.getString("note"),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                OpinionStatus.valueOf(rs.getString("status")));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
