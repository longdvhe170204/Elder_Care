package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.transaction.TestTransaction;

/** T033: hai transaction độc lập cạnh tranh cùng một task; không dùng rollback của test để giả lập khóa. */
class FR_036_CareResultConcurrencyIT extends CareResultTestBase {
    @Test
    void FR_036_onlyOneConcurrentRecordingCommits() throws Exception {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            Callable<Integer> command = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Chưa mở cổng cạnh tranh");
                return mvc.perform(post("/api/v1/care-tasks/" + task + "/record")
                                .with(user("concurrency-test"))
                                .header("Idempotency-Key", "concurrent-" + TestUsers.nextId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"numericValue\":200}"))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            };
            var first = executor.submit(command);
            var second = executor.submit(command);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM care_task WHERE id=? AND status='COMPLETED' AND result_performed_at IS NOT NULL",
                            Long.class,
                            task))
                    .isEqualTo(1);
        } finally {
            // Chỉ dọn fixture của ca này bằng kết nối test retention, không đổi quyền ứng dụng.
            try (var connection = retentionConnection()) {
                for (String sql : List.of(
                        "DELETE FROM status_history WHERE entity_type='M04.TASK' AND entity_id=?",
                        "DELETE FROM audit_log WHERE entity_type IN('M04.TASK','M04.CARE_RESULT') AND entity_id=?",
                        "DELETE FROM care_task WHERE id=?"))
                    try (var statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, task);
                        statement.executeUpdate();
                    }
            }
            residents.changeStatus(resident, ResidentStatus.RESIDENCY_ENDED, time.now());
            TestTransaction.start();
        }
    }
}
