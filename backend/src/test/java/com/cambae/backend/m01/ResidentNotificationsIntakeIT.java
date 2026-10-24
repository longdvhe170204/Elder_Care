package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.CareLevelChangeRequestNeeded;
import com.cambae.backend.m01.api.events.MissingConsentReminder;
import com.cambae.backend.m01.api.events.MissingEndOfLifeWishReminder;
import com.cambae.backend.m01.api.events.ReassessmentDue;
import com.cambae.backend.m01.api.events.ReassessmentOverdue;
import com.cambae.backend.m01.api.events.SelfDeterminationReviewReminder;
import com.cambae.backend.m01.api.events.TemporaryAbsenceOverdue;
import com.cambae.backend.support.fake.m13.FakeM13Config;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 001 T009 (FR-054, Q-97, Q-354; research H-14): thông báo M01 → M13 qua listener duy nhất của 009. Lệnh trạng thái
 * thật (Chuyển viện, Ghi nhận qua đời) và các sự kiện nhắc, cảnh báo được M13 nhận là hợp lệ: đúng mức, không thiếu
 * dấu nào (mức, nội dung rút gọn, đối tượng thông tin, mã Q-354), tin báo qua đời không bị coi là việc sau qua đời, và
 * gửi lại sự kiện cùng khóa không tạo yêu cầu thứ hai (009 FR-005). Người nhận phụ thuộc phân công và ca nên không kiểm
 * ở đây (bản giả của 008, 012 chưa có).
 */
@Import(FakeM13Config.class)
class ResidentNotificationsIntakeIT extends StatusTestBase {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate tx;

    private void publish(HasNotificationRequests event) {
        tx.executeWithoutResult(s -> publisher.publishEvent(event));
    }

    private List<Map<String, Object>> awaitRequests(String eventType, long residentId) {
        await().atMost(Duration.ofSeconds(15))
                .until(() -> !requests(eventType, residentId).isEmpty());
        return requests(eventType, residentId);
    }

    private List<Map<String, Object>> requests(String eventType, long residentId) {
        return jdbc.queryForList(
                "SELECT * FROM notification_request WHERE source_module = 'M01' AND event_type = ? AND resident_id = ?"
                        + " ORDER BY id",
                eventType,
                residentId);
    }

    private static void assertCleanAndValid(Map<String, Object> row, String level) {
        assertThat(row.get("intake_result")).isEqualTo("VALID");
        assertThat(row.get("level")).isEqualTo(level);
        // NO_RECIPIENT tùy dữ liệu người liên hệ chính của lần chạy; mọi dấu thiếu của module nguồn đều không được có
        assertThat(((String) row.get("flags")).replace("NO_RECIPIENT", "").replace(",", ""))
                .isEmpty();
    }

    @Test
    void Q_97_recordDeath_reachesTheNotificationModule_asAnUrgentNoticeForThePrimaryContact() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());

        statusService.recordDeath(target(id), reason(), noDetails());

        Map<String, Object> row = awaitRequests("RESIDENT_DEATH_RECORDED", id).getFirst();
        assertCleanAndValid(row, "URGENT");
        assertThat(row.get("information_subject")).isEqualTo("ABOUT_RESIDENT");
        assertThat(row.get("q354_case")).isEqualTo("DEATH");
        assertThat(row.get("source_record_type")).isEqualTo("M01.STATUS_HISTORY");
    }

    @Test
    void Q_97_transferOut_reachesTheNotificationModule_asAnUrgentNoticeForThePrimaryContact() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());
        var details = JsonNodeFactory.instance.objectNode();
        details.putObject("basis").put("incidentId", 9L);

        statusService.transferOut(target(id), reason(), details);

        Map<String, Object> row = awaitRequests("RESIDENT_TRANSFERRED_OUT", id).getFirst();
        assertCleanAndValid(row, "URGENT");
        assertThat(row.get("q354_case")).isEqualTo("TRANSFER");
    }

    @Test
    void FR_054_otherStatusCommands_createNoNotification() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        statusService.grantTemporaryAbsence(target(id), reason(), noDetails());

        assertThat(requests("RESIDENT_DEATH_RECORDED", id)).isEmpty();
        assertThat(requests("RESIDENT_TRANSFERRED_OUT", id)).isEmpty();
    }

    @Test
    void FR_054_reminderAndAlertEvents_areValidWithTheLevelsOfTheSpec() {
        long id = ThreadLocalRandom.current().nextLong(8_000_000_000L, 9_000_000_000L);
        Instant at = Instant.parse("2026-10-24T03:00:00Z");
        long key = System.nanoTime();

        publish(new ReassessmentDue(id, key, "PERIODIC", at));
        publish(new ReassessmentOverdue(id, at));
        publish(new CareLevelChangeRequestNeeded(id, key, CareLevel.SPECIAL, at));
        publish(new MissingConsentReminder(id, 1, at));
        publish(new MissingEndOfLifeWishReminder(id, 1, at));
        publish(new SelfDeterminationReviewReminder(id, key, null, at));
        publish(new TemporaryAbsenceOverdue(id, key, at, null, at));

        assertCleanAndValid(awaitRequests("REASSESSMENT_DUE", id).getFirst(), "LOW");
        assertCleanAndValid(awaitRequests("REASSESSMENT_OVERDUE", id).getFirst(), "MEDIUM");
        assertCleanAndValid(
                awaitRequests("CARE_LEVEL_CHANGE_REQUEST_NEEDED", id).getFirst(), "MEDIUM");
        assertCleanAndValid(awaitRequests("MISSING_CONSENT_REMINDER", id).getFirst(), "LOW");
        assertCleanAndValid(
                awaitRequests("MISSING_END_OF_LIFE_WISH_REMINDER", id).getFirst(), "LOW");
        assertCleanAndValid(
                awaitRequests("SELF_DETERMINATION_REVIEW_REMINDER", id).getFirst(), "LOW");
        assertCleanAndValid(awaitRequests("TEMPORARY_ABSENCE_OVERDUE", id).getFirst(), "MEDIUM");
    }

    @Test
    void FR_005_theSameEventKeyTwice_createsOneRequestOnly() {
        long id = ThreadLocalRandom.current().nextLong(8_000_000_000L, 9_000_000_000L);
        Instant at = Instant.parse("2026-10-24T03:00:00Z");

        publish(new MissingConsentReminder(id, 1, at));
        awaitRequests("MISSING_CONSENT_REMINDER", id);
        publish(new MissingConsentReminder(id, 1, at));
        publish(new MissingConsentReminder(id, 2, at));

        await().atMost(Duration.ofSeconds(15))
                .until(() -> requests("MISSING_CONSENT_REMINDER", id).size() == 2);
        assertThat(requests("MISSING_CONSENT_REMINDER", id)).hasSize(2);
    }
}
