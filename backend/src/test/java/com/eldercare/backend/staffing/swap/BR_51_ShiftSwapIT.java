package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BR-M09-10, FR-019 → FR-025, FR-045: đổi ca người nhận đồng ý, trưởng tầng duyệt, một yêu cầu mở mỗi (ca, nhân viên). */
class BR_51_ShiftSwapIT extends SwapTestBase {

    private String statusOf(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, requestId);
    }

    @Test
    void FR_022_mutualSwapIsAppliedAfterRecipientAgreesAndFloorManagerApproves() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long y = shift(base.plus(Duration.ofDays(1)).plus(Duration.ofHours(7)), 8, null, recipient);
        long id = createMutual(giver, x, recipient, y);
        assertThat(statusOf(id)).isEqualTo("AWAITING_RECIPIENT");

        send("/api/v1/shift-swaps/" + id + "/accept", recipient, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        // FR-021 (f), 015 T002: tỷ lệ phục vụ trước/sau của X, Y do 008 tính (ở đây chưa cấu hình ngưỡng nên báo thiếu
        // nguồn).
        fetch("/api/v1/shift-swaps/" + id + "/checks", head)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffingRatioBeforeAfter.shiftX.shiftId").value(x))
                .andExpect(jsonPath("$.staffingRatioBeforeAfter.shiftY.shiftId").value(y));

        send("/api/v1/shift-swaps/" + id + "/approve", head, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("APPLIED");
        assertThat(inShift(x, recipient)).isTrue();
        assertThat(inShift(x, giver)).isFalse();
        assertThat(inShift(y, giver)).isTrue();
        assertThat(inShift(y, recipient)).isFalse();
    }

    @Test
    void FR_019_recipientNeedsASharedSystemRole() throws Exception {
        TestUsers.TestUser chef = person(Role.KITCHEN_STAFF);
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        send(
                        "/api/v1/shift-swaps",
                        giver,
                        Map.of(
                                "kind",
                                "TAKE_OVER",
                                "shiftXId",
                                x,
                                "recipientId",
                                chef.staffId(),
                                "reason",
                                "[Lý do riêng tư]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROLE_MISMATCH"));
    }
}
