package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.api.events.FeedbackNotice;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * Phản hồi, khiếu nại có hạn và leo thang ({@code BR_M10_05_PhanHoiIT}; 012 T029, Q43 → Q46; US6 kịch bản 1 → 9; FR-058
 * → FR-066, FR-067, BR-M10-05).
 */
class BR_M10_05_FeedbackIT extends FeedbackTestBase {

    /** Q43, US6 kịch bản 1: khiếu nại nhóm chăm sóc ở Mới, mức Cao, hạn gửi + 24 giờ, phụ trách Trưởng tầng. */
    @Test
    void Q43_complaintIsHighDue24hAssignedToFloorManager() throws Exception {
        long id = submitted(family(mMember), null, "COMPLAINT", "CARE");
        Instant sent = at(id, "submitted_at");

        JsonNode detail = getJsonAs(staff(floorManager), "/api/v1/feedback/" + id);
        assertThat(detail.get("status").asText()).isEqualTo("NEW");
        assertThat(detail.get("priority").asText()).isEqualTo("HIGH");
        assertThat(Instant.parse(detail.get("dueAt").asText())).isEqualTo(sent.plus(Duration.ofHours(24)));
        assertThat(detail.get("overdue").asBoolean()).isFalse();
        assertThat(detail.get("familyMemberId").asLong()).isEqualTo(mMember);
        assertThat(detail.get("source").asText()).isEqualTo("PORTAL");
        assertThat(row(id).get("assignee_account_id")).isEqualTo(floorManager.accountId());

        List<FeedbackNotice> assigned = notices(id, FeedbackNotice.Kind.ASSIGNED);
        assertThat(assigned).hasSize(1);
        NotificationRequest request = assigned.getFirst().notificationRequests().getFirst();
        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertThat(request.recipientGroups())
                .containsExactly(new RecipientGroup.Staff(floorManager.staffId(), "ASSIGNEE"));
    }

    private List<String> commands(String authorization, long feedbackId) throws Exception {
        List<String> result = new ArrayList<>();
        getJsonAs(authorization, "/api/v1/feedback/" + feedbackId)
                .get("allowedCommands")
                .forEach(c -> result.add(c.asText()));
        return result;
    }

    private long staffAccount(Role role) {
        return users.staff(role).accountId();
    }

    private static List<String> texts(JsonNode detail, String type) {
        List<String> result = new ArrayList<>();
        detail.get("items").forEach(item -> {
            if (item.get("type").asText().equals(type)) {
                result.add(item.get("content").asText());
            }
        });
        return result;
    }

    private static List<Long> ids(JsonNode list) {
        List<Long> result = new ArrayList<>();
        list.forEach(item -> result.add(item.get("id").asLong()));
        return result;
    }
}
