package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m13.api.spi.NotifiableRelationshipView;
import com.cambae.backend.m13.api.spi.SmsResult;
import com.cambae.backend.m13.internal.delivery.service.OutOfAppResender;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m13.FakeFamilyNotifications;
import com.cambae.backend.support.fake.m13.FakeSmsProvider;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakeStaffProfiles;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 009 US3 (T024, tên logic {@code BR_M13_01_ChonKenhIT}, {@code FR_018_GuiLaiIT}, {@code FR_020_NoiDungKenhNgoaiIT};
 * quickstart Q16 → Q19): kênh theo mức (BR-M13-01, FR-016); tin nhắn dùng nội dung rút gọn, kênh tắt không gửi; không có số
 * ghi Không có địa chỉ (FR-018); lỗi tạm gửi lại tối đa CFG-M13-05 lần (FR-018); nội dung rút gọn có trường sức khỏe thì
 * phần kênh ngoài Nội dung không hợp lệ, phần trong ứng dụng vẫn gửi (FR-020). Nhân viên ngoài ca (FR-016a) chờ
 * {@code CaTrucApi} của 008: hiện coi mọi nhân viên đang trong ca.
 */
@Import(FakeM15Config.class)
class BR_M13_01_ChannelSelectionIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeStaffProfiles profiles;

    @Autowired
    FakeFamilyNotifications family;

    @Autowired
    FakeSmsProvider sms;

    @Autowired
    OutOfAppResender resender;

    TestUsers users;
    long resident;

    @BeforeEach
    void setUp() {
        // 10:00 hôm qua: chạy trong giờ yên tĩnh (CFG-M13-02) thì thông báo cho người thân bị giữ lại; mốc ở quá khứ để
        // phân công vai trò tạo trong test không bắt đầu sau giờ thật mà test khác dùng để kết thúc.
        clock.setTo(time.today()
                .minusDays(1)
                .atTime(10, 0)
                .atZone(BusinessTime.ZONE)
                .toInstant());
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
        jdbc.update("UPDATE notification_channel SET enabled = TRUE WHERE code = 'SMS'");
        sms.reset();
    }

    @AfterEach
    void reset() {
        clock.reset();
        jdbc.update("UPDATE notification_channel SET enabled = FALSE WHERE code = 'SMS'");
        family.reset();
        sms.reset();
    }

    private NotificationRequest request(
            String key, NotificationLevel level, Map<String, Object> displayParams, RecipientGroup group) {
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
                List.of(new ContentPart(InformationType.GENERAL, "Nội dung đầy đủ")),
                Optional.of("Có việc mới cần xem trong ứng dụng"),
                Optional.of("Việc mới"),
                Optional.empty(),
                false,
                time.now(),
                displayParams,
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty());
    }

    private long send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        long requestId = ((Number) awaitRequests(r.eventKey(), 1).getFirst().get("id")).longValue();
        return jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, requestId);
    }

    /** Kênh → kết quả lần gửi mới nhất của kênh đó (rỗng: chưa có kết quả). */
    private Map<String, String> outcomes(long notificationId) {
        Map<String, String> out = new java.util.TreeMap<>();
        jdbc.queryForList(
                        "SELECT a.channel, r.result FROM delivery_attempt a"
                                + " LEFT JOIN delivery_result r ON r.attempt_id = a.id WHERE a.notification_id = ?"
                                + " ORDER BY a.attempt_no",
                        notificationId)
                .forEach(row -> out.put(
                        (String) row.get("channel"), row.get("result") == null ? "" : (String) row.get("result")));
        return out;
    }

    private void awaitOutcomes(long notificationId, Map<String, String> expected) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> outcomes(notificationId).equals(expected));
    }

    private TestUser staffWithPhone(String phone) {
        TestUser u = users.staff(Role.ACCOUNTANT);
        profiles.phone(u.staffId(), phone);
        return u;
    }

    @Test
    void BR_M13_01_staff_lowInApp_mediumInAppAndSmsWithShortContent() {
        TestUser u = staffWithPhone("0901111001");

        long low = send(request(
                "ch-low-" + resident, NotificationLevel.LOW, Map.of(), new RecipientGroup.Staff(u.staffId(), "X")));
        long medium = send(request(
                "ch-medium-" + resident,
                NotificationLevel.MEDIUM,
                Map.of(),
                new RecipientGroup.Staff(u.staffId(), "X")));

        awaitOutcomes(low, Map.of("IN_APP", "SENT"));
        awaitOutcomes(medium, Map.of("IN_APP", "SENT", "SMS", "SENT"));
        assertThat(sms.sent())
                .containsExactly(new FakeSmsProvider.SentSms("0901111001", "Có việc mới cần xem trong ứng dụng"));
    }

    @Test
    void BR_M13_01_family_mediumPortalAndSms_FR_016_smsOff_notUsed() {
        long m = TestUsers.nextId();
        TestUser account = users.family(m);
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        m,
                        true,
                        false,
                        Set.of(FamilyRight.VIEW_COSTS),
                        Optional.of(account.accountId()),
                        true,
                        Optional.of("0901111002")));

        long on = send(request(
                "ch-family-" + resident,
                NotificationLevel.MEDIUM,
                Map.of(),
                new RecipientGroup.PrimaryContact(resident)));
        awaitOutcomes(on, Map.of("FAMILY_PORTAL", "SENT", "SMS", "SENT"));

        jdbc.update("UPDATE notification_channel SET enabled = FALSE WHERE code = 'SMS'");
        long off = send(request(
                "ch-family-off-" + resident,
                NotificationLevel.MEDIUM,
                Map.of(),
                new RecipientGroup.PrimaryContact(resident)));
        awaitOutcomes(off, Map.of("FAMILY_PORTAL", "SENT"));
    }

    @Test
    void FR_018_noPhone_noAddress_temporaryFailure_resentOnce() {
        TestUser noPhone = users.staff(Role.ACCOUNTANT);
        long a = send(request(
                "ch-nophone-" + resident,
                NotificationLevel.MEDIUM,
                Map.of(),
                new RecipientGroup.Staff(noPhone.staffId(), "X")));
        awaitOutcomes(a, Map.of("IN_APP", "SENT", "SMS", "NO_ADDRESS"));

        TestUser u = staffWithPhone("0901111003");
        sms.thenReturn(
                SmsResult.of(SmsResult.Outcome.TEMPORARY_FAILURE), SmsResult.of(SmsResult.Outcome.TEMPORARY_FAILURE));
        long b = send(request(
                "ch-resend-" + resident,
                NotificationLevel.MEDIUM,
                Map.of(),
                new RecipientGroup.Staff(u.staffId(), "X")));
        awaitOutcomes(b, Map.of("IN_APP", "SENT", "SMS", "FAILED"));

        resender.run();
        await().atMost(Duration.ofSeconds(10))
                .until(() -> jdbc.queryForObject(
                                "SELECT COUNT(*) FROM delivery_attempt a JOIN delivery_result r ON r.attempt_id = a.id"
                                        + " WHERE a.notification_id = ? AND a.channel = 'SMS'",
                                Integer.class,
                                b)
                        == 2);
        // CFG-M13-05 mặc định 1 lần gửi lại: lần thứ ba không có
        resender.run();
        assertThat(jdbc.queryForObject(
                        "SELECT MAX(attempt_no) FROM delivery_attempt WHERE notification_id = ? AND channel = 'SMS'",
                        Integer.class,
                        b))
                .isEqualTo(2);
        assertThat(sms.sent()).hasSize(2);
    }

    @Test
    void FR_020_healthKeyInShortContent_smsInvalid_inAppStillSent() {
        TestUser u = staffWithPhone("0901111004");

        long id = send(request(
                "ch-health-" + resident,
                NotificationLevel.MEDIUM,
                Map.of("diagnosis", "Viêm phổi"),
                new RecipientGroup.Staff(u.staffId(), "X")));

        awaitOutcomes(id, Map.of("IN_APP", "SENT", "SMS", "INVALID_CONTENT"));
        assertThat(sms.sent()).isEmpty();
    }
}
