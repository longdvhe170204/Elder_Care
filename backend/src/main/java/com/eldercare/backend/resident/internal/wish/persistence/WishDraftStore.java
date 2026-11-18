package com.eldercare.backend.resident.internal.wish.persistence;

import com.eldercare.backend.resident.api.dto.CourtDecisionRef;
import com.eldercare.backend.resident.api.dto.HealthInsurance;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.PreferredHospital;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.TreatmentEscort;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishOpinion;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.wish.domain.DraftDiscard;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentDraftContent;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.WishOpinionContent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bản Nháp tự khai của phiếu nguyện vọng (001 T111; Q-412, BR-M01-18): dòng {@code status = DRAFT} của bảng {@code wish}
 * (data-model 0.1). Cột sinh {@code draft_key} có {@code UNIQUE} nên tối đa một Nháp mỗi loại mỗi người; ý kiến khác của
 * Nháp ở cột {@code draft_opinions}, khi kiểm chứng chép sang {@code record_event} ({@link WishOpinionStore}). Kiểm chứng
 * do {@link EndOfLifeWishStore#activateDraft}, {@link TreatmentWishStore#activateDraft} làm.
 */
@Repository
public class WishDraftStore {

    private static final String END_OF_LIFE = "END_OF_LIFE";
    private static final String TREATMENT = "TREATMENT";

    private static final String SELECT = "SELECT id, resident_id, status, choice, first_contact, note, respondent_type,"
            + " respondent_family_member_id, relationship, signer_type, signer_family_member_id,"
            + " representative_selection, court_decision_number, court_decision_date, court_decision_court,"
            + " court_decision_file_id, signed_at, signed_copy_file_id, hospital1_name, hospital1_address,"
            + " hospital2_name, hospital2_address, insurance_registration_place, insurance_card_number,"
            + " insurance_card_expiry, ask_family_first, escort, notify_family_member_ids, draft_opinions, recorded_by,"
            + " recorded_at, discarded_by, discarded_at, discard_reason FROM wish WHERE kind = ?";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    WishDraftStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Nháp đang mở của phiếu cuối đời. */
    public Optional<EndOfLifeWishDraft> findEndOfLife(long residentId) {
        return jdbc
                .query(
                        SELECT + " AND resident_id = ? AND status = 'DRAFT'",
                        (rs, i) -> endOfLife(rs),
                        END_OF_LIFE,
                        residentId)
                .stream()
                .findFirst();
    }

    public Optional<EndOfLifeWishDraft> findEndOfLifeById(long id) {
        return jdbc
                .query(
                        SELECT + " AND id = ? AND status IN ('DRAFT', 'DISCARDED')",
                        (rs, i) -> endOfLife(rs),
                        END_OF_LIFE,
                        id)
                .stream()
                .findFirst();
    }

    /** Nháp đang mở của phiếu khi điều trị. */
    public Optional<TreatmentWishDraft> findTreatment(long residentId) {
        return jdbc
                .query(
                        SELECT + " AND resident_id = ? AND status = 'DRAFT'",
                        (rs, i) -> treatment(rs),
                        TREATMENT,
                        residentId)
                .stream()
                .findFirst();
    }

    public Optional<TreatmentWishDraft> findTreatmentById(long id) {
        return jdbc
                .query(
                        SELECT + " AND id = ? AND status IN ('DRAFT', 'DISCARDED')",
                        (rs, i) -> treatment(rs),
                        TREATMENT,
                        id)
                .stream()
                .findFirst();
    }

    /** Có Nháp đang mở của loại nào cho người cao tuổi (cảnh báo, nhắc FR-023r). */
    public List<String> openDraftKinds(long residentId) {
        return jdbc.queryForList(
                "SELECT kind FROM wish WHERE resident_id = ? AND status = 'DRAFT' ORDER BY kind",
                String.class,
                residentId);
    }

    /** Người lập Nháp đang mở có id {@code wishId} (quyền tải tệp gắn vào Nháp). */
    public Optional<Long> draftWriterOf(long wishId) {
        return jdbc
                .queryForList("SELECT recorded_by FROM wish WHERE id = ? AND status = 'DRAFT'", Long.class, wishId)
                .stream()
                .findFirst();
    }

    public long insertEndOfLife(long residentId, EndOfLifeDraftContent content, long recordedBy, Instant recordedAt) {
        return insert(END_OF_LIFE, residentId, endOfLifeColumns(content), recordedBy, recordedAt);
    }

    public void updateEndOfLife(long id, EndOfLifeDraftContent content) {
        update(id, endOfLifeColumns(content));
    }

    public long insertTreatment(long residentId, TreatmentDraftContent content, long recordedBy, Instant recordedAt) {
        return insert(TREATMENT, residentId, treatmentColumns(content), recordedBy, recordedAt);
    }

    public void updateTreatment(long id, TreatmentDraftContent content) {
        update(id, treatmentColumns(content));
    }

    /** Nháp → Đã hủy; trả số dòng đổi (0 khi không còn Nháp). */
    public int discard(long id, long by, Instant at, String reason) {
        return jdbc.update(
                "UPDATE wish SET status = 'DISCARDED', discarded_by = ?, discarded_at = ?, discard_reason = ?,"
                        + " draft_opinions = NULL WHERE id = ? AND status = 'DRAFT'",
                by,
                utc(at),
                reason,
                id);
    }

    private long insert(
            String kind, long residentId, Map<String, Object> columns, long recordedBy, Instant recordedAt) {
        List<String> names = new ArrayList<>(columns.keySet());
        String placeholders = names.stream().map(WishDraftStore::placeholder).collect(Collectors.joining(", "));
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO wish (kind, resident_id, status, recorded_by, recorded_at, "
                                    + String.join(", ", names)
                                    + ") VALUES (?, ?, 'DRAFT', ?, ?, "
                                    + placeholders
                                    + ")",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, kind);
                    statement.setLong(2, residentId);
                    statement.setLong(3, recordedBy);
                    statement.setObject(4, utc(recordedAt));
                    int index = 5;
                    for (String name : names) {
                        statement.setObject(index++, columns.get(name));
                    }
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    private void update(long id, Map<String, Object> columns) {
        List<String> names = new ArrayList<>(columns.keySet());
        List<Object> values = new ArrayList<>();
        for (String name : names) {
            values.add(columns.get(name));
        }
        values.add(id);
        jdbc.update(
                "UPDATE wish SET "
                        + names.stream().map(n -> n + " = " + placeholder(n)).collect(Collectors.joining(", "))
                        + " WHERE id = ? AND status = 'DRAFT'",
                values.toArray());
    }

    private static String placeholder(String column) {
        return column.equals("notify_family_member_ids") || column.equals("draft_opinions") ? "CAST(? AS JSON)" : "?";
    }

    private Map<String, Object> endOfLifeColumns(EndOfLifeDraftContent c) {
        CourtDecisionRef court = c.courtDecision();
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("choice", name(c.choice()));
        columns.put("first_contact", blankToNull(c.firstContact()));
        columns.put("note", blankToNull(c.note()));
        columns.put("respondent_type", name(c.respondentType()));
        columns.put("respondent_family_member_id", c.respondentFamilyMemberId());
        columns.put("relationship", blankToNull(c.relationship()));
        columns.put("signer_type", name(c.signerType()));
        columns.put("signer_family_member_id", c.signerFamilyMemberId());
        columns.put("representative_selection", name(c.representativeSelection()));
        columns.put("court_decision_number", court == null ? null : blankToNull(court.number()));
        columns.put("court_decision_date", court == null ? null : court.date());
        columns.put("court_decision_court", court == null ? null : blankToNull(court.court()));
        columns.put("court_decision_file_id", court == null ? null : court.fileId());
        columns.put("signed_at", c.signedAt() == null ? null : utc(c.signedAt()));
        columns.put("signed_copy_file_id", c.signedCopyFileId());
        columns.put("draft_opinions", opinionsJson(c.otherOpinions()));
        return columns;
    }

    private Map<String, Object> treatmentColumns(TreatmentDraftContent c) {
        List<PreferredHospital> hospitals = c.preferredHospitals() == null ? List.of() : c.preferredHospitals();
        PreferredHospital first = hospitals.isEmpty() ? null : hospitals.get(0);
        PreferredHospital second = hospitals.size() > 1 ? hospitals.get(1) : null;
        HealthInsurance insurance = c.healthInsurance();
        Map<String, Object> columns = new LinkedHashMap<>();
        columns.put("hospital1_name", first == null ? null : blankToNull(first.name()));
        columns.put("hospital1_address", first == null ? null : blankToNull(first.address()));
        columns.put("hospital2_name", second == null ? null : blankToNull(second.name()));
        columns.put("hospital2_address", second == null ? null : blankToNull(second.address()));
        columns.put(
                "insurance_registration_place",
                insurance == null ? null : blankToNull(insurance.initialRegistrationPlace()));
        columns.put("insurance_card_number", insurance == null ? null : blankToNull(insurance.cardNumber()));
        columns.put("insurance_card_expiry", insurance == null ? null : insurance.cardExpiry());
        columns.put("ask_family_first", c.askFamilyFirst());
        columns.put("escort", name(c.accompaniedBy()));
        columns.put("note", blankToNull(c.note()));
        columns.put("respondent_type", c.respondent().type().name());
        columns.put("respondent_family_member_id", c.respondent().familyMemberId());
        columns.put("signed_copy_file_id", c.signedCopyFileId());
        columns.put(
                "notify_family_member_ids",
                (c.notifyFamilyMemberIds() == null ? List.<Long>of() : c.notifyFamilyMemberIds())
                        .stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]")));
        columns.put("draft_opinions", opinionsJson(c.otherOpinions()));
        return columns;
    }

    private String opinionsJson(List<WishOpinion> opinions) {
        List<WishOpinionContent> rows = (opinions == null ? List.<WishOpinion>of() : opinions)
                .stream()
                        .map(o -> new WishOpinionContent(
                                o.source() == null ? null : o.source().type().name(),
                                o.source() == null ? null : o.source().familyMemberId(),
                                o.content()))
                        .toList();
        return json.writeValueAsString(rows);
    }

    private List<WishOpinion> opinions(String value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(json.readValue(value, WishOpinionContent[].class))
                .map(o -> new WishOpinion(
                        new OpinionSource(OpinionSourceType.valueOf(o.sourceType()), o.sourceFamilyMemberId()),
                        o.content()))
                .toList();
    }

    private EndOfLifeWishDraft endOfLife(ResultSet rs) throws SQLException {
        CourtDecisionRef court =
                rs.getString("court_decision_number") == null && rs.getObject("court_decision_file_id") == null
                        ? null
                        : new CourtDecisionRef(
                                rs.getString("court_decision_number"),
                                rs.getObject("court_decision_date", LocalDate.class),
                                rs.getString("court_decision_court"),
                                rs.getObject("court_decision_file_id", Long.class));
        String choice = rs.getString("choice");
        String signer = rs.getString("signer_type");
        String selection = rs.getString("representative_selection");
        return new EndOfLifeWishDraft(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getString("status"),
                new EndOfLifeDraftContent(
                        choice == null ? null : WishChoice.valueOf(choice),
                        rs.getString("first_contact"),
                        rs.getString("note"),
                        WishSignerType.valueOf(rs.getString("respondent_type")),
                        rs.getObject("respondent_family_member_id", Long.class),
                        rs.getString("relationship"),
                        signer == null ? null : WishSignerType.valueOf(signer),
                        rs.getObject("signer_family_member_id", Long.class),
                        selection == null ? null : RepresentativeSelection.valueOf(selection),
                        court,
                        opinions(rs.getString("draft_opinions")),
                        instant(rs, "signed_at"),
                        rs.getObject("signed_copy_file_id", Long.class)),
                rs.getLong("recorded_by"),
                instant(rs, "recorded_at"),
                discard(rs));
    }

    private TreatmentWishDraft treatment(ResultSet rs) throws SQLException {
        List<PreferredHospital> hospitals = new ArrayList<>();
        if (rs.getString("hospital1_name") != null || rs.getString("hospital1_address") != null) {
            hospitals.add(new PreferredHospital(rs.getString("hospital1_name"), rs.getString("hospital1_address")));
        }
        if (rs.getString("hospital2_name") != null || rs.getString("hospital2_address") != null) {
            hospitals.add(new PreferredHospital(rs.getString("hospital2_name"), rs.getString("hospital2_address")));
        }
        boolean hasInsurance = rs.getString("insurance_registration_place") != null
                || rs.getString("insurance_card_number") != null
                || rs.getObject("insurance_card_expiry") != null;
        String escort = rs.getString("escort");
        String notify = rs.getString("notify_family_member_ids");
        return new TreatmentWishDraft(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getString("status"),
                new TreatmentDraftContent(
                        List.copyOf(hospitals),
                        hasInsurance
                                ? new HealthInsurance(
                                        rs.getString("insurance_registration_place"),
                                        rs.getString("insurance_card_number"),
                                        rs.getObject("insurance_card_expiry", LocalDate.class))
                                : null,
                        rs.getObject("ask_family_first", Boolean.class),
                        notify == null ? List.of() : Arrays.asList(json.readValue(notify, Long[].class)),
                        escort == null ? null : TreatmentEscort.valueOf(escort),
                        rs.getString("note"),
                        new OpinionSource(
                                OpinionSourceType.valueOf(rs.getString("respondent_type")),
                                rs.getObject("respondent_family_member_id", Long.class)),
                        opinions(rs.getString("draft_opinions")),
                        rs.getObject("signed_copy_file_id", Long.class)),
                rs.getLong("recorded_by"),
                instant(rs, "recorded_at"),
                discard(rs));
    }

    private static DraftDiscard discard(ResultSet rs) throws SQLException {
        Long by = rs.getObject("discarded_by", Long.class);
        return by == null ? null : new DraftDiscard(by, instant(rs, "discarded_at"), rs.getString("discard_reason"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
