package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/**
 * FR-005, NFR-04, US2 #8 (009 T012; quickstart Q36): yêu cầu duy nhất theo (bản ghi nguồn, khóa sự kiện); lần nhận
 * trùng chỉ ghi lần nhận, không tạo yêu cầu mới, kể cả khi hai lần đến đồng thời; khóa khác là yêu cầu mới.
 */
class FR_005_DeduplicationIT extends M13IntegrationTestBase {

    private NotificationRequest request(String sourceId, String key) {
        return FR_003_RequestValidationIT.withKey(
                RequestValidationTest.request(
                        Optional.of(NotificationLevel.LOW),
                        RequestValidationTest.MANAGERS,
                        RequestValidationTest.GENERAL,
                        Optional.empty(),
                        Optional.empty()),
                key,
                new SourceRecord("M04.CARE_TASK", sourceId));
    }

    private int duplicates(Object requestId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM reminder_marker WHERE kind = 'M13.REQUEST_DUPLICATE'"
                        + " AND entity_type = 'M13.NOTIFICATION_REQUEST' AND entity_id = ?",
                Integer.class,
                requestId);
    }

    @Test
    void FR_005_sameSourceAndKey_isRecordedAsDuplicate() {
        String source = UUID.randomUUID().toString();
        String key = "overdue#1-" + source;

        sourceCommand("fr005-a", new SampleSourceEvent(List.of(request(source, key))));
        Object id = awaitRequests(key, 1).getFirst().get("id");
        sourceCommand("fr005-b", new SampleSourceEvent(List.of(request(source, key))));

        await().atMost(Duration.ofSeconds(10)).until(() -> duplicates(id) == 1);
        assertThat(requests(key)).hasSize(1);
    }

    @Test
    void FR_005_concurrentDuplicates_createOneRequest() {
        String source = UUID.randomUUID().toString();
        String key = "overdue#1-" + source;

        CompletableFuture<?> a = CompletableFuture.runAsync(
                () -> sourceCommand("fr005-c1", new SampleSourceEvent(List.of(request(source, key)))));
        CompletableFuture<?> b = CompletableFuture.runAsync(
                () -> sourceCommand("fr005-c2", new SampleSourceEvent(List.of(request(source, key)))));
        CompletableFuture.allOf(a, b).join();

        Object id = awaitRequests(key, 1).getFirst().get("id");
        await().atMost(Duration.ofSeconds(10)).until(() -> duplicates(id) == 1);
        assertThat(requests(key)).hasSize(1);
    }

    @Test
    void FR_005_newKeyForRepeatedReminder_isNewRequest() {
        String source = UUID.randomUUID().toString();

        sourceCommand("fr005-k1", new SampleSourceEvent(List.of(request(source, "overdue#1"))));
        sourceCommand("fr005-k2", new SampleSourceEvent(List.of(request(source, "overdue#2"))));

        await().atMost(Duration.ofSeconds(10))
                .until(() -> jdbc.queryForObject(
                                "SELECT COUNT(*) FROM notification_request WHERE source_record_id = ?",
                                Integer.class,
                                source)
                        == 2);
    }
}
