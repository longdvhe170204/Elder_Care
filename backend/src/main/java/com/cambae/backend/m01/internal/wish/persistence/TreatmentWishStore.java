package com.cambae.backend.m01.internal.wish.persistence;

import com.cambae.backend.m01.api.dto.HealthInsurance;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.dto.PreferredHospital;
import com.cambae.backend.m01.api.dto.TreatmentEscort;
import com.cambae.backend.m01.api.dto.WishOpinion;
import com.cambae.backend.m01.internal.wish.domain.RecordedOpinion;
import com.cambae.backend.m01.internal.wish.domain.TreatmentWish;
import com.cambae.backend.m01.internal.wish.domain.TreatmentWishStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Phiếu nguyện vọng khi điều trị ở bảng {@code treatment_wish}, {@code treatment_wish_notify},
 * {@code treatment_wish_opinion} (data-model mục 6b). Cột sinh {@code active_resident_id} có {@code UNIQUE} nên tối đa
 * một phiên bản Hiệu lực mỗi người cao tuổi (DBR-39); trigger chỉ cho đổi theo chiều Hiệu lực → Hết hiệu lực.
 */
@Repository
public class TreatmentWishStore {

    private static final String SELECT = "SELECT id, resident_id, hospital1_name, hospital1_address, hospital2_name,"
            + " hospital2_address, insurance_registration_place, insurance_card_number, insurance_card_expiry,"
            + " ask_family_first, escort, note, respondent_type, respondent_family_member_id, signed_copy_file_id,"
            + " status, recorded_by, recorded_at FROM treatment_wish";

    private final JdbcTemplate jdbc;

    TreatmentWishStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Dữ liệu một phiên bản mới. */
    public record NewWish(
            long residentId,
            List<PreferredHospital> preferredHospitals,
            HealthInsurance healthInsurance,
            boolean askFamilyFirst,
            List<Long> notifyFamilyMemberIds,
            TreatmentEscort accompaniedBy,
            String note,
            OpinionSource respondent,
            Long signedCopyFileId,
            long recordedBy,
            Instant recordedAt) {}

    /** Thêm phiên bản Hiệu lực cùng người cần báo và các ý kiến khác; trả id. */
    public long insert(NewWish wish, List<WishOpinion> opinions) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO treatment_wish (resident_id, hospital1_name, hospital1_address,"
                                    + " hospital2_name, hospital2_address, insurance_registration_place,"
                                    + " insurance_card_number, insurance_card_expiry, ask_family_first, escort, note,"
                                    + " respondent_type, respondent_family_member_id, signed_copy_file_id, status,"
                                    + " recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    PreferredHospital first = wish.preferredHospitals().get(0);
                    PreferredHospital second = wish.preferredHospitals().size() > 1
                            ? wish.preferredHospitals().get(1)
                            : null;
                    HealthInsurance insurance = wish.healthInsurance();
                    statement.setLong(1, wish.residentId());
                    statement.setString(2, first.name());
                    statement.setString(3, first.address());
                    statement.setObject(4, second == null ? null : second.name());
                    statement.setObject(5, second == null ? null : second.address());
                    statement.setObject(6, insurance == null ? null : insurance.initialRegistrationPlace());
                    statement.setObject(7, insurance == null ? null : insurance.cardNumber());
                    statement.setObject(8, insurance == null ? null : insurance.cardExpiry());
                    statement.setBoolean(9, wish.askFamilyFirst());
                    statement.setString(10, wish.accompaniedBy().name());
                    statement.setObject(11, wish.note());
                    statement.setString(12, wish.respondent().type().name());
                    statement.setObject(13, wish.respondent().familyMemberId());
                    statement.setObject(14, wish.signedCopyFileId());
                    statement.setLong(15, wish.recordedBy());
                    statement.setObject(16, utc(wish.recordedAt()));
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        for (long familyMemberId : wish.notifyFamilyMemberIds()) {
            jdbc.update(
                    "INSERT INTO treatment_wish_notify (treatment_wish_id, family_member_id) VALUES (?, ?)",
                    id,
                    familyMemberId);
        }
        for (WishOpinion opinion : opinions) {
            jdbc.update(
                    "INSERT INTO treatment_wish_opinion (treatment_wish_id, source_type, source_family_member_id,"
                            + " content, recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?)",
                    id,
                    opinion.source().type().name(),
                    opinion.source().familyMemberId(),
                    opinion.content(),
                    wish.recordedBy(),
                    utc(wish.recordedAt()));
        }
        return id;
    }

