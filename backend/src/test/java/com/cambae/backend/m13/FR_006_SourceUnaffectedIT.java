package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * FR-006, research B-03 (009 T012; quickstart Q14): lỗi của yêu cầu thông báo không làm hỏng lệnh nguồn; lỗi được ghi
 * vào yêu cầu bị từ chối để hiện ở danh sách lỗi gửi.
 */
class FR_006_SourceUnaffectedIT extends M13IntegrationTestBase {

    /** Sự kiện nguồn dựng yêu cầu bị lỗi. */
    record BrokenSourceEvent(String marker) implements HasNotificationRequests {
        @Override
        public List<NotificationRequest> notificationRequests() {
            throw new IllegalStateException("module nguồn dựng yêu cầu lỗi " + marker);
        }
    }

    private int sourceRows(String note) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sample_office_record WHERE note = ?", Integer.class, note);
    }

    @Test
    void FR_006_invalidRequest_sourceCommandStillCommits() {
        String key = UUID.randomUUID().toString();
        NotificationRequest invalid = FR_003_RequestValidationIT.withKey(
                RequestValidationTest.request(
                        Optional.of(NotificationLevel.URGENT),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        Optional.empty()),
                key,
                new SourceRecord("M05.INCIDENT", key));

        sourceCommand("fr006-" + key, new SampleSourceEvent(List.of(invalid)));

        assertThat(sourceRows("fr006-" + key)).isEqualTo(1);
        assertThat(awaitRequests(key, 1).getFirst().get("intake_result")).isEqualTo("REJECTED");
    }

    @Test
    void FR_006_requestBuildFailure_sourceCommandStillCommits_andErrorIsRecorded() {
        String marker = UUID.randomUUID().toString();

        sourceCommand("fr006-" + marker, new BrokenSourceEvent(marker));

        assertThat(sourceRows("fr006-" + marker)).isEqualTo(1);
        await().atMost(Duration.ofSeconds(10)).until(() -> !rejectedBuilds().isEmpty());
        Map<String, Object> row = rejectedBuilds().getLast();
        assertThat(row.get("event_type")).isEqualTo(BrokenSourceEvent.class.getName());
    }

    private List<Map<String, Object>> rejectedBuilds() {
        return jdbc.queryForList(
                "SELECT * FROM notification_request WHERE rejection_reason = 'REQUEST_BUILD_FAILED' ORDER BY id");
    }
}
