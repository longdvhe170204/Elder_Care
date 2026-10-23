package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
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
import com.cambae.backend.m13.internal.quiethours.service.QuietHoursRelease;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m13.FakeFamilyNotifications;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
 * 009 US4 (T029, tên logic {@code BR_M13_03_GioYenTinhIT}): thông báo Nhẹ, Trung bình cho người thân trong giờ yên tĩnh
 * CFG-M13-02 được giữ, không có lần gửi nào (FR-022); Khẩn cấp và nhân viên không giữ (FR-022, FR-023); hết giờ yên tĩnh
 * thì xét lại điều kiện rồi gửi theo thứ tự tiếp nhận, hoặc Không gửi kèm lý do (FR-025). Bản đồng ý chưa có (001) nên
 * yêu cầu dùng đối tượng thông tin "việc của người thân" (thông báo về người cao tuổi chưa tới người thân).
 */
@Import(FakeM15Config.class)
class BR_M13_03_QuietHoursIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeFamilyNotifications family;

    @Autowired
    QuietHoursRelease release;

    TestUsers users;
    long resident;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
    }

    @AfterEach
    void reset() {
        clock.reset();
        family.reset();
    }

    private static Instant at(String local) {
        return OffsetDateTime.parse(local + "+07:00").toInstant();
    }

    /** Người thân có tài khoản cổng Hoạt động. */
    private long familyWithPortal(boolean primary, Set<FamilyRight> rights) {
        long id = TestUsers.nextId();
        TestUser account = users.family(id);
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        id, primary, false, rights, Optional.of(account.accountId()), true, Optional.empty()));
        return id;
    }

    private NotificationRequest request(String key, NotificationLevel level, RecipientGroup... groups) {
        return new NotificationRequest(
                "M11",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M11.TEST", key),
                key,
                Optional.of(level),
                Optional.of(resident),
                Optional.empty(),
                List.of(groups),
                List.of(new ContentPart(InformationType.COSTS, "Bảng chi phí tháng đã chốt")),
                Optional.of("Bảng chi phí đã chốt"),
                Optional.of("Chi phí"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty());
    }

    private long send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        return ((Number) awaitRequests(r.eventKey(), 1).getFirst().get("id")).longValue();
    }

    private Map<String, Object> state(long requestId, long familyMemberId) {
        return jdbc.queryForMap(
                "SELECT n.id, s.status, s.scheduled_send_at, s.sent_at FROM notification n"
                        + " JOIN notification_state s ON s.notification_id = n.id"
                        + " WHERE n.request_id = ? AND n.recipient_type = 'FAMILY_MEMBER' AND n.recipient_id = ?",
                requestId,
                familyMemberId);
    }

    private int attempts(long notificationId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM delivery_attempt WHERE notification_id = ?", Integer.class, notificationId);
    }

    private String notSentReason(long notificationId) {
        return jdbc.queryForObject(
                "SELECT reason FROM notification_event WHERE notification_id = ? AND type = 'NOT_SENT'",
                String.class,
                notificationId);
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    /** Đồng hồ giả lập vẫn chạy sau {@code setTo}, nên so thời điểm ghi với mốc trong vài giây. */
    private static void assertNear(Object actual, Instant expected) {
        assertThat((LocalDateTime) actual).isCloseTo(utc(expected), within(2, ChronoUnit.SECONDS));
    }

    @Test
    void BR_M13_03_lowAndMediumForFamilyHeld_untilEndOfQuietHours_noAttempt() {
        long m = familyWithPortal(true, Set.of(FamilyRight.VIEW_COSTS));
        clock.setTo(at("2030-03-04T22:30:00"));

        long medium = send(request(
                "qh-medium-" + resident, NotificationLevel.MEDIUM, new RecipientGroup.PrimaryContact(resident)));
        long low =
                send(request("qh-low-" + resident, NotificationLevel.LOW, new RecipientGroup.PrimaryContact(resident)));

        for (long id : List.of(medium, low)) {
            Map<String, Object> s = state(id, m);
            assertThat(s.get("status")).isEqualTo("HELD_FOR_QUIET_HOURS");
            assertThat(s.get("scheduled_send_at")).isEqualTo(utc(at("2030-03-05T07:00:00")));
            assertThat(s.get("sent_at")).isNull();
            assertThat(attempts(((Number) s.get("id")).longValue())).isZero();
        }
    }

    @Test
    void FR_023_urgentNotHeld_FR_022_staffNotHeld() {
        long m = familyWithPortal(true, Set.of(FamilyRight.VIEW_COSTS));
        TestUser accountant = users.staff(Role.ACCOUNTANT);
        clock.setTo(at("2030-03-04T23:10:00"));

        long urgent = send(request(
                "qh-urgent-" + resident, NotificationLevel.URGENT, new RecipientGroup.PrimaryContact(resident)));
        long staff = send(request(
                "qh-staff-" + resident, NotificationLevel.LOW, new RecipientGroup.FacilityRole(Role.ACCOUNTANT)));

        Map<String, Object> s = state(urgent, m);
        assertThat(s.get("status")).isNotEqualTo("HELD_FOR_QUIET_HOURS");
        assertNear(s.get("sent_at"), at("2030-03-04T23:10:00"));
        assertNear(
                jdbc.queryForObject(
                        "SELECT s.sent_at FROM notification n JOIN notification_state s ON s.notification_id = n.id"
                                + " WHERE n.request_id = ? AND n.recipient_id = ?",
                        LocalDateTime.class,
                        staff,
                        accountant.staffId()),
                at("2030-03-04T23:10:00"));
    }

    @Test
    void FR_025_atEndOfQuietHours_sentInIntakeOrder_withPortalAttempt() {
        long m = familyWithPortal(true, Set.of(FamilyRight.VIEW_COSTS));
        clock.setTo(at("2030-03-04T22:30:00"));
        long first = send(
                request("qh-first-" + resident, NotificationLevel.MEDIUM, new RecipientGroup.PrimaryContact(resident)));
        clock.setTo(at("2030-03-04T23:00:00"));
        long second = send(
                request("qh-second-" + resident, NotificationLevel.LOW, new RecipientGroup.PrimaryContact(resident)));

        clock.setTo(at("2030-03-05T06:59:59"));
        release.run();
        assertThat(state(first, m).get("status")).isEqualTo("HELD_FOR_QUIET_HOURS");

        clock.setTo(at("2030-03-05T07:00:00"));
        release.run();

        long firstId = ((Number) state(first, m).get("id")).longValue();
        long secondId = ((Number) state(second, m).get("id")).longValue();
        await().atMost(Duration.ofSeconds(10))
                .until(() -> "SENT".equals(state(first, m).get("status"))
                        && "SENT".equals(state(second, m).get("status")));
        assertNear(state(first, m).get("sent_at"), at("2030-03-05T07:00:00"));
        assertNear(
                jdbc.queryForObject(
                        "SELECT received_at FROM notification_request WHERE id = ?", LocalDateTime.class, first),
                at("2030-03-04T22:30:00"));
        assertThat(jdbc.queryForList(
                        "SELECT notification_id FROM notification_event WHERE type = 'SEND' AND notification_id IN (?, ?)"
                                + " ORDER BY id",
                        Long.class,
                        firstId,
                        secondId))
                .containsExactly(firstId, secondId);
        assertThat(jdbc.queryForObject(
                        "SELECT channel FROM delivery_attempt WHERE notification_id = ?", String.class, firstId))
                .isEqualTo("FAMILY_PORTAL");

        assertThat(release.run()).isZero();
    }

    @Test
    void FR_025_rightRemovedOrRelationshipEnded_notSentWithReason() {
        long r = familyWithPortal(false, Set.of(FamilyRight.VIEW_COSTS));
        long n = familyWithPortal(false, Set.of(FamilyRight.VIEW_COSTS));
        clock.setTo(at("2030-03-04T22:30:00"));
        long id = send(request(
                "qh-recheck-" + resident,
                NotificationLevel.LOW,
                new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_COSTS)));
        long rNotification = ((Number) state(id, r).get("id")).longValue();
        long nNotification = ((Number) state(id, n).get("id")).longValue();

        // Trước 07:00: R bị bỏ quyền xem chi phí, quan hệ của N chấm dứt
        family.reset();
        family.relate(
                resident,
                new NotifiableRelationshipView(r, false, false, Set.of(), Optional.empty(), false, Optional.empty()));
        clock.setTo(at("2030-03-05T07:00:00"));
        release.run();

        assertThat(state(id, r).get("status")).isEqualTo("NOT_SENT");
        assertThat(state(id, n).get("status")).isEqualTo("NOT_SENT");
        assertThat(notSentReason(rNotification)).isEqualTo("RECIPIENT_NO_LONGER_ELIGIBLE");
        assertThat(notSentReason(nNotification)).isEqualTo("RECIPIENT_NO_LONGER_ELIGIBLE");
        assertThat(attempts(rNotification) + attempts(nNotification)).isZero();
    }
}
