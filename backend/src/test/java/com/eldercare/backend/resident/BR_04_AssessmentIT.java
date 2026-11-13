package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import org.junit.jupiter.api.Test;

/**
 * 001 T034 (US3; BR-M01-09, FR-024 → FR-029, FR-031, DBR-05, Q-306): điều dưỡng lập nháp, chỉ Bác sĩ xác nhận; thiếu
 * nhận xét hoặc mức thì không xác nhận được và {@code CHECK} ở MySQL chặn một lớp nữa; xác nhận đặt mức hiện hành và dòng
 * lịch sử cùng transaction; không tạo bản ghi kế hoạch chăm sóc.
 */
class BR_04_AssessmentIT extends AssessmentTestBase {

    private long admissionResident() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        return id;
    }

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.administration.correction.api.CorrectionApi corrections;

    /**
     * FR-049a (US4 #12; quickstart 001 Q20, B1): hồ sơ trạng thái cuối, chính Bác sĩ đã xác nhận đính chính lần đánh giá
     * thì thành yêu cầu phê duyệt (cách C của 000 FR-026), không bị chặn 403; bản gốc chưa đổi.
     */
    @Test
    void FR_049a_recorderCorrectsOnFinalProfile_becomesApprovalRequest() {
        long resident = admissionResident();
        AssessmentView done = confirmed(resident, AssessmentType.INITIAL, CareLevel.REGULAR, "Nhận xét gốc", null);
        setStatus(resident, "DECEASED");
        user.set(doctor.user());

        var result = corrections.create(
                com.eldercare.backend.resident.internal.assessment.service.AssessmentService.RECORD_TYPE,
                done.id(),
                com.eldercare.backend.administration.correction.api.dto.CorrectionType.AMEND_CONTENT,
                java.util.Map.of("comment", "Nhận xét đính chính"),
                new com.eldercare.backend.common.Reason("Ghi nhầm nhận xét"));

        assertThat(result)
                .isInstanceOf(
                        com.eldercare.backend.administration.correction.api.CorrectionResult.PendingApproval.class);
        assertThat(jdbc.queryForObject("SELECT comment FROM assessment WHERE id = ?", String.class, done.id()))
                .isEqualTo("Nhận xét gốc");
    }
}
