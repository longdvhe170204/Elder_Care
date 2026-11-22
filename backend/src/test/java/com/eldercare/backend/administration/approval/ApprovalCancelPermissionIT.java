package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.common.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Quyền hủy Nháp, Chờ duyệt do loại yêu cầu quy định (000 FR-041a (4); Q-421), mặc định chỉ người yêu cầu; lý do khi gửi
 * duyệt truyền cho loại yêu cầu (FR-041a (5)).
 */
class ApprovalCancelPermissionIT extends ApprovalTestBase {

    @Test
    void FR_041a_defaultOnlyRequesterCancels() throws Exception {
        long id = pending(time.today().plusDays(5));
        user.set(OFFICE_B);
        api.cancel(id, "Không cần nữa").andExpect(status().isForbidden());
        assertThat(read(id).get("allowedCommands").toString()).doesNotContain("cancel");
    }

    @Test
    void FR_041a_typeAllowsRole_otherStaffCancelsPending_andSeesCommand() throws Exception {
        control.cancelRoles = Set.of(Role.OFFICE_STAFF);
        long id = pending(time.today().plusDays(5));

        user.set(OFFICE_B);
        assertThat(api.read(id).get("allowedCommands").toString()).contains("cancel");
        api.cancel(id, "Gia đình đổi ý")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ApprovalStatus.CANCELLED));
    }

    @Test
    void FR_041a_submitReason_passedToType() throws Exception {
        control.submitReasonRequired = true;
        user.set(OFFICE_A);
        long id = api.create(time.today().plusDays(5), false).get("id").asLong();

        api.submit(id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("SUBMIT_REASON_REQUIRED"));
        api.submitWithReason(id, "Người cao tuổi không đồng ý, đã giải thích")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ApprovalStatus.PENDING_APPROVAL));
        user.set(MANAGER);
        assertThat(lastAudit(id, "REQUEST_SUBMITTED", "reason"))
                .isEqualTo("Người cao tuổi không đồng ý, đã giải thích");
    }
}
