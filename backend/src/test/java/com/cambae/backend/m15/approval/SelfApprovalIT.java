package com.cambae.backend.m15.approval;

import static com.cambae.backend.m15.approval.ApprovalFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * US5 #13; FR-035 (000 T088; quickstart K13, K19): người yêu cầu có quyền duyệt được tự duyệt khi có ý kiến; nhật ký
 * đánh dấu tự duyệt, tra riêng được; loại yêu cầu không cho tự duyệt thì chặn.
 */
class SelfApprovalIT extends ApprovalTestBase {

    private long ownPending() throws Exception {
        user.set(MANAGER);
        long id = api.create(time.today().plusDays(3), false).get("id").asLong();
        api.submit(id).andExpect(status().isOk());
        return id;
    }

    @Test
    void US5_13_selfApproval_requiresComment_andIsMarked() throws Exception {
        long id = ownPending();

        api.approve(id, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        api.approve(id, "Tự duyệt vì gấp")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.selfApproved").value(true));

        assertThat(lastAudit(id, "REQUEST_APPROVED", "JSON_EXTRACT(details, '$.selfApproved')"))
                .isEqualTo("true");
        assertThat(lastAudit(id, "REQUEST_APPROVED", "reason")).isEqualTo("Tự duyệt vì gấp");
        mvc.perform(get("/api/v1/audit-logs")
                        .param("action", "REQUEST_APPROVED")
                        .param("selfApproved", "true")
                        .param("entityId", String.valueOf(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void FR_035_typeDisallowingSelfApproval_isBlocked() throws Exception {
        control.allowSelfApproval = false;
        long id = ownPending();

        api.approve(id, "Tự duyệt")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SELF_APPROVAL_NOT_ALLOWED"));
        mvc.perform(get(ApprovalClient.URL + "/" + id))
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'approve')]").doesNotExist());
    }
}
