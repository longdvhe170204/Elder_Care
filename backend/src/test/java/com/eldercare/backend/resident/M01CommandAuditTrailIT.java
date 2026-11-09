package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.resident.api.CareLevelApi;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.internal.consent.service.ConsentService;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.json.JsonMapper;

/**
 * 001 T013, T075 (DBR-23, FR-049, SC-001): mọi lệnh của M01 trên dữ liệu nhóm 2 và 3 ghi nhật ký có {@code resident_id}
 * của người cao tuổi, đúng loại bản ghi và mã bản ghi, đúng người thực hiện (do 000 điền từ người dùng hiện tại, vai
 * trò theo thứ tự enum), kèm lý do khi lệnh có lý do. Bổ sung cho {@link M01AuditCompletenessIT} (lệnh danh mục, đếm
 * câu {@code INSERT}). Mỗi test chạy lệnh bằng người dùng thật của lệnh đó (Phụ lục 27) rồi đọc các dòng
 * {@code audit_log} mới phát sinh.
 */
@Import({FakeFileConfig.class, AllergyCheckersOff.class})
class M01CommandAuditTrailIT extends StatusTestBase {

    private static final Reason REASON = new Reason("Lý do kiểm vết");
    private static final String RESIDENT = "M01.RESIDENT";

    @Autowired
    FileApi files;

    @Autowired
    HealthItemService items;

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    ConsentService consents;

    @Autowired
    EndOfLifeWishService wishes;

    @Autowired
    TreatmentWishService treatments;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    CareLevelApi careLevels;

    @Autowired
    com.eldercare.backend.administration.account.api.FieldVisibilityApi visibility;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeFamilyRelationships families;

    @Autowired
    List<FieldClassificationProvider> providers;

    @Autowired
    JsonMapper json;

    /** Một dòng nhật ký, chỉ các cột mà hợp đồng truy vết yêu cầu. */
    record Entry(
            String actorType,
            Long actorId,
            String role,
            String action,
            String entityType,
            String entityId,
            Long residentId,
            String before,
            String after,
            String reason,
            String basis) {}

    private long mark() {
        return jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM audit_log", Long.class);
    }

    private List<Entry> since(long mark) {
        return jdbc.query(
                "SELECT actor_type, actor_id, role, action, entity_type, entity_id, resident_id,"
                        + " CAST(data_before AS CHAR) b, CAST(data_after AS CHAR) a, reason, CAST(basis AS CHAR) bs"
                        + " FROM audit_log WHERE id > ? ORDER BY id",
                (rs, i) -> new Entry(
                        rs.getString("actor_type"),
                        rs.getObject("actor_id", Long.class),
                        rs.getString("role"),
                        rs.getString("action"),
                        rs.getString("entity_type"),
                        rs.getString("entity_id"),
                        rs.getObject("resident_id", Long.class),
                        rs.getString("b"),
                        rs.getString("a"),
                        rs.getString("reason"),
                        rs.getString("bs")),
                mark);
    }

    private static String roleOf(TestUser who) {
        return who.user().roles().stream()
                .min(Comparator.naturalOrder())
                .map(com.eldercare.backend.common.Role::code)
                .orElseThrow();
    }

    /** Đúng một dòng nhật ký có mã hành động này; người thực hiện, người cao tuổi, loại và mã bản ghi đúng hợp đồng. */
    private Entry exactlyOne(
            List<Entry> trail, String action, String entityType, Object entityId, long residentId, TestUser actor) {
        List<Entry> found = trail.stream()
                .filter(e -> e.action().equals(action) && e.entityType().equals(entityType))
                .toList();
        assertThat(found)
                .as("nhật ký %s %s trong %s", action, entityType, trail)
                .hasSize(1);
        Entry e = found.getFirst();
        assertThat(e.entityId()).as("mã bản ghi").isEqualTo(String.valueOf(entityId));
        assertThat(e.residentId()).as("resident_id của %s", action).isEqualTo(residentId);
        assertThat(e.actorType()).as("loại người thực hiện").isEqualTo("USER");
        assertThat(e.actorId()).as("người thực hiện").isEqualTo(actor.accountId());
        assertThat(e.role()).as("vai trò").isEqualTo(roleOf(actor));
        return e;
    }

