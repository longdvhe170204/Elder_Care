package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.consent.service.ConsentService;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 001 T011, T079 (data-model mục 11; FR-050, FR-051, FR-051a; 000 R-11): M01 đăng ký {@code RetentionProvider} cho hồ sơ
 * người cao tuổi và các bản ghi con. Mọi loại tính hạn theo CFG-M15-03 từ lúc hồ sơ vào trạng thái cuối; hồ sơ còn
 * hoạt động, hồ sơ trạng thái cuối chưa quá hạn, hồ sơ có hồ sơ sau còn hoạt động trỏ tới không bao giờ bị xóa. Khi xóa:
 * cả chuỗi (lịch sử, đồng ý, sức khỏe, đánh giá, phiếu nguyện vọng, nhắc, đính chính, nhật ký) cùng một transaction.
 * Đồng hồ giả lập; dữ liệu tạo bằng lệnh thật rồi đẩy đồng hồ tới sau hạn.
 */
@Import(FakeFileConfig.class)
class ResidentRetentionIT extends StatusTestBase {

    private static final Reason REASON = new Reason("Kiểm thử lưu giữ");

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
    FakeFamilyRelationships families;

    @Autowired
    ExpiredDataPurgeJob job;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private long pdf() {
        return files.upload(
                "ban-ky.pdf", FileTypes.PDF, "%PDF-1.4 thử".getBytes(StandardCharsets.UTF_8), office.accountId());
    }

    /** Gắn tệp như lệnh của module (FileApi.attach chỉ chạy trong transaction của lệnh). */
    private long attach(long fileId, String ownerType, long ownerId) {
        jdbc.update(
                "UPDATE file SET owner_type = ?, owner_id = ?, attached_at = uploaded_at WHERE id = ?",
                ownerType,
                ownerId,
                fileId);
        return fileId;
    }

    /** Người cao tuổi có đủ loại bản ghi của M01, đã Kết thúc lưu trú vào lúc đồng hồ hiện tại. */
    private long endedResidentWithEverything() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        confirmed(id, AssessmentType.INITIAL, CareLevel.REGULAR, "Nhận xét", "Lưu ý");
        setStatus(id, "IN_RESIDENCE");
        long assessmentId =
                jdbc.queryForObject("SELECT id FROM assessment WHERE resident_id = ? LIMIT 1", Long.class, id);

