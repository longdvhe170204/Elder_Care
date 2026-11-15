package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/**
 * 001 T044 (FR-045, SC-002; research H-02 mục 3, 6): lệnh chưa đủ điều kiện liệt kê mọi điều kiện chưa thỏa của mọi
 * module trong một lần và giữ nguyên trạng thái; xem trước dùng cùng bộ kiểm tra mà không đổi dữ liệu.
 */
@Import(CostParticipantOff.class)
class FR_045_ListConditionsIT extends StatusTestBase {

    private static final Condition BELONGINGS =
            new Condition("BELONGINGS_NOT_RETURNED", "Còn đồ gửi", "BR-M12-03", false);
    private static final Condition MEDICINE =
            new Condition("MEDICINE_NOT_RETURNED", "Còn thuốc gửi", "BR-M07-01", true);

    @Test
    void FR_045_everyUnmetConditionOfEveryParticipant_isListedInOneAnswer_andStateIsUntouched() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        one.unmet(BELONGINGS);
        two.unmet(MEDICINE);
        user.set(office.user());
        int rows = historyCount(id);

        BusinessException e = failure(() -> statusService.endResidency(target(id), reason(), noDetails()));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.errorCode()).isEqualTo("UNMET_CONDITIONS");
        assertThat(unmetCodes(e)).containsExactlyInAnyOrder("BELONGINGS_NOT_RETURNED", "MEDICINE_NOT_RETURNED");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(historyCount(id)).isEqualTo(rows);
        assertThat(one.applied()).isEmpty();
        assertThat(two.applied()).isEmpty();
    }
}
