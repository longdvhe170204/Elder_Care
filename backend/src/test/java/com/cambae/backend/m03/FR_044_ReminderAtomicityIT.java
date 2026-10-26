package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cambae.backend.m03.internal.bed.persistence.BedReadinessStore;
import com.cambae.backend.m03.scheduling.BedReadinessReminderJob;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Transaction processor kết thúc trước assertion, không che lỗi rollback bằng transaction của test. */
class FR_044_ReminderAtomicityIT extends StructureTestSupport {
    @Autowired
    BedReadinessReminderJob job;

    @MockitoSpyBean
    BedReadinessStore store;

    @Test
    void FR_044_failedReminderRollsBackMarkerAndCanRetry() {
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(7200)),
                Timestamp.from(time.now().minusSeconds(3600)),
                bed);
        doAnswer(invocation -> {
                    invocation.callRealMethod();
                    throw new IllegalStateException("Lỗi sau đánh dấu");
                })
                .when(store)
                .mark(eq(bed), any(Instant.class));
        assertThatThrownBy(job::runNow)
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT ready_reminded_at FROM bed WHERE id=?", Timestamp.class, bed))
                .isNull();
        doCallRealMethod().when(store).mark(eq(bed), any(Instant.class));
        job.runNow();
        assertThat(jdbc.queryForObject("SELECT ready_reminded_at FROM bed WHERE id=?", Timestamp.class, bed))
                .isNotNull();
    }
}
