package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.internal.change.domain.ResidencyChangeDigest;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * FR-043i (Q-418, R12): yêu cầu người đại diện gửi qua cổng mà Hành chính đổi phần thực chất (ngày hiệu lực, giường, dịch
 * vụ…) thì phải xác nhận người đại diện mới gửi duyệt được; chỉ đổi lý do, ghi chú thì không. Như {@code FR_043b}, lập bằng
 * Hành chính rồi đổi người yêu cầu thành tài khoản người thân và ghi dấu nội dung như lúc gửi qua cổng.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043i_PortalReconfirmIT extends ChangeTestBase {

    private ObjectNode body(Setup s, long serviceId, LocalDate effective, String reason) {
        ObjectNode body = request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + serviceId + ", \"action\": \"ADD\"}]}",
                effective);
        body.remove("representativeConfirmation");
        body.put("reason", reason);
        return body;
    }

    /** Yêu cầu "gửi qua cổng": người yêu cầu là tài khoản người đại diện, kèm dấu nội dung lúc gửi. */
    private long portalRequest(Setup s, ObjectNode body) throws Exception {
        long id = createChange(body).get("approvalRequestId").asLong();
        TestUser representative = users.family(representativeOf(s.residentId()));
        jdbc.update("UPDATE approval_request SET requester_id = ? WHERE id = ?", representative.accountId(), id);
        JsonNode content = JSON.readTree(
                jdbc.queryForObject("SELECT content FROM approval_request WHERE id = ?", String.class, id));
        LocalDate date = jdbc.queryForObject(
                "SELECT requested_effective_date FROM approval_request WHERE id = ?", LocalDate.class, id);
        jdbc.update(
                "INSERT INTO residency_change_portal_digest (approval_request_id, digest, created_at) VALUES (?, ?, NOW(6))",
                id,
                ResidencyChangeDigest.of(content, date));
        return id;
    }

    private ResultActions edit(long id, ObjectNode body) throws Exception {
        user.set(office.user());
        return mvc.perform(put("/api/v1/residency-change-requests/" + id)
                .with(csrf())
                .header("If-Match", requestEtag(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    @Test
    void R12_officeChangesEffectiveDate_needsConfirmation() throws Exception {
        Setup s = activeSetup();
        long extra = service("ACTIVITY", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = portalRequest(s, body(s, extra, today().plusDays(5), REASON));

        edit(id, body(s, extra, today().plusDays(9), REASON)).andExpect(status().isOk());

        assertThat(getChange(id).get("representativeConfirmationRequired").asBoolean())
                .isTrue();
        MvcResult result =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("REPRESENTATIVE_CONFIRMATION_REQUIRED");

        ObjectNode confirmed = body(s, extra, today().plusDays(9), REASON);
        confirmed.set("representativeConfirmation", confirmation(today(), representativeOf(s.residentId())));
        edit(id, confirmed).andExpect(status().isOk());
        generic(id, "submit", "{}").andExpect(status().isOk());
    }

    @Test
    void R12_officeChangesOnlyReason_noConfirmationNeeded() throws Exception {
        Setup s = activeSetup();
        long extra = service("ACTIVITY", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = portalRequest(s, body(s, extra, today().plusDays(5), REASON));

        edit(id, body(s, extra, today().plusDays(5), "Gia đình đề nghị, Hành chính bổ sung ghi chú"))
                .andExpect(status().isOk());

        assertThat(getChange(id).get("representativeConfirmationRequired").asBoolean())
                .isFalse();
        generic(id, "submit", "{}").andExpect(status().isOk());
    }
}
