package com.eldercare.backend.resident.internal.wish.persistence;

import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.internal.wish.domain.NewTreatmentWish;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWish;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishStatus;
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
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Phiếu nguyện vọng khi điều trị: dòng {@code kind = TREATMENT} của bảng {@code wish} (người cần báo ở cột JSON {@code
 * notify_family_member_ids}, ý kiến khác ở {@link WishOpinionStore}; data-model 001 mục 0.1). Cột sinh {@code active_key}
 * có {@code UNIQUE} nên tối đa một phiên bản Hiệu lực mỗi người cao tuổi (DBR-39); trigger chỉ cho đổi theo chiều Hiệu
 * lực → Hết hiệu lực.
 */
@Repository
public class TreatmentWishStore {

    private static final String SELECT = "SELECT id, resident_id, hospital1_name, hospital1_address, hospital2_name,"
            + " hospital2_address, insurance_registration_place, insurance_card_number, insurance_card_expiry,"
            + " ask_family_first, escort, note, respondent_type, respondent_family_member_id, signed_copy_file_id,"
            + " status, recorded_by, recorded_at FROM wish WHERE kind = 'TREATMENT'";

    private static final String WISH_TYPE = "M01.TREATMENT_WISH";

    private final JdbcTemplate jdbc;
    private final WishOpinionStore opinions;

    TreatmentWishStore(JdbcTemplate jdbc, WishOpinionStore opinions) {
        this.jdbc = jdbc;
        this.opinions = opinions;
    }

    /** Thêm phiên bản Hiệu lực cùng người cần báo và các ý kiến khác; trả id. */
    public long insert(NewTreatmentWish wish, List<WishOpinion> opinions) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO wish (kind, resident_id, hospital1_name, hospital1_address,"
                                    + " hospital2_name, hospital2_address, insurance_registration_place,"
                                    + " insurance_card_number, insurance_card_expiry, ask_family_first, escort, note,"
                                    + " respondent_type, respondent_family_member_id, signed_copy_file_id, status,"
                                    + " recorded_by, recorded_at, notify_family_member_ids)"
                                    + " VALUES ('TREATMENT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?,"
                                    + " CAST(? AS JSON))",
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
                    statement.setString(
                            17,
                            wish.notifyFamilyMemberIds().stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(",", "[", "]")));
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        this.opinions.add(WISH_TYPE, id, opinions, wish.recordedBy(), wish.recordedAt());
        return id;
    }

    public Optional<TreatmentWish> findActive(long residentId) {
        return jdbc
                .query(SELECT + " AND resident_id = ? AND status = 'ACTIVE'", (rs, i) -> map(rs), residentId)
                .stream()
                .findFirst();
    }

    /** Lịch sử phiếu của người cao tuổi, mới nhất trước (FR-023k). */
    public List<TreatmentWish> listByResident(long residentId) {
        return jdbc.query(SELECT + " AND resident_id = ? ORDER BY id DESC", (rs, i) -> map(rs), residentId);
    }

    public Optional<Long> residentIdOf(long id) {
        return jdbc
                .queryForList("SELECT resident_id FROM wish WHERE id = ? AND kind = 'TREATMENT'", Long.class, id)
                .stream()
                .findFirst();
    }

    public boolean existsActive(long residentId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM wish WHERE kind = 'TREATMENT' AND resident_id = ? AND status = 'ACTIVE'",
                Integer.class,
                residentId);
        return count != null && count > 0;
    }

    /** Chuyển phiên bản Hiệu lực sang Hết hiệu lực; {@link #supersededBy} điền bản mới sau khi bản mới được thêm. */
    public int expire(long id) {
        return jdbc.update(
                "UPDATE wish SET status = 'EXPIRED' WHERE id = ? AND kind = 'TREATMENT' AND status = 'ACTIVE'", id);
    }

    public void supersededBy(long id, long newId) {
        jdbc.update("UPDATE wish SET superseded_by_id = ? WHERE id = ?", newId, id);
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
                        "SELECT j.id FROM wish w, JSON_TABLE(w.notify_family_member_ids, '$[*]' COLUMNS (id BIGINT PATH '$')) j"
                                + " WHERE w.id = ? ORDER BY j.id",
                        Long.class,
                        id),
                TreatmentEscort.valueOf(rs.getString("escort")),
                rs.getString("note"),
                new OpinionSource(
                        OpinionSourceType.valueOf(rs.getString("respondent_type")),
                        rs.getObject("respondent_family_member_id", Long.class)),
                opinions.list(WISH_TYPE, id),
                rs.getObject("signed_copy_file_id", Long.class),
                TreatmentWishStatus.valueOf(rs.getString("status")),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
