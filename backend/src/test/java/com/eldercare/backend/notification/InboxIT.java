package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.events.NotificationViewed;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.notification.FakeFamilyNotifications;
import com.eldercare.backend.support.fake.resident.FakeConsentApi;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 009 US5 (T032, T033, T034): hộp thông báo của tôi (FR-033), mở thông báo ghi xem lần đầu một lần và hiển thị theo quyền
 * hiện hành (FR-034, Q-99), ẩn (FR-036), đăng ký thiết bị đẩy, luồng SSE.
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({FakeM15Config.class, FakeConsentApi.Config.class})
class InboxIT extends M13IntegrationTestBase {

    private static final String INBOX = "/api/v1/me/notifications";

    @Autowired
    MockMvc mvc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeFamilyNotifications family;

    @Autowired
    FakeConsentApi consents;

    @Autowired
    ApplicationEvents events;

    TestUsers users;
    long resident;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
    }

    @AfterEach
    void resetFamily() {
        family.reset();
        consents.reset();
    }

    private NotificationRequest notice(
            String key,
            NotificationLevel level,
            RecipientGroup group,
            Optional<InformationSubject> subject,
            InformationType... parts) {
        return new NotificationRequest(
                "M05",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.TEST", key),
                key,
                Optional.of(level),
                Optional.of(resident),
                Optional.empty(),
                List.of(group),
                List.of(parts).stream()
                        .map(t -> new ContentPart(t, "Phần " + t))
                        .toList(),
                Optional.of("Rút gọn"),
                Optional.of("Tiêu đề " + key),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                subject,
                Optional.empty());
    }

    private String send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        awaitRequests(r.eventKey(), 1);
        return r.eventKey();
    }

    private long notificationId(String key, long accountId) {
        return jdbc.queryForObject(
                "SELECT n.id FROM notification n JOIN notification_request r ON r.id = n.request_id"
                        + " WHERE r.event_key = ? AND n.account_id = ?",
                Long.class,
                key,
                accountId);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String body = mvc.perform(get(url).header("Authorization", TestUsers.bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body);
    }

    private RecipientGroup staff(TestUser u) {
        return new RecipientGroup.Staff(u.staffId(), "TEST");
    }

    @Test
    void FR_034_open_recordsFirstViewOnce_andPublishesViewedOnce() throws Exception {
        TestUser office = users.staff(Role.OFFICE_STAFF);
        String token = users.session(office.accountId());
        String key = send(notice(
                "fr034a-" + resident, NotificationLevel.LOW, staff(office), Optional.empty(), InformationType.GENERAL));
        long id = notificationId(key, office.accountId());

        JsonNode first = getJson(INBOX + "/" + id, token);
        JsonNode second = getJson(INBOX + "/" + id, token);

        assertThat(first.get("viewedAt").asString()).isNotBlank();
        assertThat(second.get("viewedAt").asString())
                .isEqualTo(first.get("viewedAt").asString());
        assertThat(first.get("status").asString()).isEqualTo("VIEWED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event e WHERE e.kind = 'M13.NOTIFICATION_EVENT' AND e.entity_type = 'M13.NOTIFICATION' AND e.entity_id = ? AND e.content->>'$.type' = 'VIEW'",
                        Integer.class,
                        id))
                .isEqualTo(1);
        assertThat(events.stream(NotificationViewed.class).filter(e -> e.notificationId() == id))
                .hasSize(1);
        assertThat(getJson(INBOX, token).get("unreadCount").asInt()).isZero();
    }
}
