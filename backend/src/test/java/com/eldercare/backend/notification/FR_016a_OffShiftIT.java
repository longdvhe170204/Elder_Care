package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.notification.internal.delivery.service.SmsWindowCloser;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.notification.FakeSmsProvider;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import com.eldercare.backend.support.fake.staffing.FakeM09Config;
import com.eldercare.backend.support.fake.staffing.FakeShifts;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 009 US3 (T024, tên logic {@code FR_016a_NgoaiCaIT}; T026 phần chờ 008): nhân viên không có tên trong ca đang diễn ra
 * chỉ nhận Trung bình trong ứng dụng, tin nhắn ghi Bỏ qua – ngoài ca; Khẩn cấp gửi mọi kênh bất kể ca (FR-016a). Tin tổng
 * hợp gửi nhân viên nêu tầng/khu vực và ca của các thông báo trong đó (FR-016b, 008 FR-059). "Trong ca" của vai trò không
 * xếp ca theo giờ hành chính CFG-M13-06 do {@code ShiftApi} của 008 tính ({@code ShiftQueriesIT}).
 */
@Import({FakeM15Config.class, FakeM09Config.class, FakeLocations.Config.class})
class FR_016a_OffShiftIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeStaffProfiles profiles;

    @Autowired
    FakeShifts shifts;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeSmsProvider sms;

    @Autowired
    SmsWindowCloser closer;

    TestUsers users;
    long floor;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        jdbc.update("UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.enabled', CAST('true' AS JSON))"
                + " WHERE catalog_type = 'M13.NOTIFICATION_CHANNEL' AND code = 'SMS'");
        sms.reset();
        clock.setTo(at("2030-03-04T10:00:00"));
    }

    @AfterEach
    void reset() {
        jdbc.update("UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.enabled', CAST('false' AS JSON))"
                + " WHERE catalog_type = 'M13.NOTIFICATION_CHANNEL' AND code = 'SMS'");
        clock.reset();
        sms.reset();
        shifts.reset();
    }

    private static Instant at(String local) {
        return OffsetDateTime.parse(local + "+07:00").toInstant();
    }

    private TestUser nurse(String phone) {
        TestUser u = users.staff(Role.NURSING_STAFF);
        profiles.phone(u.staffId(), phone);
        return u;
    }

    /** Ca ngày 07:00–15:00 của tầng đang diễn ra lúc 10:00. */
    private void dayShift(TestUser... staff) {
        shifts.shift(
                new ShiftView(
                        TestUsers.nextId(),
                        floor,
                        at("2030-03-04T07:00:00"),
                        at("2030-03-04T15:00:00"),
                        "IN_PROGRESS",
                        null),
                List.of(staff).stream().map(TestUser::staffId).toList());
    }

    private void send(String key, NotificationLevel level, TestUser to) {
        NotificationRequest r = new NotificationRequest(
                "M04",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M04.TEST", key),
                key,
                Optional.of(level),
                Optional.empty(),
                Optional.of(floor),
                List.of(new RecipientGroup.Staff(to.staffId(), "X")),
                List.of(new ContentPart(InformationType.GENERAL, "Nội dung")),
                Optional.of("Rút gọn " + key),
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
    }

    private List<String> smsResults(String key) {
        return jdbc.queryForList(
                "SELECT r.m13_result FROM record_event a JOIN notification n ON n.id = a.entity_id"
                        + " JOIN notification_request q ON q.id = n.request_id"
                        + " JOIN record_event r ON r.kind = 'M13.DELIVERY_RESULT' AND r.entity_type = 'M13.DELIVERY_ATTEMPT' AND r.entity_id = a.id WHERE a.kind = 'M13.DELIVERY_ATTEMPT' AND a.entity_type = 'M13.NOTIFICATION'"
                        + " AND q.event_key = ? AND a.content->>'$.channel' = 'SMS'",
                String.class,
                key);
    }

    @Test
    void FR_016a_offShiftStaff_mediumSmsSkipped_urgentStillSent_onShiftSent() {
        TestUser off = nurse("0903333001");
        TestUser on = nurse("0903333002");
        dayShift(on);
        String p = "ngoai-ca-" + off.staffId() + "-";

        send(p + "medium-off", NotificationLevel.MEDIUM, off);
        send(p + "medium-on", NotificationLevel.MEDIUM, on);
        send(p + "urgent-off", NotificationLevel.URGENT, off);

        assertThat(smsResults(p + "medium-off")).containsExactly("SKIPPED_OFF_SHIFT");
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 2);
        assertThat(sms.sent().stream().map(FakeSmsProvider.SentSms::phoneNumber))
                .containsExactlyInAnyOrder("0903333002", "0903333001");
        assertThat(smsResults(p + "medium-on")).doesNotContain("SKIPPED_OFF_SHIFT");
        assertThat(smsResults(p + "urgent-off")).doesNotContain("SKIPPED_OFF_SHIFT");
    }

    @Test
    void FR_016b_staffDigest_namesFloorAndShiftOfBatchedNotifications() {
        TestUser on = nurse("0903333003");
        dayShift(on);
        String p = "tong-hop-" + on.staffId() + "-";
        send(p + "1", NotificationLevel.MEDIUM, on);
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 1);
        clock.setTo(at("2030-03-04T10:01:00"));
        send(p + "2", NotificationLevel.MEDIUM, on);
        send(p + "3", NotificationLevel.MEDIUM, on);

        clock.setTo(at("2030-03-04T10:05:02"));
        closer.run();
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 2);

        String digest = sms.sent().get(1).content();
        assertThat(digest)
                .contains("2 thông báo mới")
                .contains("[Tầng kiểm thử]")
                .contains("07:00–15:00")
                .doesNotContain("Rút gọn");
    }
}
