package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Research B-13, data-model mục 9, 10 (009 T011): phân loại trường của thông báo (phần sức khỏe, chi phí; số điện
 * thoại); lưu giữ yêu cầu thông báo theo CFG-M15-04, yêu cầu gắn người cao tuổi chưa hết hạn; gắn phạm vi theo người cao
 * tuổi và tầng của yêu cầu.
 */
class M13ProvidersIT extends M13IntegrationTestBase {

    @Autowired
    List<FieldClassificationProvider> classifications;

    @Autowired
    List<RetentionProvider> retentions;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    private <T> T only(List<T> providers, java.util.function.Function<T, String> type, String value) {
        return providers.stream()
                .filter(p -> type.apply(p).equals(value))
                .findFirst()
                .orElseThrow();
    }

    /** Yêu cầu kèm một thông báo, sự kiện, lần gửi và kết quả; trả (id yêu cầu, id thông báo). */
    private long[] requestWithNotification(String receivedAt, Long residentId) {
        String key = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO notification_request (source_record_type, source_record_id, event_key, level,"
                        + " resident_id, floor_area_id, received_at, intake_result) VALUES ('M05.INCIDENT', ?, ?,"
                        + " 'LOW', ?, 4, ?, 'VALID')",
                key,
                key,
                residentId,
                receivedAt);
        long request = jdbc.queryForObject("SELECT id FROM notification_request WHERE event_key = ?", Long.class, key);
        jdbc.update(
                "INSERT INTO notification_request_content (request_id, seq, information_type, content)"
                        + " VALUES (?, 1, 'GENERAL', 'x')",
                request);
        jdbc.update(
                "INSERT INTO notification (request_id, recipient_type, recipient_id, basis, level, created_at)"
                        + " VALUES (?, 'STAFF', 1, '[]', 'LOW', ?)",
                request,
                receivedAt);
        long notification =
                jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, request);
        jdbc.update(
                "INSERT INTO notification_state (notification_id, recipient_key, level) VALUES (?, 'STAFF:1', 'LOW')",
                notification);
        jdbc.update(
                "INSERT INTO notification_event (notification_id, type, occurred_at) VALUES (?, 'SEND', ?)",
                notification,
                receivedAt);
        jdbc.update(
                "INSERT INTO delivery_attempt (notification_id, channel, attempt_no, scheduled_at)"
                        + " VALUES (?, 'IN_APP', 1, ?)",
                notification,
                receivedAt);
        jdbc.update(
                "INSERT INTO delivery_result (attempt_id, result, occurred_at) SELECT id, 'SENT', ? FROM"
                        + " delivery_attempt WHERE notification_id = ?",
                receivedAt,
                notification);
        return new long[] {request, notification};
    }

    @Test
    void B_13_fieldClassification_healthAndCostPartsAndPhoneNumbers() {
        FieldClassificationProvider p =
                only(classifications, FieldClassificationProvider::entityType, "M13.NOTIFICATION");

        assertThat(p.fieldGroups())
                .containsEntry("content.HEALTH", InfoGroup.HEALTH)
                .containsEntry("content.COSTS", InfoGroup.COSTS)
                .containsEntry("address", InfoGroup.CONTACT)
                .doesNotContainKey("content.GENERAL");
    }

    @Test
    void FR_041_retention_expiresRequestsWithoutResident_andDeletesWholeChain() {
        RetentionProvider p = only(retentions, RetentionProvider::dataType, "M13.NOTIFICATION_REQUEST");
        long[] old = requestWithNotification("2001-01-01 00:00:00", null);
        long[] linked = requestWithNotification("2001-01-01 00:00:00", 55L);

        List<Long> candidates = p.expiredCandidates(Instant.parse("2002-01-01T00:00:00Z"), 1000)
                .map(RetentionProvider.Candidate::id)
                .toList();
        p.delete(List.of(old[0]));

        assertThat(p.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        assertThat(candidates).contains(old[0]).doesNotContain(linked[0]);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification_request WHERE id = ?", Integer.class, old[0]))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, old[1]))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM delivery_attempt WHERE notification_id = ?", Integer.class, old[1]))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, linked[1]))
                .isEqualTo(1);
    }

    @Test
    void FR_041_retention_deletesCallRemindersOfExpiredNotifications_withWholeCallChain() {
        RetentionProvider p = only(retentions, RetentionProvider::dataType, "M13.NOTIFICATION_REQUEST");
        long[] old = requestWithNotification("2001-01-01 00:00:00", null);
        jdbc.update(
                "INSERT INTO call_reminder (callee_type, callee_id, source_record_type, source_record_id, creation_reason,"
                        + " created_at) VALUES ('FAMILY_MEMBER', 7, 'M05.INCIDENT', 'x', 'UNACKNOWLEDGED', '2001-01-01')");
        long root = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO call_reminder (call_chain_id, callee_type, callee_id, creation_reason, created_at)"
                        + " VALUES (?, 'FAMILY_MEMBER', 8, 'PREVIOUS_NOT_REACHED', '2001-01-01')",
                root);
        long next = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO call_reminder_notification (call_reminder_id, notification_id, merged_at)"
                        + " VALUES (?, ?, '2001-01-01')",
                root,
                old[1]);
        for (long id : new long[] {root, next}) {
            jdbc.update(
                    "INSERT INTO call_reminder_handoff (call_reminder_id, reminded_staff_id, tier, reason, due_at,"
                            + " occurred_at) VALUES (?, 1, 'MANAGERS', 'INITIAL', '2001-01-01', '2001-01-01')",
                    id);
            jdbc.update(
                    "INSERT INTO call_reminder_state (call_reminder_id, status, final_result, due_at, current_staff_id,"
                            + " callee_key) VALUES (?, 'RESULT_RECORDED', 'NOT_REACHED', '2001-01-01', 1, ?)",
                    id,
                    "retention:" + id);
            jdbc.update(
                    "INSERT INTO call_result (call_reminder_id, called_by, called_at, result, reason, recorded_at)"
                            + " VALUES (?, 1, '2001-01-01', 'NOT_REACHED', 'NO_ANSWER', '2001-01-01')",
                    id);
        }

        p.delete(List.of(old[0]));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM call_reminder WHERE id IN (?, ?)", Integer.class, root, next))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM call_result WHERE call_reminder_id IN (?, ?)", Integer.class, root, next))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE id = ?", Integer.class, old[1]))
                .isZero();
    }

    @Test
    void FR_034_scopeOfNotification_isResidentAndFloorOfRequest() {
        ScopeAssignmentProvider p = only(scopes, ScopeAssignmentProvider::entityType, "M13.NOTIFICATION");
        long[] n = requestWithNotification("2026-10-21 01:00:00", 77L);

        Target t = p.assignScope(n[1]);

        assertThat(t).isEqualTo(new Target("M13.NOTIFICATION", Long.toString(n[1]), 77L, 4L));
    }
}
