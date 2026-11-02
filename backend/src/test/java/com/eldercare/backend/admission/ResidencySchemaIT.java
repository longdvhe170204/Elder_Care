package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import com.eldercare.backend.admission.internal.persistence.ResidencyStateStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 004 T005, T110 (data-model mục 0, 2, 11; FR-072, FR-077, BR-M02-19; R-15): trạng thái theo người ở {@code resident_module_state}, dấu
 * chạy và dấu nhắc ở {@code reminder_marker}: dấu "đã đóng" đặt một lần, mỗi việc mỗi ngày một dấu, mỗi loại nhắc mỗi ngày một dấu.
 */
class ResidencySchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    ResidencyStateStore state;

    @Autowired
    M02RunStore runs;

    @Autowired
    TransactionTemplate transactions;

    private static long nextResident() {
        return 900_000_000L + System.nanoTime() % 90_000_000L;
    }

    @Test
    void FR_072_closedAt_isSetOnce_andNeverRemoved() {
        long resident = nextResident();
        Instant first = Instant.parse("2031-03-04T01:00:00Z");

        transactions.executeWithoutResult(
                status -> assertThat(state.close(resident, first)).isTrue());
        transactions.executeWithoutResult(status ->
                assertThat(state.close(resident, first.plusSeconds(60))).isFalse());

        assertThat(state.closedAt(resident)).contains(first);
    }

    @Test
    void FR_074_admittedOn_isSetOnce() {
        long resident = nextResident();

        transactions.executeWithoutResult(status -> state.recordAdmittedOn(resident, LocalDate.of(2031, 3, 4)));
        transactions.executeWithoutResult(status -> state.recordAdmittedOn(resident, LocalDate.of(2031, 3, 5)));

        assertThat(state.admittedOn(resident)).contains(LocalDate.of(2031, 3, 4));
    }

    @Test
    void FR_077_dailyRun_eachTaskIsClaimedOncePerDay() {
        String task = "TEST_SCHEMA_" + nextResident();
        LocalDate day = LocalDate.of(2031, 3, 4);

        transactions.executeWithoutResult(status -> {
            assertThat(runs.claimDay(task, day, "SUCCEEDED", "run-1")).isTrue();
            assertThat(runs.claimDay(task, day, "SUCCEEDED", "run-2")).isFalse();
            assertThat(runs.claimDay(task, day.plusDays(1), "SKIPPED", "run-2")).isTrue();
        });

        assertThat(runs.lastDay(task)).contains(day.plusDays(1));
    }

    @Test
    void FR_072_reminder_isMarkedOncePerDayForTypeAndSubject() {
        long subject = nextResident();
        LocalDate day = LocalDate.of(2031, 3, 4);

        transactions.executeWithoutResult(status -> {
            assertThat(runs.markReminded("POST_DEATH_TASKS", subject, day)).isTrue();
            assertThat(runs.markReminded("POST_DEATH_TASKS", subject, day)).isFalse();
            assertThat(runs.markReminded("CONTRACT_OVERDUE", subject, day)).isTrue();
        });

        assertThat(runs.lastReminded("POST_DEATH_TASKS", subject)).contains(day);
    }
}
