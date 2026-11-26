package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/**
 * Demo 2026-11-26 (E, chờ B, chủ dự án duyệt): người đại diện gửi yêu cầu kết thúc lưu trú trên app (api-004
 * {@code /family-portal/residents/{residentId}/residency-ending-requests}); Hành chính được báo; người đại diện hủy được
 * khi còn Chờ xử lý. Chỉ người đại diện hiện tại; mỗi người cao tuổi một yêu cầu đang chờ.
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingRequestIT extends EndingTestBase {

    @Autowired
    ApplicationEvents applicationEvents;

    private String portal(long residentId) {
        return "/api/v1/family-portal/residents/" + residentId + "/residency-ending-requests";
    }

    private final java.util.Map<Long, TestUser> accounts = new java.util.HashMap<>();

    private void as(long familyMemberId) {
        user.set(accounts.computeIfAbsent(familyMemberId, users::family).user());
    }

    private ResultActions submit(long familyMemberId, long residentId, LocalDate date) throws Exception {
        as(familyMemberId);
        return mvc.perform(post(portal(residentId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"endCase\":\"CONTRACT_EXPIRED\",\"expectedEndDate\":\"" + date
                        + "\",\"reason\":\"Gia đình đón về chăm sóc tại nhà\"}"));
    }

    private ResultActions cancel(long familyMemberId, long requestId) throws Exception {
        as(familyMemberId);
        return mvc.perform(post("/api/v1/family-portal/residency-ending-requests/" + requestId + "/cancel")
                .with(csrf()));
    }

    private List<NotificationRequest> notices(long residentId, String eventType) {
        return applicationEvents.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(r -> r.eventType().equals(eventType) && r.residentId().orElse(0L) == residentId)
                .toList();
    }

    private static long idOf(ResultActions result) throws Exception {
        return JSON.readTree(result.andReturn().getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    @Test
    void representativeSubmits_pending_officeNotified_secondWhilePendingRejected() throws Exception {
        long residentId = inResidence();
        long representative = representativeOf(residentId);
        LocalDate date = d0.plusDays(5);

        long id = idOf(submit(representative, residentId, date)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.residentId").value(residentId))
                .andExpect(jsonPath("$.familyMemberId").value(representative))
                .andExpect(jsonPath("$.endCase").value("CONTRACT_EXPIRED"))
                .andExpect(jsonPath("$.expectedEndDate").value(date.toString())));

        assertThat(notices(residentId, "M02_ENDING_REQUEST_SUBMITTED"))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
                    assertThat(n.sourceRecord().type()).isEqualTo("M02.ENDING_REQUEST");
                    assertThat(n.sourceRecord().id()).isEqualTo(Long.toString(id));
                    assertThat(n.content().getFirst().content())
                            .contains("yêu cầu kết thúc lưu trú")
                            .contains("Gia đình đón về chăm sóc tại nhà");
                });

        submit(representative, residentId, date)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ENDING_REQUEST_ALREADY_PENDING"));
    }

    @Test
    void notRepresentative_forbidden() throws Exception {
        long residentId = inResidence();
        long other = family().familyMember("Người thân khác " + residentId, "0907" + residentId % 1_000_000);

        submit(other, residentId, d0.plusDays(5)).andExpect(status().isForbidden());
    }

    @Test
    void representativeListsAndCancels_officeNotified_cancelTwiceRejected() throws Exception {
        long residentId = inResidence();
        long representative = representativeOf(residentId);
        long id = idOf(submit(representative, residentId, d0.plusDays(5)).andExpect(status().isCreated()));

        as(representative);
        JsonNode list = JSON.readTree(mvc.perform(get(portal(residentId)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        assertThat(list.get(0).get("id").asLong()).isEqualTo(id);

        cancel(representative, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(notices(residentId, "M02_ENDING_REQUEST_CANCELLED")).hasSize(1);
        cancel(representative, id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ENDING_REQUEST_NOT_PENDING"));
    }
}
