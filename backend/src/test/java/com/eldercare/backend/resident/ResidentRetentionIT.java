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
                "INSERT INTO periodic_reminder (type, resident_id, milestone_id, started_at) VALUES"
                        + " ('MISSING_CONSENT', ?, 0, ?)",
                id,
                java.sql.Timestamp.from(time.now()));
        jdbc.update(
                "INSERT INTO reassessment_notice (resident_id, basis, source_type, source_id, notified_at) VALUES"
                        + " (?, 'PERIODIC', 'ASSESSMENT', ?, ?)",
                id,
                assessmentId,
                java.sql.Timestamp.from(time.now()));

        user.set(office.user());
        statusService.grantTemporaryAbsence(target(id), REASON, noDetails());
        statusService.recordReturn(target(id), REASON, noDetails());
        statusService.endResidency(target(id), REASON, noDetails());
        return id;
    }

    private int count(String table, long residentId) {
        String column = table.equals("resident") ? "id" : "resident_id";
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, residentId);
    }

    private static final List<String> TABLES = List.of(
            "resident",
            "resident_status_history",
            "care_level_history",
            "initial_health_record",
            "health_item",
            "assessment",
            "consent",
            "end_of_life_wish",
            "treatment_wish",
            "reassessment_notice",
            "periodic_reminder");

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
        for (String table : TABLES) {
            assertThat(count(table, id)).as("trước khi xóa: %s", table).isPositive();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE resident_id = ?", Integer.class, id))
                .isPositive();

        runJobAfter(Duration.ofDays(11 * 366));

        for (String table : TABLES) {
            assertThat(count(table, id)).as("sau khi xóa: %s", table).isZero();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction WHERE record_type = 'M01.ASSESSMENT' AND original_record_id = ?",
                        Integer.class,
                        assessmentId))
                .as("đính chính của hồ sơ")
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
        assertThat(count("resident_status_history", recentlyEnded)).isPositive();
    }

    @Test
    void FR_051a_expiredProfileWithALaterActiveProfileLinkedToIt_isKept() {
        String cccd = ResidentProfileTestBase_citizenId();
        user.set(office.user());
        long earlier =
                profiles.create(personal("Trần Văn Cũ", cccd), false).profile().id();
        setStatus(earlier, "RESIDENCY_ENDED");
        jdbc.update(
                "INSERT INTO resident_status_history (resident_id, from_status, to_status, command, occurred_at,"
                        + " performed_by, by_system) VALUES (?, 'IN_RESIDENCE', 'RESIDENCY_ENDED', 'SEED', ?, ?, TRUE)",
                earlier,
                java.sql.Timestamp.from(time.now()),
                office.accountId());
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
