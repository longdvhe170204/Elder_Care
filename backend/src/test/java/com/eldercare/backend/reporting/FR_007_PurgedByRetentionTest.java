package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogRecord;
import com.eldercare.backend.reporting.internal.metric.service.PurgedDataTypes;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 016 T109 (FR-007, edge case "Dữ liệu đã bị loại bỏ theo NFR-07"; chủ dự án duyệt {@code purgedDataTypes} 2026-11-10):
 * khoảng báo cáo giao với dữ liệu đã loại bỏ theo bản tóm tắt {@code EXPIRED_DATA_PURGED} của 000 FR-051 thì chỉ tiêu đọc
 * loại dữ liệu đó mang nhãn và tên loại dữ liệu.
 */
class FR_007_PurgedByRetentionTest {

    private static final Instant JAN_2020 = Instant.parse("2020-01-01T00:00:00Z");
    private static final Instant JUN_2020 = Instant.parse("2020-06-01T00:00:00Z");

    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final PurgedDataTypes purged = new PurgedDataTypes(audit);

    @Test
    void FR_007_periodStartingBeforeLatestPurgedPoint_listsOnlyTypesOfTheMetricsModule() {
        when(audit.recorded(AuditAction.EXPIRED_DATA_PURGED, "M15.PURGE_RUN", null, null))
                .thenReturn(List.of(run(Map.of(
                        "M07.DOSE", range(3, JAN_2020, JUN_2020),
                        "M04.TASK", range(2, JAN_2020, JAN_2020),
                        "M07.EMPTY", range(0, JAN_2020, JUN_2020)))));

        assertThat(purged.affecting("006 MedicationStatisticsApi.doses", Instant.parse("2020-03-01T00:00:00Z")))
                .containsExactly("M07.DOSE");
        assertThat(purged.affecting("005 TaskStatisticsApi.inRange", Instant.parse("2020-03-01T00:00:00Z")))
                .as("M04.TASK chỉ tới 01/01/2020, khoảng bắt đầu sau đó")
                .isEmpty();
        assertThat(purged.affecting("005 TaskStatisticsApi.inRange", Instant.parse("2019-12-01T00:00:00Z")))
                .containsExactly("M04.TASK");
        assertThat(purged.affecting("006 MedicationStatisticsApi.doses", Instant.parse("2021-01-01T00:00:00Z")))
                .isEmpty();
    }

    @Test
    void FR_007_noPurgeRunOrUnknownSource_noLabel() {
        when(audit.recorded(AuditAction.EXPIRED_DATA_PURGED, "M15.PURGE_RUN", null, null))
                .thenReturn(List.of());
        assertThat(purged.affecting("006 MedicationStatisticsApi.doses", JAN_2020))
                .isEmpty();
        assertThat(purged.affecting("nguồn lạ", JAN_2020)).isEmpty();
    }

    private static AuditLogRecord run(Map<String, Object> deleted) {
        return new AuditLogRecord(1L, JUN_2020, null, "SCHEDULER", "run-1", null, Map.of("deleted", deleted));
    }

    private static Map<String, Object> range(int count, Instant from, Instant to) {
        return Map.of("count", count, "from", from.toString(), "to", to.toString());
    }
}
