package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** DBR-18, SC-003: nhiều phát sinh cùng khóa loại đến đồng thời chỉ cho một cảnh báo đang mở với số lần đúng. */
class DBR_18_ConcurrentMergeIT extends AlertRequestTestBase {
    private static final String KEY = "VUOT_NGUONG:77";
    private static final int THREADS = 10;

    @Test
    void SC_003_tenConcurrentOccurrencesGiveOneOpenAlertCountedTen() throws Exception {
        var pool = Executors.newFixedThreadPool(THREADS);
        var start = new CountDownLatch(1);
        var results = new ArrayList<Future<?>>();
        try {
            for (int i = 0; i < THREADS; i++) {
                long source = i + 1;
                results.add(pool.submit(() -> {
                    start.await();
                    return request(KEY, IncidentLevel.MEDIUM, T0.plusSeconds(source), source);
                }));
            }
            start.countDown();
            for (var result : results) result.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertThat(openIds(KEY)).hasSize(1);
        assertThat(alerts.findById(openIds(KEY).getFirst()).orElseThrow().occurrenceCount())
                .isEqualTo(THREADS);
    }
}
