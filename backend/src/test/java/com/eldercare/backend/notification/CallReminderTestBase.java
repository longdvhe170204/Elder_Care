package com.eldercare.backend.notification;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountView;
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
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.notification.internal.urgent.service.CallReminderCreation;
import com.eldercare.backend.notification.internal.urgent.service.CallReminderHandoffs;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.notification.FakeFamilyNotifications;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Dữ liệu dựng chung cho test lượt nhắc gọi (009 US1, T017): yêu cầu Khẩn cấp từ module nguồn về một người cao tuổi ở một
 * tầng; tua đồng hồ giả lập qua CFG-M13-01, CFG-M13-04 (mặc định 5 phút) rồi gọi thẳng hai việc 20 giây.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
abstract class CallReminderTestBase extends M13IntegrationTestBase {

    static final Duration PAST_CFG = Duration.ofMinutes(6);

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeStaffProfiles profiles;

    @Autowired
    FakeFamilyNotifications family;

    @Autowired
    AccountApi accounts;

    @Autowired
    CallReminderCreation creation;

    @Autowired
    CallReminderHandoffs handoffs;

    TestUsers users;
    long resident;
    long floor;
    Instant start;

    @BeforeEach
    void setUpReminders() {
        isolateFromOtherTests();
        start = time.now();
        clock.setTo(start);
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
        floor = TestUsers.nextId();
    }

    /**
     * Hai việc 20 giây xét toàn cơ sở dữ liệu (DB test giữ qua các lần chạy): dừng lượt Chờ, đánh dấu đã xác nhận thông báo
     * Khẩn cấp còn sót của test khác để mỗi test chỉ xét dữ liệu của mình. Luồng tiếp nhận của test trước có thể còn ghi
     * nên thử lại khi gặp deadlock.
     */
    private void isolateFromOtherTests() {
        for (int attempt = 1; ; attempt++) {
            try {
                jdbc.update("UPDATE call_reminder SET status = 'STOPPED', stop_reason = 'TEST_ISOLATION'"
                        + " WHERE status = 'AWAITING_RESULT'");
                jdbc.update("INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system,"
                        + " occurred_at, confirmed_at, unique_key) SELECT 'M13.NOTIFICATION', n.id, 'M13',"
                        + " 'M13.NOTIFICATION_EVENT', JSON_OBJECT('type', 'ACKNOWLEDGE', 'acknowledgementMethod',"
                        + " 'IN_APP'), TRUE, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), CONCAT('M13.NE:', n.id, ':ACKNOWLEDGE')"
                        + " FROM notification n WHERE n.level = 'URGENT' AND n.acknowledged_at IS NULL"
                        + " AND n.sent_at IS NOT NULL AND NOT EXISTS (SELECT 1 FROM record_event e"
                        + " WHERE e.unique_key = CONCAT('M13.NE:', n.id, ':ACKNOWLEDGE'))");
                jdbc.update("UPDATE notification SET status = 'ACKNOWLEDGED', acknowledged_at = UTC_TIMESTAMP(6),"
                        + " acknowledgement_method = 'IN_APP' WHERE level = 'URGENT' AND acknowledged_at IS NULL"
                        + " AND sent_at IS NOT NULL");
                return;
            } catch (org.springframework.dao.PessimisticLockingFailureException e) {
                if (attempt == 5) {
                    throw e;
                }
            }
        }
    }

    @AfterEach
    void resetReminders() {
        clock.reset();
        family.reset();
    }

    void advance(Duration d) {
        clock.setTo(time.now().plus(d));
    }

    NotificationRequest urgent(String key, RecipientGroup... groups) {
        return new NotificationRequest(
                "M05",
                "INCIDENT_URGENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.INCIDENT", key),
                key,
                Optional.of(NotificationLevel.URGENT),
                Optional.of(resident),
                Optional.of(floor),
                List.of(groups),
                List.of(new ContentPart(InformationType.GENERAL, "Ngã trong phòng")),
                Optional.of("Có sự cố khẩn cấp " + key),
                Optional.of("Sự cố"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.of(InformationSubject.ABOUT_FAMILY_MATTER),
                Optional.empty());
    }

