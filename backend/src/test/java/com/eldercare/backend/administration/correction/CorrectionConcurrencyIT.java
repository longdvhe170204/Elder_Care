package com.eldercare.backend.administration.correction;

import static com.eldercare.backend.administration.sample.CorrectionFixtures.NURSE_A;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.sample.CorrectionFixtures;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** FR-027, FR-028 (000 T051): hai đính chính đồng thời trên cùng bản gốc nhận số thứ tự liên tiếp, không trùng. */
@Import(CorrectionFixtures.class)
class CorrectionConcurrencyIT extends MySqlIntegrationTestBase {

    @Autowired
    CorrectionApi corrections;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_027_concurrentCorrections_getConsecutiveSequenceNumbers() throws Exception {
        user.set(NURSE_A);
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note) VALUES (UTC_TIMESTAMP(6), ?, 2, 'gốc')",
                NURSE_A.accountId());
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> tasks = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String note = "lần " + i;
            tasks.add(pool.submit(() -> {
                start.await();
                corrections.create(
                        SampleFloorRecordProvider.TYPE,
                        id,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("note", note),
                        new Reason("sửa đồng thời"));
                return null;
            }));
        }
        start.countDown();
        for (Future<?> task : tasks) {
            task.get();
        }
        pool.shutdown();

        assertThat(jdbc.queryForList(
                        "SELECT sequence_no FROM correction WHERE record_type = ? AND original_record_id = ? ORDER BY sequence_no",
                        Integer.class,
                        SampleFloorRecordProvider.TYPE,
                        id))
                .containsExactly(1, 2);
    }
}
