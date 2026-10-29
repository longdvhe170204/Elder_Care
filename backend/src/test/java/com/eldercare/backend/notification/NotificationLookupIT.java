package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.api.NotificationLookupApi;
import com.eldercare.backend.notification.api.dto.NotificationStatus;
import com.eldercare.backend.notification.api.dto.NotifiedPersonView;
import com.eldercare.backend.notification.api.dto.RecipientType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 009 US5 (T032, T034): lịch sử gửi theo bản ghi nguồn và người nhận (FR-037, BR-M13-05: Quản lý viện toàn viện, Trưởng
 * tầng tầng được giao); danh sách lỗi gửi (FR-038, Quản lý viện); dữ liệu cho module nguồn (FR-035, BR-M10-06).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class NotificationLookupIT extends M13IntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    NotificationLookupApi lookup;

    TestUsers users;
    long floor;
    long otherFloor;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        floor = TestUsers.nextId();
        otherFloor = TestUsers.nextId();
    }

    private String send(String key, long floorAreaId, NotificationLevel level, RecipientGroup group) {
        NotificationRequest r = new NotificationRequest(
                "M05",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.TEST", key),
                key,
                Optional.of(level),
                Optional.empty(),
                Optional.of(floorAreaId),
                List.of(group),
                List.of(new ContentPart(InformationType.GENERAL, "Nội dung")),
                Optional.of("Rút gọn"),
                Optional.of("Tiêu đề"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        awaitRequests(key, 1);
        return key;
    }

    private JsonNode history(TestUser user, String query) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/notification-history" + query)
                        .header("Authorization", TestUsers.bearer(users.session(user.accountId()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    private void assignFloor(TestUser floorManager, long floorAreaId) {
        assignments.assign(
                floorManager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floorAreaId,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
    }

    @Test
    void FR_037_managerSeesAll_floorManagerOnlyAssignedFloor_othersForbidden() throws Exception {
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        TestUser office = users.staff(Role.OFFICE_STAFF);
        assignFloor(floorManager, floor);
        String own = send(
                "fr037a-" + floor, floor, NotificationLevel.LOW, new RecipientGroup.Staff(office.staffId(), "TEST"));
        String other = send(
                "fr037b-" + floor,
                otherFloor,
                NotificationLevel.LOW,
                new RecipientGroup.Staff(office.staffId(), "TEST"));

        JsonNode all = history(manager, "?sourceRecordType=M05.TEST&sourceRecordId=" + own);
        assertThat(all).hasSize(1);
        JsonNode entry = all.get(0);
        assertThat(entry.get("recipient").asString()).isEqualTo("STAFF:" + office.staffId());
        assertThat(entry.get("basis")).extracting(JsonNode::asString).containsExactly("Staff:TEST");
        assertThat(entry.get("level").asString()).isEqualTo("LOW");
        assertThat(entry.get("channels"))
                .extracting(c -> c.get("channel").asString())
                .contains("IN_APP");
        assertThat(entry.get("sentAt").asString()).isNotBlank();

        assertThat(history(floorManager, "?sourceRecordType=M05.TEST&sourceRecordId=" + own))
                .hasSize(1);
        assertThat(history(floorManager, "?sourceRecordType=M05.TEST&sourceRecordId=" + other))
                .isEmpty();
        mvc.perform(get("/api/v1/notification-history")
                        .header("Authorization", TestUsers.bearer(users.session(office.accountId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_038_deliveryErrors_managerOnly_includesNoRecipient() throws Exception {
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        TestUser office = users.staff(Role.OFFICE_STAFF);
        String key = send(
                "fr038-" + floor,
                floor,
                NotificationLevel.MEDIUM,
                new RecipientGroup.AssignedNurse(TestUsers.nextId(), new Substitution.NoSubstitute()));

        JsonNode errors = json.readTree(mvc.perform(get("/api/v1/delivery-errors?type=NO_RECIPIENT")
                        .header("Authorization", TestUsers.bearer(users.session(manager.accountId()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        assertThat(errors).extracting(e -> e.get("eventKey").asString()).contains(key);
        assertThat(errors).allSatisfy(e -> assertThat(e.get("type").asString()).isEqualTo("NO_RECIPIENT"));
        mvc.perform(get("/api/v1/delivery-errors")
                        .header("Authorization", TestUsers.bearer(users.session(office.accountId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_035_bySource_givesBasisStatusChannelsViewedAt() throws Exception {
        TestUser office = users.staff(Role.OFFICE_STAFF);
        String key = send(
                "fr035-" + floor, floor, NotificationLevel.LOW, new RecipientGroup.Staff(office.staffId(), "TEST"));
        long id = jdbc.queryForObject(
                "SELECT n.id FROM notification n JOIN notification_request r ON r.id = n.request_id"
                        + " WHERE r.event_key = ?",
                Long.class,
                key);
        mvc.perform(get("/api/v1/me/notifications/" + id)
                        .header("Authorization", TestUsers.bearer(users.session(office.accountId()))))
                .andExpect(status().isOk());

        List<NotifiedPersonView> people = lookup.bySource("M05.TEST", key);

        assertThat(people).singleElement().satisfies(p -> {
            assertThat(p.notificationId()).isEqualTo(id);
            assertThat(p.recipientType()).isEqualTo(RecipientType.STAFF);
            assertThat(p.recipientId()).isEqualTo(office.staffId());
            assertThat(p.basis()).containsExactly("Staff:TEST");
            assertThat(p.status()).isEqualTo(NotificationStatus.VIEWED);
            assertThat(p.channels())
                    .extracting(NotifiedPersonView.ChannelResult::channel)
                    .contains("IN_APP");
            assertThat(p.sentAt()).isPresent();
            assertThat(p.viewedAt()).isPresent();
            assertThat(p.missingLevel()).isFalse();
        });
        assertThat(lookup.viewedAt(id)).isEqualTo(people.getFirst().viewedAt());
    }
}
