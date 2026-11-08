package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.TaskStatus;
import org.junit.jupiter.api.Test;

/** T059: số liệu cung cấp cho 016 (module-interfaces mục 1.9, FR-050 dòng "Gửi 016"); chỉ đọc. */
class FR_050_TaskStatisticsIT extends CareOverdueTestBase {
    TaskStatisticsApi api() {
        return context.getBean("careTaskStatistics", TaskStatisticsApi.class);
    }

    @Test
    void FR_050_tasksInRangeCarryStatusHistoryAndClosingRule() throws Exception {
        long id = create();
        context.getBean("careTaskHandover", com.eldercare.backend.care.api.ShiftTaskApi.class)
                .onAbsence(caregiver.staffId(), shiftId);
        jdbc.update(
                "UPDATE care_task SET status='CANCELLED',closure_rule='ABSENCE_CANCELLED',closed_at=NOW(6) WHERE id=?",
                id);

        var rows = api().inRange(ScopeFilter.FACILITY, planned.minusSeconds(60), planned.plusSeconds(60))
                .filter(r -> r.id() == id)
                .toList();

        assertThat(rows).singleElement().satisfies(r -> {
            assertThat(r.residentId()).contains(residentId);
            assertThat(r.floorAreaId()).isEqualTo(floor);
            assertThat(r.shiftId()).isEqualTo(shiftId);
            assertThat(r.status()).isEqualTo(TaskStatus.CANCELLED);
            assertThat(r.closingRule()).contains("ABSENCE_CANCELLED");
            assertThat(r.performerId()).isEmpty();
            assertThat(r.history()).isNotEmpty();
            assertThat(r.history().getFirst().status()).isEqualTo(TaskStatus.NOT_YET_DUE);
            assertThat(r.performedAt()).isEmpty();
            assertThat(r.recordedLate()).isFalse();
        });
    }
}
