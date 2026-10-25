package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

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
import com.cambae.backend.m15.parameter.api.ParameterChanged;
import com.cambae.backend.m15.parameter.api.ParameterCode;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * 009 US4 #9 (T029, tên logic {@code FR_024_DoiThamSoIT}): đổi CFG-M13-02 khi có thông báo đang giữ thì thời điểm gửi
 * dự kiến tính lại theo giá trị mới; giá trị mới làm bây giờ không còn trong giờ yên tĩnh thì gửi ngay (FR-024).
 */
@Import(FakeM15Config.class)
class FR_024_ParameterChangeIT extends M13IntegrationTestBase {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeFamilyNotifications family;

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

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    /** Đồng hồ giả lập vẫn chạy sau {@code setTo}, nên so thời điểm ghi với mốc trong vài giây. */
    private static void assertNear(Object actual, Instant expected) {
        assertThat((LocalDateTime) actual).isCloseTo(utc(expected), within(2, ChronoUnit.SECONDS));
    }

    /**
     * Như lệnh đổi CFG-M13-02 của 000 sau commit ({@code ParameterChanged} mang giá trị mới). Không gọi API đổi tham số: lệnh đó
     * cần tài khoản Quản lý viện tạo theo đồng hồ giả lập năm 2030, làm hỏng test khóa Quản lý viện chạy sau.
     */
    private void change(String from, String to) {
        tx.executeWithoutResult(s -> events.publishEvent(new ParameterChanged(
                ParameterCode.CFG_M13_02,
                null,
                JSON.readTree("{\"from\":\"" + from + "\",\"to\":\"" + to + "\"}"),
                time.now())));
    }

    private Map<String, Object> state(long notificationId) {
        return jdbc.queryForMap(
                "SELECT status, scheduled_send_at, sent_at FROM notification_state WHERE notification_id = ?",
                notificationId);
    }

    @Test
    void FR_024_heldNotification_rescheduled_thenSentWhenNoLongerQuiet() {
        long n = TestUsers.nextId();
        TestUser account = users.family(n);
        family.relate(
                resident,
                new NotifiableRelationshipView(
                        n,
                        false,
                        false,
                        Set.of(FamilyRight.VIEW_COSTS),
                        Optional.of(account.accountId()),
                        true,
                        Optional.empty()));
        clock.setTo(at("2030-03-04T22:30:00"));
        String key = "fr024-" + resident;
        sourceCommand(
                "009",
                new SampleSourceEvent(List.of(new NotificationRequest(
                        "M11",
                        "TEST_EVENT",
                        SourceGroup.OTHER,
                        new SourceRecord("M11.TEST", key),
                        key,
                        Optional.of(NotificationLevel.LOW),
                        Optional.of(resident),
                        Optional.empty(),
                        List.of(new RecipientGroup.FamilyWithRight(resident, FamilyRight.VIEW_COSTS)),
                        List.of(new ContentPart(InformationType.COSTS, "Phiếu báo phí tháng")),
                        Optional.empty(),
                        Optional.of("Chi phí"),
                        Optional.empty(),
                        false,
                        time.now(),
                        Map.of(),
                        false,
                        Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                        Optional.empty()))));
        long requestId = ((Number) awaitRequests(key, 1).getFirst().get("id")).longValue();
        long id = jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, requestId);
        assertThat(state(id).get("scheduled_send_at")).isEqualTo(utc(at("2030-03-05T07:00:00")));

        clock.setTo(at("2030-03-04T23:00:00"));
        change("21:00", "06:00");
        await().atMost(Duration.ofSeconds(10))
                .until(() -> utc(at("2030-03-05T06:00:00")).equals(state(id).get("scheduled_send_at")));
        assertThat(state(id).get("status")).isEqualTo("HELD_FOR_QUIET_HOURS");

        clock.setTo(at("2030-03-05T05:30:00"));
        change("21:00", "05:00");
        await().atMost(Duration.ofSeconds(10))
                .until(() -> "SENT".equals(state(id).get("status")));
        assertNear(state(id).get("sent_at"), at("2030-03-05T05:30:00"));
    }
}
