package com.eldercare.backend.admission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * "Đổi api" 004 (A duyệt 2026-11-07; 004 T047): danh sách yêu cầu thay đổi lưu trú lọc theo hợp đồng và trạng thái, để màn
 * web tìm lại yêu cầu mà không cần nhớ mã.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyChangeListIT extends ChangeTestBase {

    @Test
    void FR_038_listChangeRequests_filtersByContractAndStatus() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(
                        s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();

        user.set(office.user());
        mvc.perform(get("/api/v1/residency-change-requests")
                        .param("contractId", String.valueOf(s.contractId()))
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].approvalRequestId").value(id))
                .andExpect(jsonPath("$[0].contractId").value(s.contractId()));
        mvc.perform(get("/api/v1/residency-change-requests")
                        .param("contractId", String.valueOf(s.contractId()))
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
