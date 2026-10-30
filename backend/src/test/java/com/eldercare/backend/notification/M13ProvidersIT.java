package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
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
                        + " resident_id, floor_area_id, received_at, intake_result, contents, recipient_groups)"
                        + " VALUES ('M05.INCIDENT', ?, ?, 'LOW', ?, 4, ?, 'VALID', JSON_ARRAY(JSON_OBJECT('seq', 1,"
                        + " 'informationType', 'GENERAL', 'content', 'x')), JSON_ARRAY())",
                key,
                key,
                residentId,
                receivedAt);
        long request = jdbc.queryForObject("SELECT id FROM notification_request WHERE event_key = ?", Long.class, key);
        jdbc.update(
                "INSERT INTO notification (request_id, recipient_type, recipient_id, basis, level, created_at)"
                        + " VALUES (?, 'STAFF', 1, '[]', 'LOW', ?)",
                request,
                receivedAt);
        long notification =
                jdbc.queryForObject("SELECT id FROM notification WHERE request_id = ?", Long.class, request);
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                        + " confirmed_at) VALUES ('M13.NOTIFICATION', ?, 'M13', 'M13.NOTIFICATION_EVENT',"
                        + " JSON_OBJECT('type', 'SEND'), TRUE, ?, ?)",
                notification,
                receivedAt,
                receivedAt);
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                        + " confirmed_at) VALUES ('M13.NOTIFICATION', ?, 'M13', 'M13.DELIVERY_ATTEMPT',"
                        + " JSON_OBJECT('channel', 'IN_APP', 'attemptNo', 1), TRUE, ?, ?)",
                notification,
                receivedAt,
                receivedAt);
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                        + " confirmed_at) SELECT 'M13.DELIVERY_ATTEMPT', id, 'M13', 'M13.DELIVERY_RESULT',"
                        + " JSON_OBJECT('result', 'SENT'), TRUE, ?, ? FROM record_event WHERE kind = 'M13.DELIVERY_ATTEMPT'"
                        + " AND entity_type = 'M13.NOTIFICATION' AND entity_id = ?",
                receivedAt,
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
                        "SELECT COUNT(*) FROM record_event WHERE entity_type = 'M13.NOTIFICATION' AND entity_id = ?",
                        Integer.class,
                        old[1]))
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
                        + " created_at, status, final_result, due_at, current_staff_id, callee_key) VALUES ('FAMILY_MEMBER',"
                        + " 7, 'M05.INCIDENT', 'x', 'UNACKNOWLEDGED', '2001-01-01', 'RESULT_RECORDED', 'NOT_REACHED',"
                        + " '2001-01-01', 1, UUID())");
        long root = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO call_reminder (call_chain_id, callee_type, callee_id, creation_reason, created_at, status,"
                        + " final_result, due_at, current_staff_id, callee_key) VALUES (?, 'FAMILY_MEMBER', 8,"
                        + " 'PREVIOUS_NOT_REACHED', '2001-01-01', 'RESULT_RECORDED', 'NOT_REACHED', '2001-01-01', 1, UUID())",
                root);
        long next = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        String child = "INSERT INTO record_event (entity_type, entity_id, module, kind, content, actor_id, by_system,"
                + " occurred_at, confirmed_at) VALUES ('M13.CALL_REMINDER', ?, 'M13', ?, ?, ?, ?, '2001-01-01',"
                + " '2001-01-01')";
        jdbc.update(child, root, "M13.CALL_REMINDER_NOTIFICATION", "{\"notificationId\": " + old[1] + "}", null, true);
        for (long id : new long[] {root, next}) {
            jdbc.update(
                    child,
                    id,
                    "M13.CALL_REMINDER_HANDOFF",
                    "{\"remindedStaffId\": 1, \"tier\": \"MANAGERS\", \"reason\": \"INITIAL\"}",
                    null,
                    true);
            jdbc.update(
                    child,
                    id,
                    "M13.CALL_RESULT",
                    "{\"result\": \"NOT_REACHED\", \"reason\": \"NO_ANSWER\", \"supplementary\": false}",
                    1,
                    false);
        }

        p.delete(List.of(old[0]));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM call_reminder WHERE id IN (?, ?)", Integer.class, root, next))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE entity_type = 'M13.CALL_REMINDER' AND entity_id IN (?, ?)",
                        Integer.class,
                        root,
                        next))
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
