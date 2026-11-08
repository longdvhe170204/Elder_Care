package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.TaskSourceApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.MeasurementScheduleApi;
import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import com.eldercare.backend.health.internal.threshold.service.ThresholdIndicators;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lịch đo trên MySQL (V007_09): lưu và đọc ba kiểu tần suất, thay đổi phát nguồn đã đổi, ngừng phát nguồn bị hủy, lịch
 * không sửa được, không có dòng công việc nào do 007 tạo (FR-003 → FR-006; tasks T020). 005 và 001 dùng bean giả;
 * lệnh chạy với một Bác sĩ thật qua kiểm quyền.
 */
@Transactional
@Import(SwitchableUserConfig.class)
class MeasurementScheduleIT extends MySqlIntegrationTestBase {
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");

    @MockitoBean
    TaskSourceApi taskSources;

    @MockitoBean
    ResidentApi residents;

    @MockitoBean
    ThresholdIndicators indicators;

    @Autowired
    MeasurementScheduleStore store;

    @Autowired
    MeasurementScheduleApi api;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SwitchableUser current;

    @Autowired
    BusinessTime time;

    private static NewMeasurementSchedule schedule(ScheduleFrequency frequency, Instant start) {
        return new NewMeasurementSchedule(Set.of(3L, 4L), frequency, start, null, "MANDATORY", "Theo dõi");
    }

    private long insert(long resident, NewMeasurementSchedule content, String status, Long replaces) {
        return store.insert(
                new MeasurementSchedule(0, resident, content, status, replaces, null, null, 0),
                TestUsers.nextId(),
                NOW);
    }

    @Test
    void FR_003_threeFrequencyTypesRoundTripAndGiveOccurrences() {
        long resident = TestUsers.nextId();
        var clock = new ScheduleFrequency(
                MeasurementFrequency.CLOCK_TIMES, List.of(LocalTime.of(6, 0)), null, null, Set.of());
        var every = new ScheduleFrequency(MeasurementFrequency.EVERY_N_HOURS, List.of(), 6, NOW, Set.of());
        var weekly = new ScheduleFrequency(
                MeasurementFrequency.WEEKDAYS, List.of(LocalTime.of(8, 0)), null, null, Set.of(3));
        long a = insert(resident, schedule(clock, NOW.minus(Duration.ofDays(1))), "EFFECTIVE", null);
        long b = insert(resident, schedule(every, NOW), "EFFECTIVE", null);
        long c = insert(resident, schedule(weekly, NOW.plus(Duration.ofDays(1))), "PENDING", null);

        assertThat(store.find(a).orElseThrow().content()).isEqualTo(schedule(clock, NOW.minus(Duration.ofDays(1))));
        assertThat(store.find(b).orElseThrow().content().frequency().everyHours())
                .isEqualTo(6);
        assertThat(store.find(c).orElseThrow().content().frequency().weekdays()).containsExactly(3);
        assertThat(api.effectiveAt(resident, NOW).stream().map(v -> v.id())).containsExactlyInAnyOrder(a, b);
        assertThat(api.occurrences(b, NOW, NOW.plus(Duration.ofHours(12))))
                .containsExactly(NOW, NOW.plus(Duration.ofHours(6)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM monitoring_setting WHERE kind = 'MEASUREMENT_SCHEDULE' AND resident_id = ?",
                        Integer.class,
                        resident))
                .isEqualTo(3);
    }
}
