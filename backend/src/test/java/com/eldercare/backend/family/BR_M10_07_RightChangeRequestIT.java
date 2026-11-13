package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.internal.scheduling.service.M10TimedTask;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * 012 T014 (BR-M10-07; FR-005, FR-006, FR-013 → FR-020, FR-069, FR-075; quickstart Q8 → Q12; US3 kịch bản 1 → 8): vòng
 * đời yêu cầu thay đổi quyền qua REST với phiên thật của Hành chính, Quản lý viện và người thân.
 */
class BR_M10_07_RightChangeRequestIT extends FamilyCommandTestBase {

    @Autowired
    List<M10TimedTask> tasks;

    long a;
    long rMember;
    long r;
    long sMember;
    long s;
    long nMember;
    long n;
    String manager;

    /** A có R (người đại diện, liên hệ chính), S (người đại diện), N (người thân thường, được phép đón). */
    @BeforeEach
    void family() throws Exception {
        a = resident(ResidentStatus.IN_RESIDENCE);
        rMember = member("Trần R", "079000000001");
        r = relate(a, rMember, true, true);
        sMember = fixtures.familyMember("Trần S", "0902000001");
        s = fixtures.relate(sMember, a, 5, true, false);
        nMember = fixtures.familyMember("Trần N", "0902000002", "CCCD", "079000000002");
        n = fixtures.relate(nMember, a, 6, false, false, FamilyRight.PICKUP_ALLOWED);
        manager = staffSession(Role.NURSING_HOME_MANAGER);
    }

    @Test
    void US3_1_2_officeAddsNewPicker_pending_thenRepresentativeConfirmsOnPortal() throws Exception {
        JsonNode created = json(createRequest(
                        office,
                        a,
                        "\"type\": \"RIGHT\", \"right\": \"PICKUP_ALLOWED\", \"newValue\": true, \"newPicker\":"
                                + " {\"fullName\": \"Lê X\", \"idDocumentType\": \"CCCD\", \"idDocumentNumber\":"
                                + " \"079000000099\", \"phoneNumber\": \"0903000099\", \"relationship\": \"OTHER\"}")
                .andExpect(status().isCreated()));
        long request = created.get("id").asLong();
        long x = created.get("relationshipId").asLong();

        assertThat(created.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(created.get("source").asString()).isEqualTo("COUNTER");
        assertThat(column(x, "pickup_allowed")).isFalse();
        assertThat(pickupNames()).doesNotContain("Lê X");
        assertThat(notices("RIGHT_CHANGE_NEEDS_CONFIRMATION")).singleElement().satisfies(notice -> {
            assertThat(notice.level()).contains(NotificationLevel.MEDIUM);
            assertThat(notice.recipientGroups())
                    .contains(
                            new RecipientGroup.FamilyMember(rMember, "REPRESENTATIVE"),
                            new RecipientGroup.FamilyMember(sMember, "REPRESENTATIVE"),
                            new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        });

        requestCommand(familySession(rMember), request, "confirm", "{}").andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("EFFECTIVE");
        assertThat(column(x, "pickup_allowed")).isTrue();
        assertThat(pickupNames()).contains("Lê X");
        assertThat(requestJson(request).get("confirmationMethod").asString()).isEqualTo("PORTAL");
    }

    private JsonNode requestJson(long requestId) throws Exception {
        for (JsonNode node : getJson("/api/v1/right-change-requests?residentId=" + a)) {
            if (node.get("id").asLong() == requestId) {
                return node;
            }
        }
        throw new AssertionError("Không thấy yêu cầu " + requestId);
    }

    private List<String> pickupNames() throws Exception {
        List<String> names = new ArrayList<>();
        getJson("/api/v1/residents/" + a + "/pickup-list")
                .forEach(node -> names.add(node.get("fullName").asString()));
        return names;
    }

    private List<NotificationRequest> notices(String eventType) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(r -> r.eventType().equals(eventType))
                .filter(r -> r.residentId().equals(Optional.of(a)))
                .toList();
    }
}