    /** Gửi một yêu cầu; trả id thông báo theo người nhận ("loại:id"). */
    Map<String, Long> send(NotificationRequest r) {
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        long requestId = ((Number) awaitRequests(r.eventKey(), 1).getFirst().get("id")).longValue();
        Map<String, Long> out = new java.util.HashMap<>();
        jdbc.queryForList("SELECT id, recipient_type, recipient_id FROM notification WHERE request_id = ?", requestId)
                .forEach(n -> out.put(
                        n.get("recipient_type") + ":" + n.get("recipient_id"), ((Number) n.get("id")).longValue()));
        return out;
    }

    long staffNotification(Map<String, Long> sent, TestUser u) {
        return sent.get("STAFF:" + u.staffId());
    }

    ResultActions acknowledge(long notificationId, TestUser u) throws Exception {
        return mvc.perform(post("/api/v1/notifications/" + notificationId + "/acknowledge")
                .header("Authorization", bearer(u))
                .header("Idempotency-Key", UUID.randomUUID().toString()));
    }

    String bearer(TestUser u) {
        return TestUsers.bearer(users.session(u.accountId()));
    }

    /** Lượt nhắc gọi của người cần gọi, theo thứ tự tạo. */
    List<Long> reminders(String calleeType, long calleeId) {
        return jdbc.queryForList(
                "SELECT id FROM call_reminder WHERE callee_type = ? AND callee_id = ? ORDER BY id",
                Long.class,
                calleeType,
                calleeId);
    }

    Map<String, Object> state(long reminderId) {
        return jdbc.queryForMap("SELECT * FROM call_reminder WHERE id = ?", reminderId);
    }

    List<Map<String, Object>> handoffRows(long reminderId) {
        return jdbc.queryForList(
                "SELECT JSON_VALUE(h.content, '$.previousStaffId' RETURNING SIGNED) AS previous_staff_id,"
                        + " JSON_VALUE(h.content, '$.remindedStaffId' RETURNING SIGNED) AS reminded_staff_id,"
                        + " h.content->>'$.tier' AS tier, h.content->>'$.reason' AS reason FROM record_event h"
                        + " WHERE h.kind = 'M13.CALL_REMINDER_HANDOFF' AND h.entity_type = 'M13.CALL_REMINDER'"
                        + " AND h.entity_id = ? ORDER BY h.id",
                reminderId);
    }

    /** Thông báo M13 gửi tới nhân viên về lượt nhắc gọi (nhắc, chuyển, dừng, cập nhật), theo mức. */
    void awaitReminderNotices(long reminderId, long staffId, String level, int count) {
        await().atMost(Duration.ofSeconds(10)).until(() -> reminderNotices(reminderId, staffId, level) == count);
    }

    int reminderNotices(long reminderId, long staffId, String level) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notification n JOIN notification_request q ON q.id = n.request_id"
                        + " WHERE q.source_record_type = 'M13.CALL_REMINDER' AND q.source_record_id = ?"
                        + " AND n.recipient_type = 'STAFF' AND n.recipient_id = ? AND n.level = ?",
                Integer.class,
                Long.toString(reminderId),
                staffId,
                level);
    }

    String notificationStatus(long notificationId) {
        return jdbc.queryForObject(
                "SELECT CONCAT(IFNULL(status, ''), '/', IFNULL(acknowledgement_method, '')) FROM notification"
                        + " WHERE id = ?",
                String.class,
                notificationId);
    }

    /** Quản lý viện được giao ở bậc cuối: tài khoản tạo sớm nhất trong các Quản lý viện đang Hoạt động. */
    long firstManagerStaffId() {
        return accounts.activeWithRole(Role.NURSING_HOME_MANAGER).stream()
                .min(Comparator.comparingLong(AccountView::accountId))
                .map(AccountView::staffId)
                .orElseThrow();
    }
}
