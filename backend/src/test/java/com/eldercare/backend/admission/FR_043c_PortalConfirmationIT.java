package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.ResidencyChangeConfirmationAnswered;
import com.eldercare.backend.admission.api.events.ResidencyChangeConfirmationReminder;
import com.eldercare.backend.admission.api.events.ResidencyChangeConfirmationRequested;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Người đại diện xác nhận yêu cầu thay đổi lưu trú trên ứng dụng (FR-043c, D đề nghị 2026-11-20): Hành chính gửi yêu cầu
 * ở Nháp cho người đại diện; người đại diện đồng ý (thay cho xác nhận kèm bằng chứng) hoặc không đồng ý kèm lý do; sửa
 * nháp thì lần gửi cũ bị thay thế; chưa trả lời quá CFG-M15-05 thì nhắc một lần.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043c_PortalConfirmationIT extends ChangeTestBase {

    private static final String PORTAL = "/api/v1/family-portal/residency-change-confirmations";

    private ObjectNode unconfirmedCareLevel(Setup s) {
        ObjectNode body =
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(2));
        body.remove("representativeConfirmation");
        return body;
    }

    private ResultActions sendRaw(long id) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/residency-change-requests/" + id + "/send-for-confirmation")
                .with(csrf())
                .header("If-Match", requestEtag(id)));
    }

    private void asRepresentative(Setup s) {
        TestUser representative = users.family(representativeOf(s.residentId()));
        user.set(representative.user());
    }

    private JsonNode pending() throws Exception {
        MvcResult result = mvc.perform(get(PORTAL)).andExpect(status().isOk()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private long pendingIdOf(long approvalRequestId) throws Exception {
        for (JsonNode item : pending()) {
            if (item.get("approvalRequestId").asLong() == approvalRequestId) {
                return item.get("id").asLong();
            }
        }
        throw new AssertionError("Không thấy yêu cầu " + approvalRequestId + " trong danh sách chờ xác nhận");
    }

    private ResultActions answer(long confirmationId, String command, String body) throws Exception {
        return mvc.perform(post(PORTAL + "/" + confirmationId + "/" + command)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void FR_043c_sent_representativeAccepts_officeNotified_submitAllowed() throws Exception {
        Setup s = activeSetup();
        long id = createChange(unconfirmedCareLevel(s)).get("approvalRequestId").asLong();

        MvcResult sent = sendRaw(id).andExpect(status().isOk()).andReturn();
        JsonNode body = JSON.readTree(sent.getResponse().getContentAsString());
        assertThat(body.get("confirmationRequest").get("status").asString()).isEqualTo("AWAITING");
        assertThat(events.stream(ResidencyChangeConfirmationRequested.class))
                .anyMatch(e -> e.requestId() == id
                        && e.notificationRequests().getFirst().recipientGroups().stream()
                                .anyMatch(g -> g.equals(new RecipientGroup.Representative(s.residentId()))));

        asRepresentative(s);
        JsonNode item = null;
        for (JsonNode candidate : pending()) {
            if (candidate.get("approvalRequestId").asLong() == id) {
                item = candidate;
            }
        }
        assertThat(item).isNotNull();
        assertThat(item.get("changeTypes").get(0).asString()).isEqualTo("CARE_LEVEL");
        assertThat(item.get("newValue").get("careLevel").asString()).isEqualTo("REGULAR");
        assertThat(item.has("costImpact")).isTrue();
        answer(item.get("id").asLong(), "accept", "{}").andExpect(status().isOk());

        user.set(office.user());
        JsonNode after = getChange(id);
        assertThat(after.get("confirmationRequest").get("status").asString()).isEqualTo("ACCEPTED");
        assertThat(after.get("confirmationRequest").hasNonNull("respondedAt")).isTrue();
        assertThat(events.stream(ResidencyChangeConfirmationAnswered.class))
                .anyMatch(e -> e.requestId() == id && e.accepted());
        generic(id, "submit", "{}").andExpect(status().isOk());
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void FR_043c_representativeDeclines_withReason_submitStillBlocked() throws Exception {
        Setup s = activeSetup();
        long id = createChange(unconfirmedCareLevel(s)).get("approvalRequestId").asLong();
        sendRaw(id).andExpect(status().isOk());

        asRepresentative(s);
        long confirmationId = pendingIdOf(id);
        answer(confirmationId, "decline", "{}").andExpect(status().is4xxClientError());
        answer(confirmationId, "decline", "{\"reason\": \"Gia đình muốn giữ mức cũ\"}")
                .andExpect(status().isOk());
        assertThat(pending()).noneMatch(i -> i.get("approvalRequestId").asLong() == id);

        user.set(office.user());
        JsonNode request = getChange(id).get("confirmationRequest");
        assertThat(request.get("status").asString()).isEqualTo("DECLINED");
        assertThat(request.get("declineReason").asString()).isEqualTo("Gia đình muốn giữ mức cũ");
        assertThat(events.stream(ResidencyChangeConfirmationAnswered.class))
                .anyMatch(e -> e.requestId() == id && !e.accepted());
        MvcResult blocked =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(unmetCodes(blocked)).containsExactly("REPRESENTATIVE_CONFIRMATION_REQUIRED");
    }

    @Test
    void FR_043c_editAfterAccept_supersedesIt_submitBlockedAgain() throws Exception {
        Setup s = activeSetup();
        long id = createChange(unconfirmedCareLevel(s)).get("approvalRequestId").asLong();
        sendRaw(id).andExpect(status().isOk());
        asRepresentative(s);
        answer(pendingIdOf(id), "accept", "{}").andExpect(status().isOk());

        user.set(office.user());
        mvc.perform(put("/api/v1/residency-change-requests/" + id)
                        .with(csrf())
                        .header("If-Match", requestEtag(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unconfirmedCareLevel(s).toString()))
                .andExpect(status().isOk());

        assertThat(getChange(id).get("confirmationRequest").get("status").asString())
                .isEqualTo("SUPERSEDED");
        generic(id, "submit", "{}").andExpect(status().isConflict());
    }

    @Test
    void FR_043c_otherFamilyMember_cannotSeeOrAnswer() throws Exception {
        Setup s = activeSetup();
        Setup other = activeSetup();
        long id = createChange(unconfirmedCareLevel(s)).get("approvalRequestId").asLong();
        sendRaw(id).andExpect(status().isOk());
        asRepresentative(s);
        long confirmationId = pendingIdOf(id);

        asRepresentative(other);
        assertThat(pending()).noneMatch(i -> i.get("approvalRequestId").asLong() == id);
        answer(confirmationId, "accept", "{}").andExpect(status().isForbidden());
    }

    @Test
    void FR_043c_minorChange_needsNoSending() throws Exception {
        Setup s = activeSetup();
        // Bớt suất ăn thuộc gói của hợp đồng: thay đổi nhỏ (R19: dấu thuộc gói theo hợp đồng hiện hành)
        ObjectNode body = request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}",
                today().plusDays(3));
        body.remove("representativeConfirmation");
        long id = createChange(body).get("approvalRequestId").asLong();

        MvcResult result = sendRaw(id).andExpect(status().isConflict()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("CONFIRMATION_NOT_REQUIRED");
    }

    @Test
    void FR_043c_noAnswerAfterThreshold_remindedOnce() throws Exception {
        Setup s = activeSetup();
        noonOf(today());
        long id = createChange(unconfirmedCareLevel(s)).get("approvalRequestId").asLong();
        sendRaw(id).andExpect(status().isOk());

        noonOf(today().plusDays(1));
        runDailyJob();
        assertThat(remindersOf(id)).isEmpty();

        noonOf(today().plusDays(1));
        runDailyJob();
        noonOf(today().plusDays(1));
        runDailyJob();
        List<ResidencyChangeConfirmationReminder> reminders = remindersOf(id);
        assertThat(reminders).hasSize(1);
        assertThat(reminders.getFirst().notificationRequests().getFirst().recipientGroups())
                .contains(new RecipientGroup.Representative(s.residentId()));
    }

    private List<ResidencyChangeConfirmationReminder> remindersOf(long requestId) {
        return events.stream(ResidencyChangeConfirmationReminder.class)
                .filter(r -> r.requestId() == requestId)
                .toList();
    }
}
