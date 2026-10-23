package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.api.spi.FamilyContactView;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m13.internal.directcontact.service.DirectContactDetector;
import com.cambae.backend.m13.internal.directcontact.service.DirectContactReminder;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m13.FakeFamilyNotifications;
import com.cambae.backend.support.fake.m15.FakeM15Config;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 009 US3 (T024, tên logic {@code FR_021_LienHeTrucTiepIT}; quickstart Q19, Q26): thông báo Trung bình tới người thân không
 * tới được qua kênh nào (không có tài khoản cổng, tin nhắn không gửi được) vào danh sách "cần liên hệ trực tiếp"; Hành chính
 * ghi Đã liên hệ / Không liên hệ được (lý do bắt buộc), là trạng thái cuối; Quản lý viện chỉ xem (FR-039); mục còn chờ đầu
 * ngày thì nhắc Hành chính, báo Quản lý viện. Mức Nhẹ chỉ ghi Gửi thất bại.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class FR_021_DirectContactIT extends M13IntegrationTestBase {

    private static final String URL = "/api/v1/direct-contacts";

    @Autowired
    BusinessTime time;

    @Autowired
    FakeFamilyNotifications family;

    @Autowired
    DirectContactDetector detector;

    @Autowired
    DirectContactReminder reminder;

    @Autowired
    MockMvc mvc;

    TestUsers users;
    long resident;
    long member;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
        member = TestUsers.nextId();
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        member,
                        true,
                        false,
                        Set.of(FamilyRight.VIEW_COSTS),
                        Optional.empty(),
                        false,
                        Optional.empty()));
        family.contact(new FamilyContactView(member, "Trần Thị M", Optional.empty(), Optional.empty(), false));
    }

    @AfterEach
    void reset() {
        family.reset();
    }

    private long send(String key, NotificationLevel level) {
        NotificationRequest r = new NotificationRequest(
                "M11",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M11.TEST", key),
                key,
                Optional.of(level),
                Optional.of(resident),
                Optional.empty(),
                List.of(new RecipientGroup.PrimaryContact(resident)),
                List.of(new ContentPart(InformationType.COSTS, "Bảng chi phí tháng đã chốt")),
                Optional.of("Bảng chi phí tháng đã chốt"),
                Optional.of("Chi phí"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty());
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        long requestId = ((Number) awaitRequests(key, 1).getFirst().get("id")).longValue();
        return jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, requestId);
    }

    private long contactOf(long notificationId) {
        return jdbc.queryForObject(
                "SELECT id FROM direct_contact WHERE notification_id = ?", Long.class, notificationId);
    }

    private String bearer(TestUser u) {
        return TestUsers.bearer(users.session(u.accountId()));
    }

    private ResultActions record(TestUser u, long id, String body) throws Exception {
        return mvc.perform(post(URL + "/" + id + "/results")
                .header("Authorization", bearer(u))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void FR_021_mediumNoChannel_listedPending_lowNotListed() throws Exception {
        long medium = send("dc-medium-" + resident, NotificationLevel.MEDIUM);
        long low = send("dc-low-" + resident, NotificationLevel.LOW);

        detector.run();
        detector.run();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM direct_contact WHERE notification_id = ?", Integer.class, medium))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM direct_contact WHERE notification_id = ?", Integer.class, low))
                .isZero();
        TestUser office = users.staff(Role.OFFICE_STAFF);
        long id = contactOf(medium);
        mvc.perform(get(URL).param("status", "PENDING").header("Authorization", bearer(office)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].fullName").value("Trần Thị M"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].shortContent").value("Bảng chi phí tháng đã chốt"));
    }

    @Test
    void FR_021_officeRecords_finalState_reasonRequired_managerViewsOnly() throws Exception {
        long n = send("dc-record-" + resident, NotificationLevel.MEDIUM);
        detector.run();
        long id = contactOf(n);
        TestUser office = users.staff(Role.OFFICE_STAFF);
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        TestUser nurse = users.staff(Role.NURSING_STAFF);

        record(office, id, "{\"result\":\"UNREACHABLE\",\"occurredAt\":\"" + time.now() + "\"}")
                .andExpect(status().isUnprocessableContent());
        record(manager, id, "{\"result\":\"CONTACTED\",\"occurredAt\":\"" + time.now() + "\"}")
                .andExpect(status().isForbidden());
        record(office, id, "{\"result\":\"CONTACTED\",\"occurredAt\":\"" + time.now() + "\",\"note\":\"Đã gọi\"}")
                .andExpect(status().isCreated());
        record(office, id, "{\"result\":\"UNREACHABLE\",\"occurredAt\":\"" + time.now() + "\",\"reason\":\"Sai số\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FINAL_STATE"));

        mvc.perform(get(URL).header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].status").value("CONTACTED"));
        mvc.perform(get(URL).header("Authorization", bearer(nurse))).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM direct_contact_result WHERE direct_contact_id = ?", Integer.class, id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'DIRECT_CONTACT_RECORDED' AND entity_id = ?",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(1);
    }

    @Test
    void FR_021_pendingAtStartOfDay_officeRemindedManagerNotified() {
        send("dc-remind-" + resident, NotificationLevel.MEDIUM);
        detector.run();

        reminder.run(time.today());

        String key = "DIRECT_CONTACT_PENDING:" + time.today();
        List<Map<String, Object>> requests = awaitRequests(key, 1);
        assertThat(requests.getFirst()).containsEntry("intake_result", "VALID");
        assertThat(jdbc.queryForList(
                        "SELECT group_code FROM notification_request_recipient_group WHERE request_id = ? ORDER BY seq",
                        String.class,
                        requests.getFirst().get("id")))
                .containsExactly("FacilityRole", "FacilityRole");
    }
}
