package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.NURSE;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.events.RequestReturnedToDraft;
import com.eldercare.backend.common.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Trả về nháp (000 FR-041a (1); Q-418): chỉ loại yêu cầu cho phép, người có quyền duyệt (kể cả giới hạn vai trò), bắt
 * buộc lý do; yêu cầu về Nháp sửa và gửi lại được; handler được báo, sự kiện phát, nhật ký ghi lý do.
 */
class ApprovalReturnToDraftIT extends ApprovalTestBase {

    @Test
    void FR_041a_typeAllows_deciderReturnsToDraft_handlerNotified_eventAndAudit() throws Exception {
        control.allowReturnToDraft = true;
        long id = pending(time.today().plusDays(5));

        user.set(MANAGER);
        api.returnToDraft(id, "Sửa ngày hiệu lực")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ApprovalStatus.DRAFT));

        assertThat(control.returnedToDraft).containsExactly(id);
        assertThat(events.stream(RequestReturnedToDraft.class)
                        .filter(e -> e.request().id() == id)
                        .count())
                .isEqualTo(1);
        assertThat(lastAudit(id, "REQUEST_RETURNED_TO_DRAFT", "reason")).isEqualTo("Sửa ngày hiệu lực");

        // Người yêu cầu sửa và gửi lại được
        user.set(OFFICE_A);
        api.submit(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ApprovalStatus.PENDING_APPROVAL));
    }

    @Test
    void FR_041a_typeDoesNotAllow_409() throws Exception {
        long id = pending(time.today().plusDays(5));

        user.set(MANAGER);
        api.returnToDraft(id, "Sửa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RETURN_TO_DRAFT_NOT_ALLOWED"));
        assertThat(read(id).get("status").asString()).isEqualTo(ApprovalStatus.PENDING_APPROVAL);
    }

    @Test
    void FR_041a_notDecider_403_andRoleRestrictionApplies() throws Exception {
        control.allowReturnToDraft = true;
        long id = pending(time.today().plusDays(5));

        user.set(NURSE);
        api.returnToDraft(id, "Sửa").andExpect(status().isForbidden());

        control.roleRestriction = Set.of(Role.NURSING_HOME_MANAGER);
        user.set(ApprovalFixtures.OFFICE_B);
        api.returnToDraft(id, "Sửa").andExpect(status().isForbidden());
        assertThat(control.returnedToDraft).isEmpty();
    }

    @Test
    void FR_041a_reasonRequired_andOnlyFromPending() throws Exception {
        control.allowReturnToDraft = true;
        long id = pending(time.today().plusDays(5));
        user.set(MANAGER);
        api.returnToDraft(id, null).andExpect(status().is4xxClientError());

        api.approve(id, "Đồng ý").andExpect(status().isOk());
        api.returnToDraft(id, "Sửa").andExpect(status().isConflict());
    }

    @Test
    void FR_041a_allowedCommands_listReturnToDraftForDecider() throws Exception {
        control.allowReturnToDraft = true;
        long id = pending(time.today().plusDays(5));
        assertThat(read(id).get("allowedCommands").toString()).contains("return-to-draft");

        control.allowReturnToDraft = false;
        assertThat(read(id).get("allowedCommands").toString()).doesNotContain("return-to-draft");
    }
}
