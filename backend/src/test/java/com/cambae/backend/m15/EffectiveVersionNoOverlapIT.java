package com.cambae.backend.m15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.m15.sample.SampleService;
import com.cambae.backend.m15.sample.SampleUsers;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** FR-008, BR-M15-07 (000 T042): phiên bản của cùng đối tượng không chồng khoảng hiệu lực, kể cả khi thêm đồng thời. */
@Import(SampleUsers.class)
class EffectiveVersionNoOverlapIT extends MySqlIntegrationTestBase {

    @Autowired
    SampleService samples;

    @Autowired
    JdbcTemplate jdbc;

    private final Reason reason = new Reason("đổi đơn giá");

    private long newCase() {
        return samples.createCase(samples.createCatalogItem("Phiên bản " + UUID.randomUUID()), 1);
    }

    private int versions(long caseId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sample_case_version WHERE case_id = ?", Integer.class, caseId);
    }

    @Test
    void FR_008_overlappingVersion_isRejected_adjacentIsAccepted() {
        long caseId = newCase();
        samples.addCaseVersion(caseId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), reason);

        assertThatThrownBy(() -> samples.addCaseVersion(caseId, LocalDate.of(2026, 6, 30), null, reason))
                .isInstanceOf(ConditionNotMetException.class);
        samples.addCaseVersion(caseId, LocalDate.of(2026, 7, 1), null, reason);
        assertThatThrownBy(() -> samples.addCaseVersion(caseId, LocalDate.of(2030, 1, 1), null, reason))
                .as("phiên bản mở đầu còn hiệu lực tới vô hạn")
                .isInstanceOf(ConditionNotMetException.class);

        assertThat(versions(caseId)).isEqualTo(2);
    }

    @Test
    void FR_008_concurrentOverlappingVersions_onlyOneSucceeds() throws Exception {
        long caseId = newCase();
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int month = i + 1;
            tasks.add(pool.submit(() -> {
                start.await();
                try {
                    samples.addCaseVersion(caseId, LocalDate.of(2027, month, 1), null, reason);
                } catch (ConditionNotMetException e) {
                    failures.add(e);
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> task : tasks) {
            task.get();
        }
        pool.shutdown();

        assertThat(versions(caseId)).isEqualTo(1);
        assertThat(failures).hasSize(1);
    }
}
