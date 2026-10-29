package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.CareLevelChangeRequestNeeded;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import org.junit.jupiter.api.Test;

/**
 * 001 T061 (US6 #12; FR-035, FR-036, BR-M01-03): đánh giá lại ra mức khác khi Tạm vắng vẫn báo Hành chính lập yêu cầu
 * đổi mức, không tự tạo yêu cầu, không đổi mức hiện hành, không đổi trạng thái.
 */
class BR_M01_03_CareLevelChangeIT extends StatusTestBase {

    @Test
    void US6_12_reassessmentWhileTemporarilyAbsent_notifiesOffice_andChangesNothingElse() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Đầu vào", null);
        moveTo(id, ResidentStatus.TEMPORARILY_ABSENT);
        int historyBefore = historyRows(id);

        var confirmed = confirmed(id, AssessmentType.REASSESSMENT, CareLevel.REGULAR, "Cần thêm hỗ trợ", null);

        assertThat(events.stream(CareLevelChangeRequestNeeded.class)).anySatisfy(e -> {
            assertThat(e.residentId()).isEqualTo(id);
            assertThat(e.assessmentId()).isEqualTo(confirmed.id());
            assertThat(e.newLevel()).isEqualTo(CareLevel.REGULAR);
        });
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(currentCareLevel(id)).isEqualTo("BASIC");
        assertThat(historyRows(id)).isEqualTo(historyBefore);
        assertThat(changeRequests.hasOpenRequest(id))
                .as("hệ thống không tự tạo yêu cầu (Q-306)")
                .isFalse();
    }
}
