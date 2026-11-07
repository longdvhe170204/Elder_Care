package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.family.api.events.FamilyStayNotice;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.JsonNode;

/**
 * 012 T034 ({@code BR_M10_04_DemOLaiIT}; quickstart Q29, Q31 → Q33; US8 kịch bản 1 → 3, 5; FR-040 → FR-045, BR-M10-04):
 * Hành chính đăng ký thì Chờ xác nhận và báo Trưởng tầng; chỉ lượt Đã xác nhận mới bắt đầu; phòng từ hai giường đang dùng
 * thì xác nhận cần ý kiến người cùng phòng; mỗi mốc 00:00 của lượt Đang ở lại là một sự kiện tính phí cho 010, không tạo
 * khoản; kết thúc sớm hơn đêm đã phát thì báo hủy đêm đó; người cao tuổi rời viện thì nhắc kết thúc, lượt không tự kết
 * thúc.
 */
class BR_M10_04_OvernightStayIT extends StayTestBase {

    @Autowired
    ApplicationEventPublisher publisher;

    @Test
    void US8_1_registeredPending_floorManagerNotified_startBeforeConfirmBlocked_thenConfirmed() throws Exception {
        JsonNode stay = json(register(office, rMember, at(day, 18, 0), at(day.plusDays(2), 8, 0))
                .andExpect(status().isCreated()));
        long id = stay.get("id").asLong();

        assertThat(stay.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(stay.get("mealRegistered").asBoolean()).isTrue();
        assertThat(stay.get("nightCount").asInt()).isZero();
        assertThat(notices(id, FamilyStayNotice.Kind.REGISTERED))
                .singleElement()
                .satisfies(n -> {
                    var request = n.notificationRequests().getFirst();
                    assertThat(request.level()).contains(NotificationLevel.MEDIUM);
                    assertThat(request.recipientGroups())
                            .containsExactly(new RecipientGroup.FloorManager(
                                    floor,
                                    new Substitution.Designated(
                                            new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))));
                });

        command(office, id, "start", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));

        command(staff(floorManager), id, "confirm", "{}").andExpect(status().isOk());
        assertThat(stayStatus(id)).isEqualTo("CONFIRMED");
        assertThat(notices(id, FamilyStayNotice.Kind.CONFIRMED)).singleElement().satisfies(n -> {
            var request = n.notificationRequests().getFirst();
            assertThat(request.level()).contains(NotificationLevel.LOW);
            assertThat(request.recipientGroups())
                    .contains(
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                            new RecipientGroup.FamilyMember(rMember, "FAMILY_STAY_GUEST"));
        });
    }

    @Test
    void Q31_eachMidnightWhileStaying_isOneBillingEvent_noDuplicates() throws Exception {
        long id = confirmed(rMember, at(day, 18, 0), at(day.plusDays(2), 8, 0));
        moveTo(at(day, 18, 0));
        command(office, id, "start", "{}").andExpect(status().isOk());
        assertThat(stayStatus(id)).isEqualTo("STAYING");

        runNights(at(day.plusDays(1), 0, 0));
        runNights(at(day.plusDays(1), 0, 0));
        runNights(at(day.plusDays(2), 0, 0));
        moveTo(at(day.plusDays(2), 7, 0));
        command(office, id, "end", "{}").andExpect(status().isOk());

        List<CostSourceBilled> billed = billed(id);
        assertThat(billed).hasSize(2);
        assertThat(billed.getFirst()).satisfies(e -> {
            assertThat(e.sourceType()).isEqualTo("OVERNIGHT_STAY");
            assertThat(e.sourceId()).isEqualTo(id + ":" + day);
            assertThat(e.unit()).isEqualTo("NIGHT");
            assertThat(e.residentId()).isEqualTo(a);
            assertThat(e.serviceId()).isEmpty();
            assertThat(e.quantity()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(e.incurredOn()).isEqualTo(day);
        });
        assertThat(billed.get(1).incurredOn()).isEqualTo(day.plusDays(1));
        assertThat(stay(id).get("status").asString()).isEqualTo("ENDED");
        assertThat(stay(id).get("nightCount").asInt()).isEqualTo(2);
    }

    private List<FamilyStayNotice> notices(long id, FamilyStayNotice.Kind kind) {
        return events.stream(FamilyStayNotice.class)
                .filter(n -> n.stayId() == id && n.kind() == kind)
                .toList();
    }

    private List<CostSourceBilled> billed(long id) {
        return events.stream(CostSourceBilled.class)
                .filter(e -> e.sourceId().startsWith(id + ":"))
                .toList();
    }

    private JsonNode stay(long id) throws Exception {
        for (JsonNode s : getJson("/api/v1/family-stays?residentId=" + a)) {
            if (s.get("id").asLong() == id) {
                return s;
            }
        }
        throw new AssertionError("Không thấy lượt " + id);
    }
}
