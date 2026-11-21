package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.ResidencyChangeReturnedToDraft;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/**
 * FR-043e (Q-418, R8): Quản lý viện trả yêu cầu Chờ duyệt về Nháp kèm lý do; xác nhận người đại diện cũ bị bỏ, gửi duyệt
 * lại thì bị chặn tới khi xác nhận lại; người lập và Hành chính được báo. Hành chính không trả về được yêu cầu không phải
 * thay đổi nhỏ (không có quyền duyệt yêu cầu đó).
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043e_ReturnToDraftIT extends ChangeTestBase {

    private long pendingNonMinor(Setup s) throws Exception {
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = createChange(request(
                        s.contractId(),
                        "[\"SERVICE\"]",
                        "{\"services\": [{\"serviceId\": " + extra + ", \"action\": \"ADD\"}]}",
                        today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);
        return id;
    }

    @Test
    void R8_managerReturnsToDraft_confirmationVoided_resubmitNeedsNewConfirmation() throws Exception {
        Setup s = activeSetup();
        long id = pendingNonMinor(s);

        user.set(manager.user());
        generic(id, "return-to-draft", "{\"reason\":\"Sửa ngày hiệu lực\"}").andExpect(status().isOk());

        assertThat(requestStatus(id)).isEqualTo("DRAFT");
        JsonNode view = getChange(id);
        assertThat(view.has("representativeConfirmation"))
                .as("xác nhận cũ bị bỏ")
                .isFalse();
        assertThat(events.stream(ResidencyChangeReturnedToDraft.class))
                .anyMatch(e -> e.requestId() == id && e.reason().equals("Sửa ngày hiệu lực"));

        user.set(office.user());
        MvcResult blocked =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(blocked.getResponse().getContentAsString()).contains("REPRESENTATIVE_CONFIRMATION_REQUIRED");
    }

    @Test
    void R8_officeCannotReturnNonMinorRequest_403() throws Exception {
        Setup s = activeSetup();
        long id = pendingNonMinor(s);

        user.set(office.user());
        generic(id, "return-to-draft", "{\"reason\":\"Sửa\"}").andExpect(status().isForbidden());
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }
}