    public Optional<TreatmentWish> findActive(long residentId) {
        return jdbc
                .query(SELECT + " WHERE resident_id = ? AND status = 'ACTIVE'", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Lịch sử phiếu của người cao tuổi, mới nhất trước (FR-023k). */
    public List<TreatmentWish> listByResident(long residentId) {
        return jdbc.query(SELECT + " WHERE resident_id = ? ORDER BY id DESC", (rs, i) -> map(rs), residentId);
    }

    public Optional<Long> residentIdOf(long id) {
        return jdbc.queryForList("SELECT resident_id FROM treatment_wish WHERE id = ?", Long.class, id).stream()
                .findFirst();
    }

    public boolean existsActive(long residentId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM treatment_wish WHERE resident_id = ? AND status = 'ACTIVE'",
                Integer.class,
                residentId);
        return count != null && count > 0;
    }

    /** Chuyển phiên bản Hiệu lực sang Hết hiệu lực; {@link #supersededBy} điền bản mới sau khi bản mới được thêm. */
    public int expire(long id) {
        return jdbc.update("UPDATE treatment_wish SET status = 'EXPIRED' WHERE id = ? AND status = 'ACTIVE'", id);
    }

    public void supersededBy(long id, long newId) {
        jdbc.update("UPDATE treatment_wish SET superseded_by_id = ? WHERE id = ?", newId, id);
    }

    private TreatmentWish map(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        List<PreferredHospital> hospitals = new ArrayList<>();
        hospitals.add(new PreferredHospital(rs.getString("hospital1_name"), rs.getString("hospital1_address")));
        if (rs.getString("hospital2_name") != null) {
            hospitals.add(new PreferredHospital(rs.getString("hospital2_name"), rs.getString("hospital2_address")));
        }
        boolean hasInsurance = rs.getString("insurance_registration_place") != null
                || rs.getString("insurance_card_number") != null
                || rs.getObject("insurance_card_expiry") != null;
        HealthInsurance insurance = hasInsurance
                ? new HealthInsurance(
                        rs.getString("insurance_registration_place"),
                        rs.getString("insurance_card_number"),
                        rs.getObject("insurance_card_expiry", LocalDate.class))
                : null;
        return new TreatmentWish(
                id,
                rs.getLong("resident_id"),
                List.copyOf(hospitals),
                insurance,
                rs.getBoolean("ask_family_first"),
                jdbc.queryForList(
                        "SELECT family_member_id FROM treatment_wish_notify WHERE treatment_wish_id = ?"
                                + " ORDER BY family_member_id",
                        Long.class,
                        id),
                TreatmentEscort.valueOf(rs.getString("escort")),
                rs.getString("note"),
                new OpinionSource(
                        OpinionSourceType.valueOf(rs.getString("respondent_type")),
                        rs.getObject("respondent_family_member_id", Long.class)),
                opinions(id),
                rs.getObject("signed_copy_file_id", Long.class),
                TreatmentWishStatus.valueOf(rs.getString("status")),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }

    private List<RecordedOpinion> opinions(long wishId) {
        return jdbc.query(
                "SELECT source_type, source_family_member_id, content, recorded_by, recorded_at"
                        + " FROM treatment_wish_opinion WHERE treatment_wish_id = ? ORDER BY id",
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
