package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ConsenterType;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.consent.service.ConsentService;
import com.eldercare.backend.resident.internal.consent.service.ConsentService.RecordConsent;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 001 T058 (FR-012; US5 Acceptance): bản đồng ý không phải điều kiện chặn Hoàn tất tiếp nhận. Thiếu bản Hiệu lực thì lệnh
 * vẫn thực hiện, trả cảnh báo {@code MISSING_CONSENT} cho người thực hiện và bắt đầu nhắc Hành chính theo CFG-M01-06; đã
 * có bản Hiệu lực thì không cảnh báo, không nhắc.
 */
@Import(FakeFileConfig.class)
class CompleteAdmissionConsentIT extends StatusTestBase {

    @Autowired
    FileApi files;

    @Autowired
    ConsentService consents;

    private long admissionWithConfirmedAssessment() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        user.set(office.user());
        return id;
    }

    private int reminderRows(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM periodic_reminder WHERE type = 'MISSING_CONSENT' AND resident_id = ?",
                Integer.class,
                residentId);
    }

    @Test
    void FR_012_withoutActiveConsent_completesWithWarning_andStartsReminding() {
        long id = admissionWithConfirmedAssessment();

        var outcome = statusService.completeAdmission(target(id), reason(), noDetails());

        assertThat(currentStatus(id).name()).isEqualTo("IN_RESIDENCE");
        assertThat(outcome.warnings()).extracting(w -> w.code()).contains("MISSING_CONSENT");
        assertThat(reminderRows(id)).isEqualTo(1);
    }

    @Test
    void FR_012_withActiveConsent_completesWithoutWarningOrReminder() {
        long id = admissionWithConfirmedAssessment();
        long evidence = files.upload(
                "ban-ky.pdf", FileTypes.PDF, "%PDF-1.4 thử".getBytes(StandardCharsets.UTF_8), office.accountId());
        consents.record(
                target(id),
                new RecordConsent(ConsenterType.RESIDENT, null, null, Set.of(), true, true, Instant.now(), evidence));

        var outcome = statusService.completeAdmission(target(id), reason(), noDetails());

        assertThat(outcome.warnings()).extracting(w -> w.code()).doesNotContain("MISSING_CONSENT");
        assertThat(reminderRows(id)).isZero();
    }
}
