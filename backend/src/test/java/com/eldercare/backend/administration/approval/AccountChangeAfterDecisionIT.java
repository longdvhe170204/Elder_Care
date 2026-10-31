package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER_2;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.FakePermissions;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * FR-034a (000 T109): tài khoản của người duyệt, người yêu cầu đổi sau khi đã có quyết định hoặc đang chờ duyệt. Bị khóa
 * hoặc mất quyền (002) được giả lập bằng cách thay tài khoản trong bản giả quyền bằng tài khoản cùng id không còn vai trò
 * nào: không còn quyền nào và không còn là người duyệt.
 */
class AccountChangeAfterDecisionIT extends ApprovalTestBase {

    @Autowired
    FakePermissions permissions;

    @AfterEach
    void restoreAccounts() {
        permissions.account(MANAGER_2).account(OFFICE_A);
    }

    @Test
    void FR_034a_decisionStands_whenApproverLaterLosesPermissionOrIsLocked() throws Exception {
        LocalDate effective = time.today().plusDays(2);
        long approvedId = pending(effective);
        long rejectedId = pending(effective);
        long other = pending(time.today().plusDays(30));
        user.set(MANAGER_2);
        api.approve(approvedId, "Đồng ý").andExpect(status().isOk());
        api.reject(rejectedId, "Không đồng ý").andExpect(status().isOk());

        permissions.account(FakePermissions.user(MANAGER_2.accountId()));
        user.set(FakePermissions.user(MANAGER_2.accountId()));
        api.approve(other, "Đồng ý").andExpect(status().isForbidden());
        moveTo(effective);
        runApplyJob();

        assertThat(read(approvedId).get("status").asString()).isEqualTo("APPLIED");
        assertThat(read(approvedId).get("decidedBy").get("id").asLong()).isEqualTo(MANAGER_2.accountId());
        assertThat(effects(approvedId)).isEqualTo(1);
        assertThat(read(rejectedId).get("status").asString()).isEqualTo("REJECTED");
        assertThat(read(rejectedId).get("decidedBy").get("id").asLong()).isEqualTo(MANAGER_2.accountId());
    }

    @Test
    void FR_034a_requesterLockedOrWithoutCreatePermission_requestsKeepTheirCourse() throws Exception {
        LocalDate soon = time.today().plusDays(2);
        long approvedId = approved(soon);
        long toApprove = pending(time.today().plusDays(30));
        long toReject = pending(time.today().plusDays(30));

        permissions.account(FakePermissions.user(OFFICE_A.accountId()));
        user.set(FakePermissions.user(OFFICE_A.accountId()));
        api.createRaw("""
                        {"type":"M15.SAMPLE","content":{"key":"locked"},"requestedEffectiveDate":"%s",\
                        "reason":"Thử sau khi mất quyền"}""".formatted(soon)).andExpect(status().isForbidden());
        // Không tự đổi trạng thái khi các job chạy
        runRemindJob();
        runApplyJob();
        assertThat(read(approvedId).get("status").asString()).isEqualTo("APPROVED");
        assertThat(read(toApprove).get("status").asString()).isEqualTo("PENDING_APPROVAL");
        assertThat(read(toReject).get("status").asString()).isEqualTo("PENDING_APPROVAL");

        user.set(MANAGER);
        api.approve(toApprove, "Đồng ý")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        api.reject(toReject, "Không đồng ý")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        moveTo(soon);
        runApplyJob();
        assertThat(read(approvedId).get("status").asString()).isEqualTo("APPLIED");
        assertThat(effects(approvedId)).isEqualTo(1);
    }
}
