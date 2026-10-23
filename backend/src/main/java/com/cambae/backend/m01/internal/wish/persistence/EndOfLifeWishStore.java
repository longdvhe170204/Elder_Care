package com.cambae.backend.m01.internal.wish.persistence;

import com.cambae.backend.m01.api.dto.CourtDecisionRef;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.dto.RepresentativeSelection;
import com.cambae.backend.m01.api.dto.WishChoice;
import com.cambae.backend.m01.api.dto.WishOpinion;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import com.cambae.backend.m01.api.dto.WishSignerType;
import com.cambae.backend.m01.internal.wish.domain.EndOfLifeWish;
import com.cambae.backend.m01.internal.wish.domain.RecordedOpinion;
import com.cambae.backend.m01.internal.wish.domain.WishStatus;
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
 * Phiếu nguyện vọng cuối đời ở bảng {@code end_of_life_wish} và {@code end_of_life_wish_opinion} (data-model mục 6).
 * Cột sinh {@code active_resident_id} có {@code UNIQUE} nên tối đa một phiên bản Hiệu lực mỗi người cao tuổi (DBR-34);
 * trigger chỉ cho đổi theo chiều Hiệu lực → Được thay thế.
 */
@Repository
public class EndOfLifeWishStore {

    private static final String SELECT = "SELECT id, resident_id, choice, first_contact, note, respondent_type,"
            + " respondent_family_member_id, relationship, assessing_doctor_id, can_self_determine, assessed_at,"
            + " support_method, review_date, signer_type, signer_family_member_id, representative_selection,"
            + " court_decision_number, court_decision_date, court_decision_court, court_decision_file_id, signed_at,"
            + " signed_copy_file_id, source, status, recorded_by, recorded_at FROM end_of_life_wish";

    private final JdbcTemplate jdbc;

    EndOfLifeWishStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dữ liệu một phiên bản mới; {@code recordedAt} và {@code assessedAt} từ đồng hồ nghiệp vụ. */
    public record NewWish(
            long residentId,
            WishChoice choice,
            String firstContact,
            String note,
            WishSignerType respondentType,
            Long respondentFamilyMemberId,
            String relationship,
            long assessingDoctorId,
            boolean canSelfDetermine,
            Instant assessedAt,
            String supportMethod,
            LocalDate reviewDate,
            WishSignerType signerType,
            Long signerFamilyMemberId,
            RepresentativeSelection representativeSelection,
            CourtDecisionRef courtDecision,
            Instant signedAt,
            long signedCopyFileId,
            WishRecordSource source,
            long recordedBy,
            Instant recordedAt) {}

    /** Thêm phiên bản Hiệu lực cùng các ý kiến khác; trả id. */
    public long insert(NewWish wish, List<WishOpinion> opinions) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO end_of_life_wish (resident_id, choice, first_contact, note, respondent_type,"
                                    + " respondent_family_member_id, relationship, assessing_doctor_id,"
                                    + " can_self_determine, assessed_at, support_method, review_date, signer_type,"
                                    + " signer_family_member_id, representative_selection, court_decision_number,"
                                    + " court_decision_date, court_decision_court, court_decision_file_id, signed_at,"
                                    + " signed_copy_file_id, source, status, recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
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
        for (WishOpinion opinion : opinions) {
            OpinionSource source = opinion.source();
            jdbc.update(
                    "INSERT INTO end_of_life_wish_opinion (wish_id, source_type, source_family_member_id, content,"
                            + " recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?)",
                    id,
                    source.type().name(),
                    source.familyMemberId(),
                    opinion.content(),
                    wish.recordedBy(),
                    utc(wish.recordedAt()));
        }
        return id;
    }

    public Optional<EndOfLifeWish> find(long id) {
        return jdbc.query(SELECT + " WHERE id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst();
    }

    public Optional<EndOfLifeWish> findActive(long residentId) {
        return jdbc
                .query(SELECT + " WHERE resident_id = ? AND status = 'ACTIVE'", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Lịch sử phiếu của người cao tuổi, mới nhất trước (FR-023b). */
    public List<EndOfLifeWish> listByResident(long residentId) {
        return jdbc.query(SELECT + " WHERE resident_id = ? ORDER BY id DESC", (rs, i) -> map(rs), residentId);
    }

    /** Chuyển phiên bản Hiệu lực sang Được thay thế; {@link #supersededBy} điền bản mới sau khi bản mới được thêm. */
    public int supersede(long id) {
        return jdbc.update("UPDATE end_of_life_wish SET status = 'SUPERSEDED' WHERE id = ? AND status = 'ACTIVE'", id);
    }

    public void supersededBy(long id, long newId) {
        jdbc.update("UPDATE end_of_life_wish SET superseded_by_id = ? WHERE id = ?", newId, id);
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
                opinions(id),
                rs.getObject("signed_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                rs.getLong("signed_copy_file_id"),
                WishRecordSource.valueOf(rs.getString("source")),
                WishStatus.valueOf(rs.getString("status")),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }

    private List<RecordedOpinion> opinions(long wishId) {
        return jdbc.query(
                "SELECT source_type, source_family_member_id, content, recorded_by, recorded_at"
                        + " FROM end_of_life_wish_opinion WHERE wish_id = ? ORDER BY id",
                (rs, i) -> new RecordedOpinion(
                        new OpinionSource(
                                OpinionSourceType.valueOf(rs.getString("source_type")),
                                rs.getObject("source_family_member_id", Long.class)),
                        rs.getString("content"),
                        rs.getLong("recorded_by"),
                        rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                wishId);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
