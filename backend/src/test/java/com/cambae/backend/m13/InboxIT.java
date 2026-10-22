package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.FamilyRight;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.common.notification.Substitution;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.events.NotificationViewed;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m13.FakeFamilyNotifications;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 009 US5 (T032, T033, T034): hộp thông báo của tôi (FR-033), mở thông báo ghi xem lần đầu một lần và hiển thị theo quyền
 * hiện hành (FR-034, Q-99), ẩn (FR-036), đăng ký thiết bị đẩy, luồng SSE.
 */
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import(FakeM15Config.class)
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
    void FR_033_ownInbox_urgentUnacknowledgedFirst_counts() throws Exception {
        TestUser office = users.staff(Role.OFFICE_STAFF);
        TestUser other = users.staff(Role.OFFICE_STAFF);
        String token = users.session(office.accountId());
        send(notice(
                "fr033-low-" + resident,
                NotificationLevel.LOW,
                staff(office),
                Optional.empty(),
                InformationType.GENERAL));
        send(notice(
                "fr033-urgent-" + resident,
                NotificationLevel.URGENT,
                staff(office),
                Optional.empty(),
                InformationType.GENERAL));
        send(notice(
                "fr033-medium-" + resident,
                NotificationLevel.MEDIUM,
                staff(office),
                Optional.empty(),
                InformationType.GENERAL));

        JsonNode inbox = getJson(INBOX, token);

        assertThat(inbox.get("items")).hasSize(3);
        assertThat(inbox.get("items").get(0).get("level").asString()).isEqualTo("URGENT");
        assertThat(inbox.get("unreadCount").asInt()).isEqualTo(3);
        assertThat(inbox.get("unacknowledgedUrgentCount").asInt()).isEqualTo(1);
        assertThat(getJson(INBOX + "?level=MEDIUM", token).get("items")).hasSize(1);
        assertThat(getJson(INBOX, users.session(other.accountId())).get("items"))
                .isEmpty();
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
                        "SELECT COUNT(*) FROM notification_event WHERE notification_id = ? AND type = 'VIEW'",
                        Integer.class,
                        id))
                .isEqualTo(1);
        assertThat(events.stream(NotificationViewed.class).filter(e -> e.notificationId() == id))
                .hasSize(1);
        assertThat(getJson(INBOX, token).get("unreadCount").asInt()).isZero();
    }

    @Test
    void FR_034_otherPersonsNotification_is404() throws Exception {
        TestUser owner = users.staff(Role.OFFICE_STAFF);
        TestUser other = users.staff(Role.OFFICE_STAFF);
        String key = send(notice(
                "fr034b-" + resident, NotificationLevel.LOW, staff(owner), Optional.empty(), InformationType.GENERAL));
        long id = notificationId(key, owner.accountId());

        mvc.perform(get(INBOX + "/" + id).header("Authorization", TestUsers.bearer(users.session(other.accountId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_034_staff_contentByCurrentScope_notAtSendTime() throws Exception {
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        null,
                        time.now().minus(Duration.ofHours(1)),
                        null));
        String token = users.session(nurse.accountId());
        String key = send(notice(
                "fr034c-" + resident,
                NotificationLevel.MEDIUM,
                new RecipientGroup.AssignedNurse(resident, new Substitution.Default()),
                Optional.empty(),
                InformationType.GENERAL,
                InformationType.HEALTH));
        long id = notificationId(key, nurse.accountId());

        JsonNode inScope = getJson(INBOX + "/" + id, token);
        assertThat(inScope.get("content"))
                .extracting(p -> p.get("informationType").asString())
                .containsExactly("GENERAL", "HEALTH");
        assertThat(inScope.get("hiddenParts")).isEmpty();

        assignments.unassign(nurse.staffId());
        JsonNode outOfScope = getJson(INBOX + "/" + id, token);

        assertThat(outOfScope.get("content")).isEmpty();
        assertThat(outOfScope.get("hiddenParts")).extracting(JsonNode::asString).containsExactly("GENERAL", "HEALTH");
        assertThat(outOfScope.get("title").asString()).isEqualTo("Tiêu đề " + key);
        assertThat(outOfScope.get("eventType").asString()).isEqualTo("TEST_EVENT");
    }

    @Test
    void FR_034_family_rightRemoved_partHidden() throws Exception {
        long familyMemberId = TestUsers.nextId();
        TestUser member = users.family(familyMemberId);
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        familyMemberId,
                        false,
                        false,
                        Set.of(FamilyRight.VIEW_COSTS),
                        Optional.of(member.accountId()),
                        true,
                        Optional.empty()));
        String token = users.session(member.accountId());
        String key = send(notice(
                "fr034d-" + resident,
                NotificationLevel.LOW,
                new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_COSTS),
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                InformationType.GENERAL,
                InformationType.COSTS));
        long id = notificationId(key, member.accountId());

        assertThat(getJson(INBOX + "/" + id, token).get("content"))
                .extracting(p -> p.get("informationType").asString())
                .containsExactly("GENERAL", "COSTS");

        family.reset();
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        familyMemberId,
                        false,
                        false,
                        Set.of(),
                        Optional.of(member.accountId()),
                        true,
                        Optional.empty()));
        JsonNode after = getJson(INBOX + "/" + id, token);

        assertThat(after.get("content"))
                .extracting(p -> p.get("informationType").asString())
                .containsExactly("GENERAL");
        assertThat(after.get("hiddenParts")).extracting(JsonNode::asString).containsExactly("COSTS");
    }

    @Test
    void FR_036_hide_onlyViewed_thenLeavesInbox_recordKept() throws Exception {
        TestUser office = users.staff(Role.OFFICE_STAFF);
        String token = users.session(office.accountId());
        String key = send(notice(
                "fr036-" + resident, NotificationLevel.LOW, staff(office), Optional.empty(), InformationType.GENERAL));
        long id = notificationId(key, office.accountId());

        mvc.perform(post(INBOX + "/" + id + "/hide")
                        .header("Authorization", TestUsers.bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_VIEWED"));
        getJson(INBOX + "/" + id, token);
        mvc.perform(post(INBOX + "/" + id + "/hide")
                        .header("Authorization", TestUsers.bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString()))
                .andExpect(status().isOk());

        assertThat(getJson(INBOX, token).get("items")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void pushDevice_registerThenUpdateToken() throws Exception {
        TestUser office = users.staff(Role.OFFICE_STAFF);
        String token = users.session(office.accountId());
        for (String fcm : List.of("token-1", "token-2")) {
            mvc.perform(post("/api/v1/push-devices")
                            .header("Authorization", TestUsers.bearer(token))
                            .header("Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"app\":\"STAFF_APP\",\"fcmToken\":\"" + fcm + "\"}"))
                    .andExpect(status().isOk());
        }

        assertThat(jdbc.queryForList(
                        "SELECT fcm_token FROM push_device WHERE account_id = ?", String.class, office.accountId()))
                .containsExactly("token-2");
    }

    @Test
    void stream_pushesNewNotificationToOwner() throws Exception {
        TestUser office = users.staff(Role.OFFICE_STAFF);
        String token = users.session(office.accountId());
        MvcResult stream = mvc.perform(
                        get("/api/v1/notifications/stream").header("Authorization", TestUsers.bearer(token)))
                .andExpect(request().asyncStarted())
                .andReturn();

        String key = send(notice(
                "sse-" + resident, NotificationLevel.MEDIUM, staff(office), Optional.empty(), InformationType.GENERAL));
        long id = notificationId(key, office.accountId());

        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> stream.getResponse().getContentAsString().contains("\"notificationId\":" + id));
        assertThat(stream.getResponse().getContentType()).startsWith("text/event-stream");
    }
}
