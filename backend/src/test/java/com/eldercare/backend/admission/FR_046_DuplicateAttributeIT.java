package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T043 (US5 #7; FR-046): người cao tuổi chỉ có một yêu cầu Đã duyệt (chờ hiệu lực) cho mỗi thuộc tính; yêu cầu thứ hai
 * cùng thuộc tính bị chặn khi duyệt (409) cho tới khi yêu cầu đầu bị hủy hoặc đã áp dụng; thuộc tính khác không vướng.
 * {@code UNIQUE} của cơ sở dữ liệu là lưới an toàn.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_046_DuplicateAttributeIT extends ChangeTestBase {

    private long secondCareLevelRequest(long contractId, LocalDate effective) throws Exception {
        long id = createChange(request(contractId, "[\"CARE_LEVEL\"]", "{\"careLevel\": \"SPECIAL\"}", effective))
                .get("approvalRequestId")
                .asLong();
        submit(id);
        return id;
    }

    @Test
    void FR_046_cancellingTheFirstRequest_releasesTheAttribute_andClosesItsAddendum() throws Exception {
        Setup s = activeSetup();
        service("CARE", "Chăm sóc đặc biệt", "SPECIAL", 12_000_000);
        long first = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(5)));
        long second = secondCareLevelRequest(s.contractId(), today().plusDays(6));

        user.set(manager.user());
        generic(first, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        awaitAddendumStatus(first, "NOT_APPLIED");
        approveRaw(second).andExpect(status().isOk());

        assertThat(requestStatus(second)).isEqualTo("APPROVED");
        assertThat(addenda(s.contractId())).hasSize(2);
    }

    private int approveAs(SecurityContext security, CountDownLatch start, long requestId, String etag)
            throws Exception {
        TestSecurityContextHolder.setContext(security);
        start.await();
        return mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đồng ý\"}"))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private void awaitAddendumStatus(long requestId, String expected) {
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT addendum_status FROM contract_version WHERE kind = 'ADDENDUM' AND approval_request_id = ?",
                                String.class,
                                requestId))
                        .isEqualTo(expected));
    }
}
