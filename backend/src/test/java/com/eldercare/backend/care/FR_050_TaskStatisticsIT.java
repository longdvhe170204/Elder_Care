package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.dto.ResultType;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.attendance.domain.AttendanceData;
import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import java.time.LocalTime;
import java.util.Set;
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

    @Test
    void FR_050_scopeFilterLimitsTasksToFloorOrResident() throws Exception {
        long id = create();
        var from = planned.minusSeconds(60);
        var to = planned.plusSeconds(60);
        assertThat(api().inRange(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), from, to))
                .extracting(r -> r.id())
                .contains(id);
        assertThat(api().inRange(new ScopeFilter(false, Set.of(floor + 1), Set.of(), Set.of()), from, to))
                .extracting(r -> r.id())
                .doesNotContain(id);
        assertThat(api().inRange(new ScopeFilter(false, Set.of(), Set.of(), Set.of()), from, to))
                .isEmpty();
    }

    @Test
    void FR_050_attendanceInRangeFollowsScope() {
        var date = time.today();
        context.getBean(AttendanceStore.class)
                .create(
                        residentId,
                        date,
                        AttendanceStatus.PRESENT,
                        new AttendanceData(
                                floor,
                                LocalTime.of(7, 30),
                                LocalTime.of(16, 30),
                                LocalTime.of(16, 30),
                                null,
                                null,
                                null,
                                time.now(),
                                null,
                                null,
                                false,
                                null,
                                null),
                        manager.accountId());

        assertThat(api().attendanceInRange(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), date, date))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.residentId()).isEqualTo(residentId);
                    assertThat(a.status()).isEqualTo(AttendanceStatus.PRESENT);
                });
        assertThat(api().attendanceInRange(new ScopeFilter(false, Set.of(floor + 1), Set.of(), Set.of()), date, date))
                .isEmpty();
    }

    @Test
    void FR_050_resultsAndRecordersComeFromRecordedResults() throws Exception {
        long id = create();
        jdbc.update(
                "UPDATE care_task SET status='COMPLETED',closed_at=?,result_type='PERFORMED',result_performed_at=?,result_recorded_at=?,result_recorded_by=?,result_permission_basis='ASSIGNED',result_values='{}' WHERE id=?",
                java.time.LocalDateTime.ofInstant(time.now(), com.eldercare.backend.common.time.BusinessTime.ZONE),
                java.time.LocalDateTime.ofInstant(
                        time.now().minusSeconds(60), com.eldercare.backend.common.time.BusinessTime.ZONE),
                java.time.LocalDateTime.ofInstant(time.now(), com.eldercare.backend.common.time.BusinessTime.ZONE),
                caregiver.accountId(),
                id);

        assertThat(api().resultsInRange(
                                residentId,
                                Set.of(ResultType.PERFORMED),
                                time.now().minusSeconds(3600),
                                time.now().plusSeconds(3600)))
                .singleElement()
                .satisfies(r -> {
                    assertThat(r.taskId()).contains(id);
                    assertThat(r.recordedById()).isEqualTo(caregiver.accountId());
                });
        assertThat(api().resultsInRange(
                                residentId,
                                Set.of(ResultType.SLEEP),
                                time.now().minusSeconds(3600),
                                time.now().plusSeconds(3600)))
                .isEmpty();
        assertThat(api().resultRecorders(
                                residentId,
                                time.now().minusSeconds(3600),
                                time.now().plusSeconds(3600)))
                .containsExactly(caregiver.accountId());
    }
}
