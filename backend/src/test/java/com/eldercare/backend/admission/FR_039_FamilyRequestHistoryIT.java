package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.TestUsers.TestUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * Người đại diện xem lịch sử yêu cầu thay đổi lưu trú của hợp đồng người mình đại diện trên ứng dụng (FR-039, 000 FR-042:
 * người lập xem được yêu cầu của mình); không xem được yêu cầu của hợp đồng người khác.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_039_FamilyRequestHistoryIT extends ChangeTestBase {

    private JsonNode list(String query) throws Exception {
        return JSON.readTree(mvc.perform(get("/api/v1/residency-change-requests" + query))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void FR_039_representativeSeesOwnRequests_notOtherContracts() throws Exception {
        Setup s = activeSetup();
        Setup other = activeSetup();
        long id = createChange(request(
                        s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        TestUser representative = users.family(representativeOf(s.residentId()));
        jdbc.update("UPDATE approval_request SET requester_id = ? WHERE id = ?", representative.accountId(), id);

        user.set(representative.user());
        assertThat(list("?contractId=" + s.contractId()))
                .anyMatch(r -> r.get("approvalRequestId").asLong() == id);

        user.set(users.family(representativeOf(other.residentId())).user());
        assertThat(list("?contractId=" + s.contractId())).isEmpty();
        assertThat(list("")).noneMatch(r -> r.get("approvalRequestId").asLong() == id);
    }
}
