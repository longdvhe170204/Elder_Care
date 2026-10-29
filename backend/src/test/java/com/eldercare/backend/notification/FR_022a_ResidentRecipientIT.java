package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.notification.internal.quiethours.service.QuietHoursRelease;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 009 US4 (T040, tên logic {@code FR_022a_NguoiCaoTuoiNhanIT}; quickstart R1 → R3): nhóm người nhận (f) chính người cao
 * tuổi chỉ nhận mức Nhẹ, không có phần sức khỏe, chi phí (FR-022a); giờ yên tĩnh áp như người thân (FR-022); không có tài
 * khoản thì không có người nhận và không vào danh sách lỗi gửi (FR-007).
 */
@Import(FakeM15Config.class)
class FR_022a_ResidentRecipientIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

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
    void resetClock() {
        clock.reset();
    }

    private static Instant at(String local) {
        return OffsetDateTime.parse(local + "+07:00").toInstant();
    }

    private NotificationRequest request(String key, NotificationLevel level, InformationType... parts) {
        return new NotificationRequest(
                "M10",
                "VISIT_REMINDER",
                SourceGroup.OTHER,
                new SourceRecord("M10.VISIT", key),
                key,
                Optional.of(level),
                Optional.of(resident),
                Optional.empty(),
                List.of(new RecipientGroup.Resident(resident)),
                List.of(parts).stream()
                        .map(t -> new ContentPart(t, "Lượt thăm sáng mai lúc 9:00"))
                        .toList(),
                Optional.of("Nhắc lượt thăm"),
                Optional.of("Lượt thăm"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }

    private Map<String, Object> send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        return awaitRequests(r.eventKey(), 1).getFirst();
    }

    private static LocalDateTime utc(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }

    @Test
    void R1_lowForResident_heldThenInAppAt0700() {
        long account = users.resident(resident);
        clock.setTo(at("2030-03-04T22:00:00"));

        long requestId = ((Number) send(request("r1-" + resident, NotificationLevel.LOW, InformationType.VISITS))
                        .get("id"))
                .longValue();

        Map<String, Object> n = jdbc.queryForMap(
                "SELECT n.id, n.recipient_type, n.recipient_id, n.account_id, s.status, s.scheduled_send_at"
                        + " FROM notification n JOIN notification_state s ON s.notification_id = n.id"
                        + " WHERE n.request_id = ?",
                requestId);
        assertThat(n)
                .containsEntry("recipient_type", "RESIDENT")
                .containsEntry("recipient_id", resident)
                .containsEntry("account_id", account)
                .containsEntry("status", "HELD_FOR_QUIET_HOURS")
                .containsEntry("scheduled_send_at", utc(at("2030-03-05T07:00:00")));

        clock.setTo(at("2030-03-05T07:00:00"));
        release.run();

        long id = ((Number) n.get("id")).longValue();
        await().atMost(Duration.ofSeconds(10))
                .until(() -> "SENT"
                        .equals(jdbc.queryForObject(
                                "SELECT status FROM notification_state WHERE notification_id = ?", String.class, id)));
        assertThat(jdbc.queryForObject(
                        "SELECT channel FROM delivery_attempt WHERE notification_id = ?", String.class, id))
                .isEqualTo("IN_APP");
    }

    @Test
    void R2_mediumOrHealthForResident_rejected() {
        users.resident(resident);

        Map<String, Object> medium =
                send(request("r2-medium-" + resident, NotificationLevel.MEDIUM, InformationType.GENERAL));
        Map<String, Object> health =
                send(request("r2-health-" + resident, NotificationLevel.LOW, InformationType.HEALTH));
        Map<String, Object> costs = send(request("r2-costs-" + resident, NotificationLevel.LOW, InformationType.COSTS));

        for (Map<String, Object> r : List.of(medium, health, costs)) {
            assertThat(r)
                    .containsEntry("intake_result", "REJECTED")
                    .containsEntry("rejection_reason", "INVALID_RESIDENT_GROUP");
        }
    }

    @Test
    void R3_residentWithoutAccount_noRecipient_notADeliveryError() {
        Map<String, Object> r = send(request("r3-" + resident, NotificationLevel.LOW, InformationType.GENERAL));

        assertThat(r.get("intake_result")).isEqualTo("VALID");
        assertThat((String) r.get("flags")).doesNotContain("NO_RECIPIENT");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM notification WHERE request_id = ?", Integer.class, r.get("id")))
                .isZero();
    }
}
