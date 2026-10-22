package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.cambae.backend.m15.account.api.events.LicenseExpiring;
import com.cambae.backend.m15.account.api.events.PermissionDataError;
import com.cambae.backend.m15.account.api.events.RecordPendingReview;
import com.cambae.backend.m15.account.api.events.RecordSyncedAfterPermissionLoss;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 002 T010: yêu cầu thông báo của bốn sự kiện 002 (contracts/module-interfaces.md mục 1.10) được M13 nhận là hợp lệ;
 * thiếu mức thì gắn dấu {@code MISSING_LEVEL} như 009 FR-003 (mức để 009 xếp, FR-043b).
 */
class M15AccountEventsIntakeIT extends M13IntegrationTestBase {

    private static final Instant AT = Instant.parse("2026-10-22T03:00:00Z");

    private void assertValid(List<Map<String, Object>> rows) {
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().get("intake_result")).isEqualTo("VALID");
        assertThat(rows.getFirst().get("flags")).isEqualTo("MISSING_LEVEL");
        assertThat(rows.getFirst().get("source_module")).isEqualTo("M15");
    }

    @Test
    void fourAccountEvents_areValidRequests() {
        long id = System.nanoTime();
        sourceCommand("002", new RecordSyncedAfterPermissionLoss("M15.SAMPLE", String.valueOf(id), 2L, AT));
        sourceCommand("002", new RecordPendingReview(id, null, AT));
        sourceCommand("002", new PermissionDataError(id, "M03.BED_ALLOCATION", 12L, "TWO_OPEN_ALLOCATIONS", AT));
        sourceCommand("002", new LicenseExpiring("FACILITY", id, LocalDate.of(2026, 11, 20), List.of(), List.of(), AT));

        assertValid(awaitRequests("RECORD_SYNCED_AFTER_PERMISSION_LOSS:M15.SAMPLE:" + id, 1));
        assertValid(awaitRequests("RECORD_PENDING_REVIEW:" + id, 1));
        assertValid(awaitRequests("PERMISSION_DATA_ERROR:" + id, 1));
        // Khóa sự kiện của giấy phép là ngày hết hạn, nên tìm theo bản ghi nguồn
        await().atMost(Duration.ofSeconds(10)).until(() -> !licenseRequests(id).isEmpty());
        assertValid(licenseRequests(id));
    }

    private List<Map<String, Object>> licenseRequests(long id) {
        return jdbc.queryForList(
                "SELECT * FROM notification_request WHERE source_record_type = ? AND source_record_id = ?",
                "M15.LICENSE_EXPIRY_WARNING",
                "FACILITY:" + id);
    }
}
