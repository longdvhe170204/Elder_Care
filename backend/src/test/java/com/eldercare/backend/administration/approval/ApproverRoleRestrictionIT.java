package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.events.ApproverReminderDue;
import com.eldercare.backend.administration.approval.api.events.RequestSubmitted;
import com.eldercare.backend.common.Role;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Giới hạn vai trò người duyệt (bổ sung theo plan 004, Q-338; FR-034, FR-034a, FR-035, FR-031a; 000 T105; quickstart
 * K21): Hành chính có quyền duyệt loại mẫu nhưng loại giới hạn {Quản lý viện} thì bị 403 khi Duyệt, Từ chối, tự duyệt;
 * không có trong {@code allowedCommands}, danh sách người duyệt của gửi duyệt và nhắc; tắt giới hạn thì như cũ; giới hạn
 * tính lại ở mỗi lần xét.
 */
class ApproverRoleRestrictionIT extends ApprovalTestBase {

    @Test
    void Q_338_restrictedToManagers_officeStaffCannotDecide_andIsNotAnApprover() throws Exception {
        control.roleRestriction = Set.of(Role.NURSING_HOME_MANAGER);
        long id = pending(time.today().plusDays(30));

        user.set(OFFICE_B);
        mvc.perform(get(ApprovalClient.URL + "/" + id))
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'approve')]").doesNotExist())
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'reject')]").doesNotExist());
        api.approve(id, "Đồng ý").andExpect(status().isForbidden());
        api.reject(id, "Không đồng ý").andExpect(status().isForbidden());
        user.set(OFFICE_A);
        api.approve(id, "Tự duyệt").andExpect(status().isForbidden());

        assertThat(events.stream(RequestSubmitted.class).filter(e -> e.request().id() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.approvers())
                        .contains(MANAGER.accountId())
                        .doesNotContain(OFFICE_A.accountId(), OFFICE_B.accountId()));
        clock.setTo(Instant.parse(read(id).get("submittedAt").asString()).plus(Duration.ofHours(49)));
        runRemindJob();
        assertThat(events.stream(ApproverReminderDue.class)
                        .filter(e -> e.request().id() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.approvers()).doesNotContain(OFFICE_A.accountId(), OFFICE_B.accountId()));

        user.set(MANAGER);
        api.approve(id, "Đồng ý").andExpect(status().isOk());
    }

    @Test
    void FR_034a_restrictionIsEvaluatedAtEachDecision_andOffMeansAsBefore() throws Exception {
        control.roleRestriction = Set.of(Role.NURSING_HOME_MANAGER);
        long id = pending(time.today().plusDays(30));
        user.set(OFFICE_B);
        api.approve(id, "Đồng ý").andExpect(status().isForbidden());

        control.roleRestriction = null;

        api.approve(id, "Đồng ý")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }
}
