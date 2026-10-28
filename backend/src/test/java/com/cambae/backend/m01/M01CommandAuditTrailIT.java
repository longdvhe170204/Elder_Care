package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m01.api.CareLevelApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import com.cambae.backend.m01.internal.consent.domain.Consent;
import com.cambae.backend.m01.internal.consent.service.ConsentService;
import com.cambae.backend.m01.internal.consent.service.ConsentService.RecordConsent;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecordContent;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordCommand;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService.ProfileView;
import com.cambae.backend.m01.internal.wish.domain.EndOfLifeWish;
import com.cambae.backend.m01.internal.wish.service.EndOfLifeWishService;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
@Import(FakeFileConfig.class)
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
    com.cambae.backend.m15.account.api.FieldVisibilityApi visibility;

    @Autowired
    com.cambae.backend.support.fake.m15.FakeFamilyRelationships families;

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
                .map(com.cambae.backend.common.Role::code)
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

    @Test
    void DBR_23_createProfile_andUpdate_trailTheResident() {
        long m = mark();
        user.set(office.user());
        ProfileView created = profiles.create(
                        new PersonalInfo(
                                "Trần Văn Kiểm Vết " + UUID.randomUUID(),
                                LocalDate.of(1944, 3, 1),
                                Gender.MALE,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        false)
                .profile();

        Entry createdEntry = exactlyOne(since(m), "RESIDENT_CREATED", RESIDENT, created.id(), created.id(), office);
        assertThat(createdEntry.after()).contains("fullName");
        assertThat(since(m)).hasSize(1);

        long m2 = mark();
        PersonalInfo changed = new PersonalInfo(
                created.personalInfo().fullName(),
                created.personalInfo().dateOfBirth(),
                Gender.MALE,
                null,
                null,
                null,
                "Số 3 Lê Lợi",
                null,
                null,
                null);
        profiles.updatePersonalInfo(
                target(created.id()), com.cambae.backend.common.web.VersionEtag.of(created.version()), changed, REASON);

        Entry updated = exactlyOne(since(m2), "RESIDENT_UPDATED", RESIDENT, created.id(), created.id(), office);
        assertThat(updated.reason()).isEqualTo(REASON.value());
        assertThat(updated.before()).isNotNull();
        assertThat(updated.after()).contains("permanentAddress");
        assertThat(since(m2)).hasSize(1);
    }

    // ---- Sức khỏe ----

    @Test
    void DBR_23_healthItem_recordAndExclude_trailTheResident() {
        long resident = newResident();
        user.set(doctor.user());
        long m = mark();
        var recorded = items.record(
                target(resident),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));

        Entry entry = exactlyOne(
                since(m),
                "HEALTH_ITEM_RECORDED",
                "M01.HEALTH_ITEM",
                recorded.item().id(),
                resident,
                doctor);
        assertThat(entry.after()).isNotNull();

        long m2 = mark();
        items.exclude(target(resident), recorded.item().id(), REASON);

        Entry excluded = exactlyOne(
                since(m2),
                "HEALTH_ITEM_EXCLUDED",
                "M01.HEALTH_ITEM",
                recorded.item().id(),
                resident,
                doctor);
        assertThat(excluded.reason()).isEqualTo(REASON.value());
        assertThat(excluded.before()).isNotNull();
        assertThat(excluded.after()).isNotNull();
    }

    @Test
    void DBR_23_initialHealthRecord_createEditConfirm_trailTheResident() {
        long resident = newResident();
        assignNurse(nurse, resident);
        user.set(nurse.user());
        long m = mark();
        var draft = records.saveDraft(
                target(resident),
                null,
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi", "Ít muối", "Thuốc A", "Tập"));
        Entry created = exactlyOne(
                since(m), "INITIAL_HEALTH_RECORD_CREATED", "M01.INITIAL_HEALTH_RECORD", draft.id(), resident, nurse);
        assertThat(created.after()).isNotNull();

        long m2 = mark();
        var edited = records.saveDraft(
                target(resident),
                "\"" + draft.version() + "\"",
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Mới", "Ít muối", "Thuốc B", "Tập"));
        exactlyOne(
                since(m2),
                "INITIAL_HEALTH_RECORD_DRAFT_EDITED",
                "M01.INITIAL_HEALTH_RECORD",
                draft.id(),
                resident,
                nurse);

        user.set(doctor.user());
        long m3 = mark();
        records.confirm(target(resident), "\"" + edited.version() + "\"");
        exactlyOne(
                since(m3),
                "INITIAL_HEALTH_RECORD_CONFIRMED",
                "M01.INITIAL_HEALTH_RECORD",
                draft.id(),
                resident,
                doctor);
        everyEntryBelongsTo(since(m3), resident);
    }

    // ---- Đánh giá ----

    @Test
    void DBR_23_assessment_createEditConfirmDiscard_trailTheResident() {
        long resident = newResident();
        assignNurse(nurse, resident);
        user.set(nurse.user());
        long m = mark();
        AssessmentView draft = assessments.create(target(resident), AssessmentType.REASSESSMENT, "Căn cứ thử");
        exactlyOne(since(m), "ASSESSMENT_CREATED", "M01.ASSESSMENT", draft.id(), resident, nurse);

        user.set(doctor.user());
        long m2 = mark();
        AssessmentView edited = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.REGULAR, "Nhận xét", "Lưu ý", Set.of()));
        exactlyOne(since(m2), "ASSESSMENT_DRAFT_EDITED", "M01.ASSESSMENT", draft.id(), resident, doctor);

        long m3 = mark();
        assessments.confirm(target(resident), draft.id(), etag(edited));
        Entry confirmed = exactlyOne(since(m3), "ASSESSMENT_CONFIRMED", "M01.ASSESSMENT", draft.id(), resident, doctor);
        assertThat(confirmed.after()).contains("careLevel", "REGULAR");
        everyEntryBelongsTo(since(m3), resident);

        user.set(nurse.user());
        AssessmentView second = assessments.create(target(resident), AssessmentType.REASSESSMENT, "Lần hai");
        long m4 = mark();
        assessments.discard(target(resident), second.id(), REASON);
        Entry discarded = exactlyOne(since(m4), "ASSESSMENT_DISCARDED", "M01.ASSESSMENT", second.id(), resident, nurse);
        assertThat(discarded.reason()).isEqualTo(REASON.value());
    }

    // ---- Trạng thái ----

    @Test
    void DBR_23_everyStatusCommand_trailsTheResidentWithCommandAndHistoryId() {
        user.set(office.user());

        long absence = residentIn(ResidentStatus.IN_RESIDENCE);
        long m = mark();
        statusService.grantTemporaryAbsence(target(absence), REASON, noDetails());
        statusEntry(since(m), absence, "GRANT_TEMPORARY_ABSENCE", "IN_RESIDENCE", "TEMPORARILY_ABSENT", office);

        long m2 = mark();
        statusService.recordReturn(target(absence), REASON, noDetails());
        statusEntry(since(m2), absence, "RECORD_RETURN", "TEMPORARILY_ABSENT", "IN_RESIDENCE", office);

        long ending = residentIn(ResidentStatus.IN_RESIDENCE);
        long m3 = mark();
        statusService.endResidency(target(ending), REASON, noDetails());
        statusEntry(since(m3), ending, "END_RESIDENCY", "IN_RESIDENCE", "RESIDENCY_ENDED", office);

        long cancel = admissionResident();
        long m4 = mark();
        statusService.cancelAdmission(target(cancel), REASON, noDetails());
        statusEntry(since(m4), cancel, "CANCEL_ADMISSION", "ADMISSION_IN_PROGRESS", "ADMISSION_CANCELLED", office);

        long transfer = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());
        var basis = json.createObjectNode();
        basis.putObject("basis").put("incidentId", 9L);
        long m5 = mark();
        statusService.transferOut(target(transfer), REASON, basis);
        statusEntry(since(m5), transfer, "TRANSFER_OUT", "IN_RESIDENCE", "HOSPITALIZED", doctor);

        long death = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());
        long m6 = mark();
        statusService.recordDeath(target(death), REASON, noDetails());
        statusEntry(since(m6), death, "RECORD_DEATH", "IN_RESIDENCE", "DECEASED", doctor);
    }

    @Test
    void DBR_23_statusCommandFromAnotherModule_trailsTheResidentWithItsBasisAndNoReason() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        // 014 gọi trong yêu cầu của người điểm danh nên người dùng hiện tại cũng là họ
        user.set(nurse.user());
        long m = mark();

        inTransaction(() -> statusApi.execute(
                resident,
                com.cambae.backend.m01.api.dto.StatusCommand.ROLL_CALL_DEPARTURE,
                nurse.user(),
                java.util.Optional.empty(),
                noDetails(),
                sourceRecord(77L)));

        List<Entry> trail = since(m);
        Entry e = exactlyOne(trail, "RESIDENT_STATUS_CHANGED", RESIDENT, resident, resident, nurse);
        assertThat(e.reason()).isNull();
        assertThat(e.basis()).contains("77");
        assertThat(trail).hasSize(1);
    }

    @Test
    void DBR_23_markChecked_trailsTheResidentWithItsBasis() {
        long resident = newResident();
        user.set(doctor.user());
        var recorded = items.record(
                target(resident),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        long m = mark();

        items.markChecked(recorded.item().id(), com.cambae.backend.m01.api.dto.HealthItemCheckSource.MEDICATION);

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
                "SELECT id FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
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

    @Test
    void DBR_23_consent_recordSupersedeWithdraw_trailTheResident() {
        long resident = newResident();
        user.set(office.user());
        long m = mark();
        Consent first = consents.record(target(resident), byResident());
        exactlyOne(since(m), "CONSENT_RECORDED", "M01.CONSENT", first.id(), resident, office);

        long m2 = mark();
        Consent second = consents.record(target(resident), byResident());
        List<Entry> trail = since(m2);
        exactlyOne(trail, "CONSENT_RECORDED", "M01.CONSENT", second.id(), resident, office);
        exactlyOne(trail, "CONSENT_SUPERSEDED", "M01.CONSENT", first.id(), resident, office);

        long m3 = mark();
        consents.withdraw(consents.targetOf(second.id()), second.id(), REASON, pdf());
        Entry withdrawn = exactlyOne(since(m3), "CONSENT_WITHDRAWN", "M01.CONSENT", second.id(), resident, office);
        assertThat(withdrawn.reason()).isEqualTo(REASON.value());
    }

    private RecordConsent byResident() {
        return new RecordConsent(ConsenterType.RESIDENT, null, null, Set.of(), true, true, Instant.now(), pdf());
    }

    @Test
    void DBR_23_endOfLifeWish_andTreatmentWish_recordAndSupersede_trailTheResident() {
        long resident = newResident();
        assignNurse(nurse, resident);
        user.set(nurse.user());

        long m = mark();
        EndOfLifeWish first = wishes.record(
                target(resident),
                new WishCommandBuilder(resident, doctor.accountId(), pdf()).build(),
                com.cambae.backend.m01.api.dto.WishRecordSource.ADMISSION);
        exactlyOne(since(m), "END_OF_LIFE_WISH_RECORDED", "M01.END_OF_LIFE_WISH", first.id(), resident, nurse);

        long m2 = mark();
        EndOfLifeWish second = wishes.record(
                target(resident),
                new WishCommandBuilder(resident, doctor.accountId(), pdf())
                        .choice(com.cambae.backend.m01.api.dto.WishChoice.RETURN_HOME)
                        .build(),
                com.cambae.backend.m01.api.dto.WishRecordSource.ADMISSION);
        List<Entry> trail = since(m2);
        exactlyOne(trail, "END_OF_LIFE_WISH_RECORDED", "M01.END_OF_LIFE_WISH", second.id(), resident, nurse);
        exactlyOne(trail, "END_OF_LIFE_WISH_SUPERSEDED", "M01.END_OF_LIFE_WISH", first.id(), resident, nurse);

        long m3 = mark();
        var treatment = treatments.record(target(resident), new TreatmentCommandBuilder().build());
        exactlyOne(
                since(m3),
                "TREATMENT_WISH_RECORDED",
                "M01.TREATMENT_WISH",
                treatment.wish().id(),
                resident,
                nurse);

        long m4 = mark();
        var next = treatments.record(
                target(resident),
                new TreatmentCommandBuilder().askFamilyFirst(true).build());
        List<Entry> treatmentTrail = since(m4);
        exactlyOne(
                treatmentTrail,
                "TREATMENT_WISH_RECORDED",
                "M01.TREATMENT_WISH",
                next.wish().id(),
                resident,
                nurse);
        exactlyOne(
                treatmentTrail,
                "TREATMENT_WISH_EXPIRED",
                "M01.TREATMENT_WISH",
                treatment.wish().id(),
                resident,
                nurse);
    }

    // ---- Lệnh do module khác gọi, đính chính ----

    @Test
    void DBR_23_careLevelAppliedFromAnApprovedChangeRequest_isAudited() {
        long resident = newResident();
        setStatus(resident, "ADMISSION_IN_PROGRESS");
        confirmed(resident, AssessmentType.INITIAL, CareLevel.REGULAR, "Nhận xét", "Lưu ý");
        setStatus(resident, "IN_RESIDENCE");
        user.set(office.user());
        long m = mark();

        inTransaction(() -> {
            careLevels.applyChange(resident, CareLevel.SPECIAL, LocalDate.now(), 4242L);
            return null;
        });

        Entry e = exactlyOne(since(m), "CARE_LEVEL_CHANGED", RESIDENT, resident, resident, office);
        assertThat(e.before()).contains("REGULAR");
        assertThat(e.after()).contains("SPECIAL");
        assertThat(e.basis()).contains("4242");
    }

    @Test
    void DBR_23_correctionOfAnAssessment_isFoundInTheResidentsTrail() {
        long resident = newResident();
        confirmed(resident, AssessmentType.REASSESSMENT, CareLevel.REGULAR, "Nhận xét", "Lưu ý cũ");
        long assessmentId = jdbc.queryForObject(
                "SELECT id FROM assessment WHERE resident_id = ? AND status = 'CONFIRMED'", Long.class, resident);
        user.set(doctor.user());
        long m = mark();

        corrections.create(
                "M01.ASSESSMENT", assessmentId, CorrectionType.AMEND_CONTENT, Map.of("careNote", "Lưu ý đúng"), REASON);

        List<Entry> forResident = since(m).stream()
                .filter(e -> resident == (e.residentId() == null ? -1 : e.residentId()))
                .toList();
        assertThat(forResident)
                .as("đính chính lần đánh giá phải tra được theo người cao tuổi")
                .anySatisfy(e -> {
                    assertThat(e.entityType()).isEqualTo("M01.ASSESSMENT");
                    assertThat(e.entityId()).isEqualTo(String.valueOf(assessmentId));
                    assertThat(e.actorId()).isEqualTo(doctor.accountId());
                    assertThat(e.reason()).isEqualTo(REASON.value());
                });

        // một dòng của 000, gắn người cao tuổi qua CorrectableRecordProvider.residentId (L98), không ghi thêm dòng
        // riêng
        correctionIsOneLineOfTheResident(since(m), "M01.ASSESSMENT", assessmentId, resident);
    }

    @Test
    void DBR_23_correctionOfTheInitialHealthRecord_isOneLineOfTheResidentsTrail() {
        long resident = newResident();
        assignNurse(nurse, resident);
        user.set(nurse.user());
        var draft = records.saveDraft(
                target(resident),
                null,
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi", "Ít muối", "Thuốc A", "Tập"));
        user.set(doctor.user());
        records.confirm(target(resident), "\"" + draft.version() + "\"");
        user.set(nurse.user());
        long m = mark();

        corrections.create(
                "M01.INITIAL_HEALTH_RECORD",
                draft.id(),
                CorrectionType.AMEND_CONTENT,
                Map.of("bloodType", "A+"),
                REASON);

        correctionIsOneLineOfTheResident(since(m), "M01.INITIAL_HEALTH_RECORD", draft.id(), resident);
    }

    private void correctionIsOneLineOfTheResident(List<Entry> trail, String entityType, long recordId, long resident) {
        List<Entry> all = trail.stream()
                .filter(e -> e.entityType().equals(entityType) && e.entityId().equals(String.valueOf(recordId)))
                .toList();
        assertThat(all).extracting(Entry::action).containsExactly("CORRECTION_ADDED");
        assertThat(all.getFirst().residentId()).isEqualTo(resident);
    }

    // ---- Che giá trị nhạy cảm khi tra nhật ký ----

    private com.cambae.backend.m15.account.api.dto.InfoGroup groupOf(String entityType, String field) {
        return providers.stream()
                .filter(p -> p.entityType().equals(entityType))
                .findFirst()
                .orElseThrow()
                .fieldGroups()
                .get(field);
    }

    /**
     * Ghi chú đặc biệt là văn bản tự do có thể chứa tình trạng sức khỏe; danh sách người thân được xem sức khỏe là
     * cấu hình chia sẻ dữ liệu sức khỏe. Cả hai thuộc nhóm sức khỏe nên 000 che giá trị với người không xem được nhóm
     * này (Hành chính: chỉ thấy là đã đổi). Kiểm bằng phân loại thật và {@code FieldVisibilityApi} thật của 002; không
     * đọc qua {@code AuditLogQueryService.history} vì hàm đó đang trả 404 cho người xem theo phạm vi (đã báo A).
     */
    @Test
    void FR_049_specialNotesAndHealthSharingList_areHealthGroup_hiddenFromOfficeShownToDoctor() {
        user.set(office.user());
        long resident = profiles.create(
                        new PersonalInfo(
                                "Lê Thị Che Giá Trị " + UUID.randomUUID(),
                                LocalDate.of(1946, 5, 5),
                                Gender.FEMALE,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "Tiểu đường típ 2, dị ứng hải sản"),
                        false)
                .profile()
                .id();
        long relative = com.cambae.backend.support.TestUsers.nextId();
        families.relate(relative, resident);
        consents.record(
                target(resident),
                new RecordConsent(
                        ConsenterType.RESIDENT, null, null, Set.of(relative), true, true, Instant.now(), pdf()));

        // hai trường có thật trong nhật ký
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.RESIDENT' AND resident_id = ?"
                                + " AND JSON_CONTAINS_PATH(data_after, 'one', '$.specialNotes')",
                        Long.class,
                        resident))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.CONSENT' AND resident_id = ?"
                                + " AND JSON_CONTAINS_PATH(data_after, 'one', '$.familyMemberIdsAllowedHealthView')",
                        Long.class,
                        resident))
                .isEqualTo(1);

        var health = com.cambae.backend.m15.account.api.dto.InfoGroup.HEALTH;
        assertThat(groupOf(RESIDENT, "specialNotes")).isEqualTo(health);
        assertThat(groupOf("M01.CONSENT", "familyMemberIdsAllowedHealthView")).isEqualTo(health);
        assertThat(visibility.canView(office.user(), health, target(resident)))
                .isNotEqualTo(com.cambae.backend.m15.account.api.dto.Visibility.FULL);
        assertThat(visibility.canView(doctor.user(), health, target(resident)))
                .isEqualTo(com.cambae.backend.m15.account.api.dto.Visibility.FULL);
    }

    /** Phân loại chỉ che nhật ký: Hành chính vẫn xem và sửa ghi chú đặc biệt trên hồ sơ như trước. */
    @Test
    void FR_049_classifyingSpecialNotesAsHealth_doesNotChangeWhatOfficeSeesOnTheProfile() {
        user.set(office.user());
        long resident = profiles.create(
                        new PersonalInfo(
                                "Phạm Văn Hồ Sơ " + UUID.randomUUID(),
                                LocalDate.of(1943, 1, 9),
                                Gender.MALE,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "Cần người dìu khi đi lại"),
                        false)
                .profile()
                .id();

        assertThat(profiles.view(target(resident)).personalInfo().specialNotes())
                .isEqualTo("Cần người dìu khi đi lại");
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

    @Test
    void FR_049_everyLoggedField_ofResidentConsentAndWishes_isClassifiedOrDeliberatelyPublic() {
        long resident = newResident();
        assignNurse(nurse, resident);
        user.set(office.user());
        consents.record(target(resident), byResident());
        consents.record(target(resident), byResident());
        user.set(nurse.user());
        wishes.record(
                target(resident),
                new WishCommandBuilder(resident, doctor.accountId(), pdf()).build(),
                com.cambae.backend.m01.api.dto.WishRecordSource.ADMISSION);
        treatments.record(target(resident), new TreatmentCommandBuilder().build());
        user.set(office.user());
        statusService.grantTemporaryAbsence(target(residentIn(ResidentStatus.IN_RESIDENCE)), REASON, noDetails());

        for (String type : PUBLIC_FIELDS.keySet()) {
            Set<String> logged = new HashSet<>();
            jdbc.queryForList(
                            "SELECT CONCAT_WS('|', COALESCE(CAST(data_before AS CHAR), ''), COALESCE(CAST(data_after AS CHAR), ''))"
                                    + " FROM audit_log WHERE entity_type = ? AND resident_id IS NOT NULL",
                            String.class,
                            type)
                    .forEach(row -> {
                        for (String part : row.split("\\|(?=[{])|(?<=[}])\\|")) {
                            if (!part.isBlank()) {
                                logged.addAll(json.readTree(part).propertyNames());
                            }
                        }
                    });
            Set<String> classified = providers.stream()
                    .filter(p -> p.entityType().equals(type))
                    .findFirst()
                    .map(p -> p.fieldGroups().keySet())
                    .orElse(Set.of());
            Set<String> unclassified =
                    logged.stream().filter(f -> !classified.contains(f)).collect(Collectors.toCollection(HashSet::new));
            unclassified.removeAll(PUBLIC_FIELDS.get(type));
            assertThat(unclassified)
                    .as("trường của %s ghi vào nhật ký mà chưa phân loại (mặc định công khai nội bộ)", type)
                    .isEmpty();
        }
    }
}
