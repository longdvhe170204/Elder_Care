package com.cambae.backend.m15.retention.scheduling;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.cambae.backend.m15.retention.internal.ExpiredDataPurge;
import org.junit.jupiter.api.Test;

/**
 * FR-051, plan 000 "Điểm cần báo lại" mục 5: quản trị chạy một đợt loại bỏ dữ liệu hết hạn sau khi khôi phục bản sao
 * lưu, bằng {@code eldercare.jobs.purge-expired-data.run-once=true} lúc mở ứng dụng (backend/docs/operations.md mục 5).
 */
class ExpiredDataPurgeJobTest {

    private final ExpiredDataPurge purge = mock(ExpiredDataPurge.class);

    @Test
    void runOnce_runsOneBatchAtStartup() {
        new ExpiredDataPurgeJob(purge, true).atStartup();

        verify(purge).run();
    }

    @Test
    void default_doesNotRunAtStartup() {
        new ExpiredDataPurgeJob(purge, false).atStartup();

        verify(purge, never()).run();
    }
}
