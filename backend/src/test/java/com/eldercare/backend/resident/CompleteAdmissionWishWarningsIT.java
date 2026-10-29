package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 001 T067, T077 (FR-023d, FR-023l; US7 #4, #11): phiếu nguyện vọng không phải điều kiện chặn Hoàn tất tiếp nhận. Thiếu
 * phiếu cuối đời thì lệnh vẫn thực hiện, trả cảnh báo {@code MISSING_END_OF_LIFE_WISH} và bắt đầu nhắc theo CFG-M01-06;
 * thiếu phiếu khi điều trị thì chỉ trả cảnh báo {@code MISSING_TREATMENT_WISH}, không có nhắc định kỳ. Đã có phiếu
 * Hiệu lực thì không cảnh báo.
 */
@Import(FakeFileConfig.class)
class CompleteAdmissionWishWarningsIT extends StatusTestBase {

    @Autowired
    FileApi files;

    @Autowired
    EndOfLifeWishService wishes;

    @Autowired
    TreatmentWishService treatments;

    private long admission() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        user.set(office.user());
        return id;
    }

    private int reminderRows(long residentId, String type) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM periodic_reminder WHERE type = ? AND resident_id = ?",
                Integer.class,
                type,
                residentId);
    }

    @Test
    void US7_4_withoutAnEndOfLifeWish_completesWithWarning_andStartsReminding() {
        long id = admission();

        var outcome = statusService.completeAdmission(target(id), reason(), noDetails());

        assertThat(currentStatus(id).name()).isEqualTo("IN_RESIDENCE");
        assertThat(outcome.warnings()).extracting(w -> w.code()).contains("MISSING_END_OF_LIFE_WISH");
        assertThat(reminderRows(id, "MISSING_END_OF_LIFE_WISH")).isEqualTo(1);
    }

    @Test
    void US7_11_withoutATreatmentWish_completesWithWarning_andNeverReminds() {
        long id = admission();

        var outcome = statusService.completeAdmission(target(id), reason(), noDetails());

        assertThat(outcome.warnings()).extracting(w -> w.code()).contains("MISSING_TREATMENT_WISH");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM periodic_reminder WHERE resident_id = ? AND type LIKE '%TREATMENT%'",
                        Integer.class, id))
                .isZero();
    }

    @Test
    void US7_4_withBothActiveWishes_completesWithoutEitherWarningOrReminder() {
        long id = admission();
        assignNurse(nurse, id);
        user.set(nurse.user());
        long copy = files.upload(
                "phieu.pdf", FileTypes.PDF, "%PDF-1.4 thử".getBytes(StandardCharsets.UTF_8), nurse.accountId());
        wishes.record(
                target(id),
                new WishCommandBuilder(id, doctor.accountId(), copy).build(),
                com.eldercare.backend.resident.api.dto.WishRecordSource.ADMISSION);
        treatments.record(target(id), new TreatmentCommandBuilder().build());
        user.set(office.user());

        var outcome = statusService.completeAdmission(target(id), reason(), noDetails());

        assertThat(outcome.warnings())
                .extracting(w -> w.code())
                .doesNotContain("MISSING_END_OF_LIFE_WISH", "MISSING_TREATMENT_WISH");
        assertThat(reminderRows(id, "MISSING_END_OF_LIFE_WISH")).isZero();
    }
}
