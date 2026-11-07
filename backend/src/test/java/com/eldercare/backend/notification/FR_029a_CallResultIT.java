package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.spi.FamilyContactView;
import com.eldercare.backend.notification.api.spi.NotifiableRelationshipView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 009 US1 (T017, tên logic {@code BR_M13_02_KetQuaGoiIT}, {@code FR_029a_ChuoiGoiIT}; quickstart Q3, Q5, Q7): ghi kết quả
 * gọi qua {@code POST /call-reminders/{id}/results} chỉ người có quyền gọi (Người phụ trách ca, Trưởng tầng được giao của
 * tầng, Quản lý viện); Đã liên lạc được thì thông báo gộp Đã xác nhận qua cuộc gọi; Không liên lạc được thì nhắc gọi
 * người thân kế tiếp của chuỗi, hết người thì báo Quản lý viện Khẩn cấp và thông báo chuyển Không liên lạc được; lần gọi
 * bổ sung Đã liên lạc được thì xác nhận muộn qua cuộc gọi. Đọc lượt của tôi và chi tiết có số điện thoại hiện hành.
 */
class FR_029a_CallResultIT extends CallReminderTestBase {

    @Autowired
    NotificationLookupApi lookup;

    private ResultActions record(long reminderId, TestUser by, String body) throws Exception {
        return mvc.perform(post("/api/v1/call-reminders/" + reminderId + "/results")
                .header("Authorization", bearer(by))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String reached() {
        return "{\"result\":\"REACHED\",\"calledAt\":\"" + time.now() + "\",\"note\":\"Đã báo tin\"}";
    }

    private String notReached() {
        return "{\"result\":\"NOT_REACHED\",\"calledAt\":\"" + time.now() + "\",\"reason\":\"NO_ANSWER\"}";
    }

    /** Thông báo Khẩn cấp quản lý viện nhận khi hết người để gọi (FR-029a). */
    private int managerUnreachableNotices(long reminderId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification n JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE q.source_record_type = 'M13.CALL_REMINDER' AND q.source_record_id = ?"
                        + " AND q.event_type = 'CALL_CHAIN_NOT_REACHED' AND n.level = 'URGENT'",
                Integer.class,
                Long.toString(reminderId));
    }

    @Test
    void FR_029a_familyNotReached_nextFamilyMember_thenManagersUrgent_unreachable_thenSupplementaryReached()
            throws Exception {
        TestUser lead = users.staff(Role.NURSING_STAFF);
        duties.shiftLead(floor, lead.staffId());
        long m = TestUsers.nextId();
        long r = TestUsers.nextId();
        TestUser mAccount = users.family(m);
        family.relate(
                        resident,
                        new NotifiableRelationshipView(
                                m,
                                true,
                                false,
                                Set.of(FamilyRight.RECEIVE_URGENT),
                                Optional.of(mAccount.accountId()),
                                true,
                                Optional.of("0904444001")))
                .callOrder(resident, List.of(r, m))
                .contact(
                        new FamilyContactView(r, "[Người thân R]", Optional.of("0904444002"), Optional.empty(), false));
        long notification = send(urgent("chuoi-" + resident, new RecipientGroup.PrimaryContact(resident)))
                .get("FAMILY_MEMBER:" + m);
        advance(PAST_CFG);
        creation.run();
        long first = reminders("FAMILY_MEMBER", m).getFirst();

        record(first, lead, notReached()).andExpect(status().isCreated());

        long second = reminders("FAMILY_MEMBER", r).getFirst();
        assertThat(jdbc.queryForMap("SELECT call_chain_id, creation_reason FROM call_reminder WHERE id = ?", second))
                .containsEntry("call_chain_id", first)
                .containsEntry("creation_reason", "PREVIOUS_NOT_REACHED");
        assertThat(state(second)).containsEntry("current_staff_id", lead.staffId());
        assertThat(notificationStatus(notification)).doesNotStartWith("UNREACHABLE");

        record(second, lead, notReached()).andExpect(status().isCreated());

        assertThat(notificationStatus(notification)).startsWith("UNREACHABLE/");
        await().atMost(Duration.ofSeconds(10)).until(() -> managerUnreachableNotices(second) >= 1);
        assertThat(reminders("FAMILY_MEMBER", m)).hasSize(1);

        record(first, lead, reached()).andExpect(status().isCreated());
        assertThat(notificationStatus(notification)).isEqualTo("ACKNOWLEDGED/LATE_BY_CALL");
        assertThat(jdbc.queryForObject(
                        "SELECT content->>'$.supplementary' = 'true' FROM record_event WHERE kind = 'M13.CALL_RESULT'"
                                + " AND entity_type = 'M13.CALL_REMINDER' AND entity_id = ? ORDER BY id DESC LIMIT 1",
                        Boolean.class,
                        first))
                .isTrue();
    }
}
