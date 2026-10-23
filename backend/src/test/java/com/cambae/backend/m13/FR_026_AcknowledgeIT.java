package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.events.UrgentNotificationAcknowledged;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 009 US1 phần xác nhận (T017 một phần, tên logic {@code FR_026a_XacNhanIT}): người nhận thông báo Khẩn cấp "Xác nhận
 * đã nhận"; mở thông báo không phải xác nhận (FR-026); xác nhận không đổi bản ghi nguồn, chỉ ghi sự kiện, nhật ký và phát
 * {@code UrgentNotificationAcknowledged} (FR-026a, FR-035). Không phải Khẩn cấp hay đã xác nhận thì 409; thông báo của
 * người khác thì 404.
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(FakeM15Config.class)
class FR_026_AcknowledgeIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationEvents recorded;

    TestUsers users;
    TestUser nurse;
    String token;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        nurse = users.staff(Role.NURSING_STAFF);
        token = TestUsers.bearer(users.session(nurse.accountId()));
    }

    private long send(String key, NotificationLevel level) {
        NotificationRequest r = new NotificationRequest(
                "M05",
                "INCIDENT_URGENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.INCIDENT", key),
                key,
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.Staff(nurse.staffId(), "X")),
                List.of(new ContentPart(InformationType.GENERAL, "Ngã trong phòng")),
                Optional.of("Có sự cố khẩn cấp"),
                Optional.of("Sự cố"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        long requestId = ((Number) awaitRequests(key, 1).getFirst().get("id")).longValue();
        return jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, requestId);
    }

    private ResultActions acknowledge(long id, String bearer) throws Exception {
        return mvc.perform(post("/api/v1/notifications/" + id + "/acknowledge")
                .header("Authorization", bearer)
                .header("Idempotency-Key", UUID.randomUUID().toString()));
    }

    @Test
    void FR_026_openIsNotAcknowledge_acknowledgeRecordsInApp() throws Exception {
        long id = send("ack-" + nurse.staffId(), NotificationLevel.URGENT);

        mvc.perform(get("/api/v1/me/notifications/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VIEWED"));
        acknowledge(id, token).andExpect(status().isOk());

        assertThat(jdbc.queryForMap(
                        "SELECT status, acknowledgement_method FROM notification_state WHERE notification_id = ?", id))
                .containsEntry("status", "ACKNOWLEDGED")
                .containsEntry("acknowledgement_method", "IN_APP");
        assertThat(jdbc.queryForObject(
                        "SELECT actor_id FROM notification_event WHERE notification_id = ? AND type = 'ACKNOWLEDGE'",
                        Long.class,
                        id))
                .isEqualTo(nurse.accountId());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'NOTIFICATION_ACKNOWLEDGED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(1);
        assertThat(recorded.stream(UrgentNotificationAcknowledged.class))
                .anyMatch(e -> e.notificationId() == id && e.sourceRecord().id().equals("ack-" + nurse.staffId()));
    }

    @Test
    void FR_026_alreadyAcknowledged_or_notUrgent_409() throws Exception {
        long urgent = send("ack-twice-" + nurse.staffId(), NotificationLevel.URGENT);
        long medium = send("ack-medium-" + nurse.staffId(), NotificationLevel.MEDIUM);

        acknowledge(urgent, token).andExpect(status().isOk());
        acknowledge(urgent, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_ACKNOWLEDGED"));
        acknowledge(medium, token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_URGENT"));
    }

    @Test
    void FR_026_someoneElsesNotification_404() throws Exception {
        long id = send("ack-other-" + nurse.staffId(), NotificationLevel.URGENT);
        TestUser other = users.staff(Role.NURSING_STAFF);

        acknowledge(id, TestUsers.bearer(users.session(other.accountId()))).andExpect(status().isNotFound());
    }
}
