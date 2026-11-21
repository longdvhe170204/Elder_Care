package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
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
 * FR-043h (Q-416, R11, R18): ý kiến người cao tuổi ghi riêng, có nguồn; thiếu ý kiến không chặn gửi duyệt nhưng có cảnh báo;
 * ý kiến Không đồng ý thì gửi duyệt bắt buộc lý do; thay đổi nhỏ không cảnh báo; đổi phần thực chất sau khi ghi ý kiến thì
 * cảnh báo ý kiến đã cũ, chỉ đổi lý do thì không.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043h_ResidentOpinionIT extends ChangeTestBase {

    private ObjectNode nonMinor(Setup s, long serviceId, LocalDate effective, String reason) {
        ObjectNode body = request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + serviceId + ", \"action\": \"ADD\"}]}",
                effective);
        body.put("reason", reason);
        return body;
    }

    private ResultActions opinion(long id, String opinion) throws Exception {
        return mvc.perform(post("/api/v1/residency-change-requests/" + id + "/resident-opinions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"opinion\":\"" + opinion + "\",\"source\":\"RESIDENT\",\"note\":\"Ông nói trực tiếp\"}"));
    }

    private JsonNode warnings(long id) throws Exception {
        user.set(office.user());
        return getChange(id).get("warnings");
    }

    @Test
    void R11_missingOpinion_warnsButSubmits() throws Exception {
        Setup s = activeSetup();
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = createChange(nonMinor(s, extra, today().plusDays(5), REASON))
                .get("approvalRequestId")
                .asLong();

        assertThat(warnings(id).toString()).contains("RESIDENT_OPINION_MISSING");
        submit(id);
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void R11_disagree_submitNeedsReason() throws Exception {
        Setup s = activeSetup();
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = createChange(nonMinor(s, extra, today().plusDays(5), REASON))
                .get("approvalRequestId")
                .asLong();
        user.set(office.user());
        opinion(id, "DISAGREE").andExpect(status().isOk());

        MvcResult blocked =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(blocked.getResponse().getContentAsString()).contains("RESIDENT_DISAGREE_REASON_REQUIRED");
        generic(id, "submit", "{\"reason\":\"Đã giải thích, gia đình vẫn đề nghị\"}")
                .andExpect(status().isOk());
    }

    @Test
    void R11_nurseInScopeRecords_accountantForbidden() throws Exception {
        Setup s = activeSetup();
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = createChange(nonMinor(s, extra, today().plusDays(5), REASON))
                .get("approvalRequestId")
                .asLong();

        assignNurse(nurse, s.residentId());
        user.set(nurse.user());
        opinion(id, "AGREE").andExpect(status().isOk());

        user.set(users.staff(Role.ACCOUNTANT).user());
        opinion(id, "AGREE").andExpect(status().isForbidden());
    }

    @Test
    void R11_minorChange_noWarning() throws Exception {
        Setup s = activeSetup();
        String removeMeal = "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}";
        long id = createChange(request(s.contractId(), "[\"SERVICE\"]", removeMeal, today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        assertThat(warnings(id)).isEmpty();
    }

    @Test
    void R18_substantiveChangeAfterOpinion_outdated_reasonOnlyNot() throws Exception {
        Setup s = activeSetup();
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        long id = createChange(nonMinor(s, extra, today().plusDays(5), REASON))
                .get("approvalRequestId")
                .asLong();
        user.set(office.user());
        opinion(id, "AGREE").andExpect(status().isOk());

        edit(id, nonMinor(s, extra, today().plusDays(5), "Lý do ghi rõ hơn"));
        assertThat(warnings(id)).isEmpty();
        assertThat(getChange(id).get("currentResidentOpinion").get("outdated").asBoolean())
                .isFalse();

        edit(id, nonMinor(s, extra, today().plusDays(8), "Lý do ghi rõ hơn"));
        assertThat(warnings(id).toString()).contains("RESIDENT_OPINION_OUTDATED");
        assertThat(getChange(id).get("currentResidentOpinion").get("outdated").asBoolean())
                .isTrue();
    }

    private void edit(long id, ObjectNode body) throws Exception {
        user.set(office.user());
        mvc.perform(put("/api/v1/residency-change-requests/" + id)
                        .with(csrf())
                        .header("If-Match", requestEtag(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk());
    }
}
