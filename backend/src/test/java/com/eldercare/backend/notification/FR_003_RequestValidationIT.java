package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * FR-003, FR-004, FR-013, research B-02 (009 T012; quickstart Q14): yêu cầu được lưu kèm thời điểm tiếp nhận; thiếu
 * trường bắt buộc thì lưu với kết quả bị từ chối và lý do; thiếu mức thì Nhẹ kèm dấu; phần nội dung được lưu.
 */
class FR_003_RequestValidationIT extends M13IntegrationTestBase {

    static NotificationRequest withKey(NotificationRequest r, String key, SourceRecord source) {
        return new NotificationRequest(
                r.sourceModule(),
                r.eventType(),
                r.sourceGroup(),
                source,
                key,
                r.level(),
                r.residentId(),
                r.floorAreaId(),
                r.recipientGroups(),
                r.content(),
                r.shortContent(),
                r.title(),
                r.causedBy(),
                r.notifyCauser(),
                r.occurredAt(),
                r.displayParams(),
                r.afterDeath(),
                r.informationSubject(),
                r.q354Case());
    }

    @Test
    void FR_004_validRequest_isStoredWithReceivedTimeAndContent() {
        String key = UUID.randomUUID().toString();
        NotificationRequest r = withKey(
                RequestValidationTest.request(
                        Optional.of(NotificationLevel.MEDIUM),
                        RequestValidationTest.MANAGERS,
                        List.of(
                                new ContentPart(InformationType.GENERAL, "Có sự cố"),
                                new ContentPart(null, "Ngã, bầm tay")),
                        Optional.of("Có sự cố mới"),
                        Optional.empty()),
                key,
                new SourceRecord("M05.INCIDENT", key));

        sourceCommand("fr004", new SampleSourceEvent(List.of(r)));

        Map<String, Object> row = awaitRequests(key, 1).getFirst();
        assertThat(row.get("intake_result")).isEqualTo("VALID");
        assertThat(row.get("level")).isEqualTo("MEDIUM");
        assertThat(row.get("received_at")).isNotNull();
        assertThat(row.get("flags")).isEqualTo("MISSING_INFORMATION_TYPE");
        assertThat(jdbc.queryForList(
                        "SELECT information_type FROM notification_request_content WHERE request_id = ? ORDER BY seq",
                        String.class,
                        row.get("id")))
                .containsExactly("GENERAL", "HEALTH");
        assertThat(jdbc.queryForList(
                        "SELECT group_code FROM notification_request_recipient_group WHERE request_id = ?",
                        String.class,
                        row.get("id")))
                .containsExactly("FacilityRole");
    }

    @Test
    void FR_003_missingLevel_isStoredAsLowWithFlag() {
        String key = UUID.randomUUID().toString();
        NotificationRequest r = withKey(
                RequestValidationTest.request(
                        Optional.empty(),
                        RequestValidationTest.MANAGERS,
                        RequestValidationTest.GENERAL,
                        Optional.empty(),
                        Optional.empty()),
                key,
                new SourceRecord("M05.INCIDENT", key));

        sourceCommand("fr003-level", new SampleSourceEvent(List.of(r)));

        Map<String, Object> row = awaitRequests(key, 1).getFirst();
        assertThat(row.get("intake_result")).isEqualTo("VALID");
        assertThat(row.get("level")).isEqualTo("LOW");
        assertThat(row.get("flags")).isEqualTo("MISSING_LEVEL");
    }

    @Test
    void FR_003_missingRecipientGroups_isStoredAsRejected() {
        String key = UUID.randomUUID().toString();
        NotificationRequest r = withKey(
                RequestValidationTest.request(
                        Optional.of(NotificationLevel.LOW),
                        List.of(),
                        RequestValidationTest.GENERAL,
                        Optional.empty(),
                        Optional.empty()),
                key,
                new SourceRecord("M05.INCIDENT", key));

        sourceCommand("fr003-reject", new SampleSourceEvent(List.of(r)));

        Map<String, Object> row = awaitRequests(key, 1).getFirst();
        assertThat(row.get("intake_result")).isEqualTo("REJECTED");
        assertThat(row.get("rejection_reason")).isEqualTo("MISSING_RECIPIENT_GROUPS");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM notification WHERE request_id = ?", Integer.class, row.get("id")))
                .isZero();
    }
}