        user.set(doctor.user());
        items.record(
                target(id),
                new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false));
        assignNurse(nurse, id);
        user.set(nurse.user());
        var draft = records.saveDraft(
                target(id),
                null,
                new InitialHealthRecordContent(
                        "O+", "Đi lại khó", "Tự vệ sinh", "Tỉnh", "Ghi", "Ít muối", "Thuốc A", "Tập"));
        user.set(doctor.user());
        records.confirm(target(id), "\"" + draft.version() + "\"");

        user.set(office.user());
        long relative = TestUsers.nextId();
        families.relate(relative, id);
        consents.record(
                target(id),
                new RecordConsent(
                        ConsenterType.RESIDENT, null, null, Set.of(relative), true, true, Instant.now(), pdf()));

        user.set(nurse.user());
        wishes.record(
                target(id), new WishCommandBuilder(id, doctor.accountId(), pdf()).build(), WishRecordSource.ADMISSION);
        treatments.record(
                target(id), new TreatmentCommandBuilder().notify(relative).build());

        user.set(doctor.user());
        corrections.create(
                "M01.ASSESSMENT", assessmentId, CorrectionType.AMEND_CONTENT, Map.of("careNote", "Lưu ý đúng"), REASON);

        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at) VALUES"
                        + " ('M01.PERIODIC_REMINDER.MISSING_CONSENT', 'M01.RESIDENT', ?, 'START:0', ?)",
                id,
                java.sql.Timestamp.from(time.now()));
        com.eldercare.backend.support.ResidentHistoryRows.insertNotice(
                jdbc, id, "PERIODIC", "ASSESSMENT", assessmentId, time.now());

        user.set(office.user());
        statusService.grantTemporaryAbsence(target(id), REASON, noDetails());
        // đính chính lý do của dòng lịch sử (cách A, người ghi gốc): job xóa cùng chuỗi hồ sơ
        corrections.create(
                "M01.RESIDENT_STATUS_HISTORY",
                lastStatusHistoryId(id),
                CorrectionType.AMEND_CONTENT,
                Map.of("reason", "Lý do đúng"),
                REASON);
        statusService.recordReturn(target(id), REASON, noDetails());
        statusService.endResidency(target(id), REASON, noDetails());
        return id;
    }

    private int count(String table, long residentId) {
        if (table.startsWith("simple_record:")) {
            // lần báo đánh giá lại ở simple_record từ 001 T084
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM simple_record WHERE kind = ? AND resident_id = ?",
                    Integer.class,
                    table.substring("simple_record:".length()),
                    residentId);
        }
        if (table.startsWith("status_history:")) {
            // lịch sử trạng thái, mức chăm sóc ở status_history từ 001 T084
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?"
                            + " AND attribute = ?",
                    Integer.class,
                    residentId,
                    table.substring("status_history:".length()));
        }
        String column = table.equals("resident") ? "id" : table.equals("reminder_marker") ? "entity_id" : "resident_id";
        String scope = table.equals("reminder_marker") ? " AND entity_type = 'M01.RESIDENT'" : "";
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?" + scope, Integer.class, residentId);
    }

    private long lastStatusHistoryId(long residentId) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM status_history WHERE entity_type = 'M01.RESIDENT' AND attribute = 'STATUS'"
                        + " AND entity_id = ?",
                Long.class,
                residentId);
    }

    private static final List<String> TABLES = List.of(
            "resident",
            "status_history:STATUS",
            "status_history:CARE_LEVEL",
            "simple_record:M01.INITIAL_HEALTH_RECORD",
            "health_item",
            "assessment",
            "consent",
            "wish",
            "simple_record:M01.REASSESSMENT_NOTICE",
            "reminder_marker");

    private void runJobAfter(Duration sinceNow) {
        clock.setTo(Instant.now().plus(sinceNow));
        user.set(FakePermissions.user(0, Role.SCHEDULER));
        job.run();
    }

    @Test
    void FR_051_endedResidentPastCfgM1503_isDeletedWithTheWholeChain() {
        long id = endedResidentWithEverything();
        long assessmentId =
                jdbc.queryForObject("SELECT id FROM assessment WHERE resident_id = ? LIMIT 1", Long.class, id);
        String historyCorrections =
                "SELECT COUNT(*) FROM correction c JOIN status_history h ON h.id = c.original_record_id"
                        + " WHERE c.record_type = 'M01.RESIDENT_STATUS_HISTORY' AND h.entity_type = 'M01.RESIDENT'"
                        + " AND h.entity_id = ?";
        assertThat(jdbc.queryForObject(historyCorrections, Integer.class, id)).isEqualTo(1);
        java.util.List<Long> historyIds = jdbc.queryForList(
                "SELECT id FROM status_history WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?", Long.class, id);
        for (String table : TABLES) {
            assertThat(count(table, id)).as("trước khi xóa: %s", table).isPositive();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE resident_id = ?", Integer.class, id))
                .isPositive();
        // tệp có loại chủ sở hữu khác loại bản ghi (FilePermissionProvider.recordType): ảnh gắn hồ sơ, giấy tờ bệnh
        // viện
        // gắn dòng lịch sử trạng thái; cùng tệp bản ký của bản đồng ý (loại chủ sở hữu trùng loại bản ghi)
        long photo = attach(pdf(), "M01.RESIDENT_PHOTO", id);
        long hospitalDocument = attach(pdf(), "M01.HOSPITAL_DOCUMENT", lastStatusHistoryId(id));
        String residentFiles = "SELECT COUNT(*) FROM file WHERE id IN (" + photo + ", " + hospitalDocument
                + ") OR (owner_type = 'M01.CONSENT' AND owner_id IN (SELECT id FROM consent WHERE resident_id = " + id
                + "))";
        assertThat(jdbc.queryForObject(residentFiles, Integer.class)).isEqualTo(3);

        runJobAfter(Duration.ofDays(11 * 366));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM file WHERE id IN (" + photo + ", " + hospitalDocument + ")",
                        Integer.class))
                .as("tệp gắn hồ sơ, dòng lịch sử (FR-051a)")
                .isZero();

        for (String table : TABLES) {
            assertThat(count(table, id)).as("sau khi xóa: %s", table).isZero();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M01.ASSESSMENT' AND original_record_id = ?",
                        Integer.class,
                        assessmentId))
                .as("đính chính của hồ sơ")
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M01.RESIDENT_STATUS_HISTORY'"
                                + " AND original_record_id IN ("
                                + historyIds.stream()
                                        .map(String::valueOf)
                                        .collect(java.util.stream.Collectors.joining(","))
                                + ")",
                        Integer.class))
                .as("đính chính của dòng lịch sử trạng thái")
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment WHERE resident_id = ?", Integer.class, id))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.PURGE_RUN'", Integer.class))
                .as("bản tóm tắt đợt xóa")
                .isPositive();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE resident_id = ? AND entity_type LIKE 'M01.%'",
                        Integer.class, id))
                .as("nhật ký gắn hồ sơ hết hạn cùng chuỗi (AuditLogRetention, FR-050)")
                .isZero();
    }

    @Test
    void FR_050_activeResident_andEndedResidentWithinTheRetentionPeriod_areKept() {
        long active = newResident();
        long recentlyEnded = newResident();
        // hồ sơ này Kết thúc lưu trú sau hạn của hồ sơ khác: 5 năm sau hôm nay, còn trong CFG-M15-03 (10 năm) lúc chạy
        clock.setTo(Instant.now().plus(Duration.ofDays(5 * 366)));
        user.set(office.user());
        statusService.endResidency(target(recentlyEnded), REASON, noDetails());

        runJobAfter(Duration.ofDays(11 * 366));

        assertThat(count("resident", active)).isEqualTo(1);
        assertThat(count("resident", recentlyEnded)).isEqualTo(1);
        assertThat(count("status_history:STATUS", recentlyEnded)).isPositive();
    }

    @Test
    void FR_051a_expiredProfileWithALaterActiveProfileLinkedToIt_isKept() {
        String cccd = ResidentProfileTestBase_citizenId();
        user.set(office.user());
        long earlier =
                profiles.create(personal("Trần Văn Cũ", cccd), false).profile().id();
        setStatus(earlier, "RESIDENCY_ENDED");
        com.eldercare.backend.support.ResidentHistoryRows.insertStatus(
                jdbc,
                null,
                earlier,
                "IN_RESIDENCE",
                "RESIDENCY_ENDED",
                "SEED",
                java.sql.Timestamp.from(time.now()),
                office.accountId(),
                true,
                null);
        long later =
                profiles.create(personal("Trần Văn Mới", cccd), false).profile().id();
        assertThat(jdbc.queryForObject("SELECT previous_profile_id FROM resident WHERE id = ?", Long.class, later))
                .isEqualTo(earlier);

        runJobAfter(Duration.ofDays(11 * 366));

        assertThat(count("resident", earlier))
                .as("hồ sơ cũ được hồ sơ sau trỏ tới")
                .isEqualTo(1);
        assertThat(count("resident", later)).isEqualTo(1);
    }

    private static String ResidentProfileTestBase_citizenId() {
        return String.valueOf(300_000_000_000L + (System.nanoTime() % 99_999_999_999L));
    }

    private static com.eldercare.backend.resident.internal.profile.domain.PersonalInfo personal(
            String name, String cccd) {
        return new com.eldercare.backend.resident.internal.profile.domain.PersonalInfo(
                name + " " + cccd,
                java.time.LocalDate.of(1940, 1, 1),
                com.eldercare.backend.resident.api.dto.Gender.MALE,
                com.eldercare.backend.resident.internal.profile.domain.IdDocumentType.CITIZEN_ID,
                cccd,
                null,
                null,
                null,
                null,
                null);
    }
}
