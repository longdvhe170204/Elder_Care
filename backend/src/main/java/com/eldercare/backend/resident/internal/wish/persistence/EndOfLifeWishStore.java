package com.eldercare.backend.resident.internal.wish.persistence;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.NewEndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.WishReviewDue;
import com.eldercare.backend.resident.internal.wish.domain.WishStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Phiếu nguyện vọng cuối đời: dòng {@code kind = END_OF_LIFE} của bảng {@code wish}, ý kiến khác ở {@link WishOpinionStore}
 * (data-model 001 mục 0.1). Cột sinh {@code active_key} có {@code UNIQUE} nên tối đa một phiên bản Hiệu lực mỗi người cao
 * tuổi (DBR-34); trigger chỉ cho đổi theo chiều Hiệu lực → Được thay thế.
 */
@Repository
public class EndOfLifeWishStore {

    private static final String SELECT = "SELECT id, resident_id, choice, first_contact, note, respondent_type,"
            + " respondent_family_member_id, relationship, assessing_doctor_id, can_self_determine, assessed_at,"
            + " support_method, review_date, signer_type, signer_family_member_id, representative_selection,"
            + " court_decision_number, court_decision_date, court_decision_court, court_decision_file_id, signed_at,"
            + " signed_copy_file_id, source, status, recorded_by, recorded_at FROM wish WHERE kind = 'END_OF_LIFE'";

    private static final String WISH_TYPE = "M01.END_OF_LIFE_WISH";

    private final JdbcTemplate jdbc;
    private final WishOpinionStore opinions;

    EndOfLifeWishStore(JdbcTemplate jdbc, WishOpinionStore opinions) {
        this.jdbc = jdbc;
        this.opinions = opinions;
    }

    /** Phiếu Hiệu lực tới ngày xem lại của hồ sơ chưa ở trạng thái cuối, chưa có dòng nhắc {@code reminderType}. */
    public List<WishReviewDue> dueReviews(LocalDate today, String reminderType) {
        return jdbc.query(
                "SELECT w.resident_id, w.id, w.assessing_doctor_id FROM wish w JOIN resident s ON s.id = w.resident_id"
                        + " WHERE w.kind = 'END_OF_LIFE' AND w.status = 'ACTIVE' AND w.review_date <= ?"
                        + " AND s.status NOT IN ('RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED')"
                        + " AND NOT EXISTS (SELECT 1 FROM reminder_marker r WHERE r.kind = ?"
                        + " AND r.entity_type = 'M01.RESIDENT' AND r.entity_id = w.resident_id"
                        + " AND r.marker = CONCAT('START:', w.id))",
                (rs, i) -> new WishReviewDue(rs.getLong(1), rs.getLong(2), rs.getLong(3)),
                today,
                "M01.PERIODIC_REMINDER." + reminderType);
    }

    /** Thêm phiên bản Hiệu lực cùng các ý kiến khác; trả id. */
    public long insert(NewEndOfLifeWish wish, List<WishOpinion> opinions) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO wish (kind, resident_id, choice, first_contact, note, respondent_type,"
                                    + " respondent_family_member_id, relationship, assessing_doctor_id,"
                                    + " can_self_determine, assessed_at, support_method, review_date, signer_type,"
                                    + " signer_family_member_id, representative_selection, court_decision_number,"
                                    + " court_decision_date, court_decision_court, court_decision_file_id, signed_at,"
                                    + " signed_copy_file_id, source, status, recorded_by, recorded_at)"
                                    + " VALUES ('END_OF_LIFE', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                                    + " 'ACTIVE', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    CourtDecisionRef court = wish.courtDecision();
                    statement.setLong(1, wish.residentId());
                    statement.setString(2, wish.choice().name());
                    statement.setString(3, wish.firstContact());
                    statement.setObject(4, wish.note());
                    statement.setString(5, wish.respondentType().name());
                    statement.setObject(6, wish.respondentFamilyMemberId());
                    statement.setObject(7, wish.relationship());
                    statement.setLong(8, wish.assessingDoctorId());
                    statement.setBoolean(9, wish.canSelfDetermine());
                    statement.setObject(10, utc(wish.assessedAt()));
                    statement.setString(11, wish.supportMethod());
                    statement.setObject(12, wish.reviewDate());
                    statement.setString(13, wish.signerType().name());
                    statement.setObject(14, wish.signerFamilyMemberId());
                    statement.setObject(
                            15,
                            wish.representativeSelection() == null
                                    ? null
                                    : wish.representativeSelection().name());
                    statement.setObject(16, court == null ? null : court.number());
                    statement.setObject(17, court == null ? null : court.date());
                    statement.setObject(18, court == null ? null : court.court());
                    statement.setObject(19, court == null ? null : court.fileId());
                    statement.setObject(20, utc(wish.signedAt()));
                    statement.setLong(21, wish.signedCopyFileId());
                    statement.setString(22, wish.source().name());
                    statement.setLong(23, wish.recordedBy());
                    statement.setObject(24, utc(wish.recordedAt()));
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        this.opinions.add(WISH_TYPE, id, opinions, wish.recordedBy(), wish.recordedAt());
        return id;
    }

    public Optional<EndOfLifeWish> find(long id) {
        return jdbc.query(SELECT + " AND id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    public Optional<EndOfLifeWish> findActive(long residentId) {
        return jdbc
                .query(SELECT + " AND resident_id = ? AND status = 'ACTIVE'", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Lịch sử phiếu của người cao tuổi, mới nhất trước (FR-023b). */
    public List<EndOfLifeWish> listByResident(long residentId) {
        return jdbc.query(SELECT + " AND resident_id = ? ORDER BY id DESC", (rs, i) -> map(rs), residentId);
    }

    /** Chuyển phiên bản Hiệu lực sang Được thay thế; {@link #supersededBy} điền bản mới sau khi bản mới được thêm. */
    public int supersede(long id) {
        return jdbc.update(
                "UPDATE wish SET status = 'SUPERSEDED' WHERE id = ? AND kind = 'END_OF_LIFE' AND status = 'ACTIVE'",
                id);
    }

    public void supersededBy(long id, long newId) {
        jdbc.update("UPDATE wish SET superseded_by_id = ? WHERE id = ?", newId, id);
    }

    private EndOfLifeWish map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        CourtDecisionRef court = rs.getString("court_decision_number") == null
                ? null
                : new CourtDecisionRef(
                        rs.getString("court_decision_number"),
                        rs.getObject("court_decision_date", LocalDate.class),
                        rs.getString("court_decision_court"),
                        rs.getObject("court_decision_file_id", Long.class));
        String selection = rs.getString("representative_selection");
        return new EndOfLifeWish(
                id,
                rs.getLong("resident_id"),
                WishChoice.valueOf(rs.getString("choice")),
                rs.getString("first_contact"),
                rs.getString("note"),
                WishSignerType.valueOf(rs.getString("respondent_type")),
                rs.getObject("respondent_family_member_id", Long.class),
                rs.getString("relationship"),
                rs.getLong("assessing_doctor_id"),
                rs.getBoolean("can_self_determine"),
                rs.getObject("assessed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getString("support_method"),
                rs.getObject("review_date", LocalDate.class),
                WishSignerType.valueOf(rs.getString("signer_type")),
                rs.getObject("signer_family_member_id", Long.class),
                selection == null ? null : RepresentativeSelection.valueOf(selection),
                court,
                opinions.list(WISH_TYPE, id),
                rs.getObject("signed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getLong("signed_copy_file_id"),
                WishRecordSource.valueOf(rs.getString("source")),
                WishStatus.valueOf(rs.getString("status")),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
