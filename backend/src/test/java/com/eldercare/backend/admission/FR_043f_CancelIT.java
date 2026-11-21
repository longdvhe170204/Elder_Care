package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * FR-043f (Q-420, R9): Hành chính khác người lập, Quản lý viện hủy được yêu cầu Nháp, Chờ duyệt; Điều dưỡng không; Đã duyệt
 * do người duyệt. Hủy khi phụ lục Chờ ký, Chờ hiệu lực thì phụ lục Đã hủy, thuộc tính được nhả (lập lại cùng loại được).
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043f_CancelIT extends ChangeTestBase {

    private long draftNonMinor(Setup s) throws Exception {
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        return createChange(request(
                        s.contractId(),
                        "[\"SERVICE\"]",
                        "{\"services\": [{\"serviceId\": " + extra + ", \"action\": \"ADD\"}]}",
                        today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
    }

    @Test
    void R9_otherOfficeStaff_cancelsDraft_nurseCannot() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);

        user.set(users.staff(Role.NURSING_STAFF).user());
        generic(id, "cancel", "{\"reason\":\"Không cần\"}").andExpect(status().isForbidden());

        user.set(users.staff(Role.OFFICE_STAFF).user());
        generic(id, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        assertThat(requestStatus(id)).isEqualTo("CANCELLED");
    }

    @Test
    void R9_managerCancelsPending() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);
        submit(id);

        user.set(manager.user());
        generic(id, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        assertThat(requestStatus(id)).isEqualTo("CANCELLED");
    }

    @Test
    void R9_cancelApproved_pendingSignatureAddendumCancelled_attributeReleased() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);
        submit(id);
        managerApproves(id);
        assertThat(addendumStatus(id)).isEqualTo("PENDING_SIGNATURE");

        user.set(manager.user());
        generic(id, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());

        assertThat(requestStatus(id)).isEqualTo("CANCELLED");
        assertThat(addendumStatus(id)).isEqualTo("CANCELLED");
        long again = createApproveUnsigned(request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": "
                        + service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000)
                        + ", \"action\": \"ADD\"}]}",
                today().plusDays(6)));
        assertThat(requestStatus(again)).as("thuộc tính đã được nhả (FR-046)").isEqualTo("APPROVED");
    }

    @Test
    void R9_cancelApproved_pendingEffectiveAddendumCancelled() throws Exception {
        Setup s = activeSetup();
        long id = draftNonMinor(s);
        submit(id);
        managerApproves(id);
        sign(id, today()).andExpect(status().isOk());
        assertThat(addendumStatus(id)).isEqualTo("PENDING_EFFECTIVE");

        user.set(manager.user());
        generic(id, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        assertThat(addendumStatus(id)).isEqualTo("CANCELLED");
    }
}
