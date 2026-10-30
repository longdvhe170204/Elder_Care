package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.MANAGER_2;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * US5 #12; FR-040 (000 T088; quickstart K8): hai quyết định với cùng If-Match: một bên được ghi nhận, bên kia 409
 * {@code ALREADY_PROCESSED}, kể cả khi gửi đồng thời.
 */
class ConcurrentDecisionIT extends ApprovalTestBase {

    @Test
    void US5_12_sequentialSecondDecisionWithSameIfMatch_isAlreadyProcessed() throws Exception {
        long id = pending(time.today().plusDays(5));
        user.set(MANAGER);
        String etag = api.etag(id);

        MockHttpServletResponse first =
                api.command(id, "approve", etag, "{}").andReturn().getResponse();
        user.set(MANAGER_2);
        MockHttpServletResponse second = api.command(id, "reject", etag, "{\"comment\":\"Không đồng ý\"}")
                .andReturn()
                .getResponse();

        assertThat(first.getStatus()).isEqualTo(200);
        assertThat(second.getStatus()).isEqualTo(409);
        assertThat(second.getContentAsString()).contains("ALREADY_PROCESSED");
    }

    @Test
    void FR_040_concurrentDecisions_exactlyOneRecorded() throws Exception {
        long id = pending(time.today().plusDays(5));
        user.set(MANAGER);
        String etag = api.etag(id);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> decide = () -> {
            start.await();
            return api.command(id, "approve", etag, "{}")
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            results.add(pool.submit(decide));
            results.add(pool.submit(decide));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) {
                statuses.add(result.get());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'REQUEST_APPROVED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(1);
    }

    @Test
    void FR_040_staleIfMatchOnPendingRequest_isVersionMismatch() throws Exception {
        LocalDate date = time.today().plusDays(5);
        long id = pending(date);
        user.set(MANAGER);

        assertThat(api.command(id, "approve", "\"999\"", "{}")
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(412);
    }
}
