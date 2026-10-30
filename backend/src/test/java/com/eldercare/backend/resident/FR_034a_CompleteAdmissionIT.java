package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T048 (FR-034a; research H-02 mục 4, hàng "Hoàn tất tiếp nhận"): chỉ Hoàn tất tiếp nhận khi có đánh giá đầu vào Đã
 * xác nhận và lần gần nhất chưa quá CFG-M01-02 (90 ngày); điều kiện của M01 gộp với điều kiện của module khác.
 */
class FR_034a_CompleteAdmissionIT extends StatusTestBase {

    @Autowired
    TestClockControl clock;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    @Test
    void FR_034a_noConfirmedInitialAssessment_blocksCompleteAdmission() {
        long id = admissionResident();
        user.set(office.user());

        BusinessException e = failure(() -> statusService.completeAdmission(target(id), reason(), noDetails()));

        assertThat(e).isNotNull();
        assertThat(e.errorCode()).isEqualTo("UNMET_CONDITIONS");
        assertThat(unmetCodes(e)).containsExactly("INITIAL_ASSESSMENT_NOT_CONFIRMED");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
    }

    @Test
    void FR_034a_confirmedAssessmentWithinCfgM0102_allowsCompleteAdmission_andMergesOtherConditions() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        one.unmet(new com.eldercare.backend.resident.api.dto.Condition("NO_BED", "Chưa có giường", "BR-M03-01", false));
        user.set(office.user());

        BusinessException blocked = failure(() -> statusService.completeAdmission(target(id), reason(), noDetails()));
        assertThat(unmetCodes(blocked)).containsExactly("NO_BED");

        one.reset();
        var outcome = statusService.completeAdmission(target(id), reason(), noDetails());

        assertThat(outcome.status()).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_034a_assessmentOlderThanCfgM0102_blocksAndNamesBothConditionsWhenOthersAreUnmet() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        clock.advanceBy(Duration.ofDays(91));
        one.unmet(new com.eldercare.backend.resident.api.dto.Condition("NO_BED", "Chưa có giường", "BR-M03-01", false));
        user.set(office.user());

        BusinessException e = failure(() -> statusService.completeAdmission(target(id), reason(), noDetails()));

        assertThat(unmetCodes(e)).containsExactlyInAnyOrder("INITIAL_ASSESSMENT_EXPIRED", "NO_BED");
    }

    @Test
    void FR_034a_atNinetyDays_assessmentIsStillValid() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        clock.advanceBy(Duration.ofDays(89));
        user.set(office.user());

        assertThat(statusService.previewConditions(
                        target(id), com.eldercare.backend.resident.api.dto.StatusCommand.COMPLETE_ADMISSION))
                .isEmpty();
    }
}