    private void everyEntryBelongsTo(List<Entry> trail, long residentId) {
        assertThat(trail).isNotEmpty();
        assertThat(trail)
                .allSatisfy(e -> assertThat(e.residentId())
                        .as("%s %s phải gắn người cao tuổi", e.action(), e.entityType())
                        .isEqualTo(residentId));
    }

    private long pdf() {
        return files.upload(
                "ban-ky.pdf",
                FileTypes.PDF,
                "%PDF-1.4 bản ký thử".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    // ---- Hồ sơ ----

    // ---- Sức khỏe ----

    // ---- Đánh giá ----

    // ---- Trạng thái ----

    @Test
    void DBR_23_markChecked_trailsTheResidentWithItsBasis() {
        long resident = newResident();
        user.set(doctor.user());
        var recorded = items.record(
                target(resident),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        long m = mark();

        // Listener thật của 006, 011 bị gỡ (AllergyCheckersOff): cờ dinh dưỡng còn chờ nên lần gọi này đổi cờ, ghi nhật
        // ký
        items.markChecked(recorded.item().id(), com.eldercare.backend.resident.api.dto.HealthItemCheckSource.NUTRITION);

        Entry e = exactlyOne(
                since(m),
                "HEALTH_ITEM_CHECKED",
                "M01.HEALTH_ITEM",
                recorded.item().id(),
                resident,
                doctor);
        assertThat(e.basis()).contains(String.valueOf(recorded.item().id()));
    }

    private void statusEntry(List<Entry> trail, long resident, String command, String from, String to, TestUser actor) {
        long historyId = jdbc.queryForObject(
                "SELECT id FROM " + com.eldercare.backend.support.ResidentHistoryRows.STATUS
                        + " WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                resident);
        Entry e = exactlyOne(trail, "RESIDENT_STATUS_CHANGED", RESIDENT, resident, resident, actor);
        everyEntryBelongsTo(trail, resident);
        assertThat(e.before()).contains(from);
        assertThat(e.after()).contains(to);
        assertThat(e.reason()).isEqualTo(REASON.value());
        assertThat(json.readTree(e.after()).get("status").asString()).isEqualTo(to);
        assertThat(trail.stream()
                        .filter(t -> t.action().equals("RESIDENT_STATUS_CHANGED"))
                        .findFirst())
                .isPresent();
        assertThat(command).isNotBlank();
        assertThat(historyId).isPositive();
    }

    // ---- Đồng ý, nguyện vọng ----

    private RecordConsent byResident() {
        return new RecordConsent(ConsenterType.RESIDENT, null, null, Set.of(), true, true, Instant.now(), pdf());
    }

    // ---- Lệnh do module khác gọi, đính chính ----

    private void correctionIsOneLineOfTheResident(List<Entry> trail, String entityType, long recordId, long resident) {
        List<Entry> all = trail.stream()
                .filter(e -> e.entityType().equals(entityType) && e.entityId().equals(String.valueOf(recordId)))
                .toList();
        assertThat(all).extracting(Entry::action).containsExactly("CORRECTION_ADDED");
        assertThat(all.getFirst().residentId()).isEqualTo(resident);
    }

    // ---- Che giá trị nhạy cảm khi tra nhật ký ----

    private com.eldercare.backend.administration.account.api.dto.InfoGroup groupOf(String entityType, String field) {
        return providers.stream()
                .filter(p -> p.entityType().equals(entityType))
                .findFirst()
                .orElseThrow()
                .fieldGroups()
                .get(field);
    }

    // ---- Phân loại trường ----

    /** Trường lệnh ghi vào nhật ký nhưng cố ý thuộc nhóm công khai nội bộ (không phải dữ liệu nhạy cảm). */
    private static final Map<String, Set<String>> PUBLIC_FIELDS = Map.of(
            "M01.RESIDENT",
            Set.of("status", "fullName", "dateOfBirth", "gender", "profileCode"),
            "M01.CONSENT",
            Set.of(
                    "status",
                    "supersededById",
                    "consenterType",
                    "photoConsent",
                    "notificationConsent",
                    "consentedAt",
                    "evidenceFileId"),
            "M01.END_OF_LIFE_WISH",
            Set.of("status", "supersededById", "source"),
            "M01.TREATMENT_WISH",
            Set.of("status", "supersededById"));
}
