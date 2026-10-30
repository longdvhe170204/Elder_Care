package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.staffing.api.ShiftApi;
import com.eldercare.backend.staffing.api.StaffStatisticsApi;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.staffing.api.events.SchedulePublished;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import com.eldercare.backend.staffing.api.events.StaffAbsent;
import com.eldercare.backend.support.fake.staffing.FakeM09Config;
import com.eldercare.backend.support.fake.staffing.FakeShifts;
import com.eldercare.backend.support.fake.staffing.FakeStaffStatistics;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** T003: hợp đồng đọc và bản giả dùng chung, chưa cài tra cứu ca thực tế của T015. */
class M09PublicApiTest {

    private static final Instant START = Instant.parse("2026-10-23T00:00:00Z");
    private static final Instant END = START.plusSeconds(3600);

    @Test
    void FR_049_shiftFakeFiltersStaffScopeAndHalfOpenTimeRange() {
        var fake = new FakeShifts();
        var view = new ShiftView(1L, 100L, START, END, "IN_PROGRESS", 10L);
        fake.shift(view, List.of(10L, 20L));

        assertThat(fake.currentShift(100L, START)).contains(view);
        assertThat(fake.currentShift(200L, START)).isEmpty();
        assertThat(fake.currentShift(100L, END)).isEmpty();
        assertThat(fake.staffShifts(10L, START.minusSeconds(1), END)).containsExactly(view);
        assertThat(fake.staffShifts(30L, START, END)).isEmpty();
        assertThat(fake.staffShifts(10L, END, END.plusSeconds(1))).isEmpty();
        assertThat(fake.onShift(20L, START)).isTrue();
        assertThat(fake.onShift(20L, END)).isFalse();
    }

    @Test
    void FR_049_fakesAreAvailableThroughTheSharedConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(FakeM09Config.class)
                .run(context -> {
                    assertThat(context.getBean(ShiftApi.class)).isInstanceOf(FakeShifts.class);
                    assertThat(context.getBean(StaffStatisticsApi.class)).isInstanceOf(FakeStaffStatistics.class);
                });
    }

    @Test
    void FR_054_publishedEventCopiesStaffAndCarriesNotificationRequests() {
        var ids = new ArrayList<>(List.of(10L));
        var event = new SchedulePublished(1L, ids, START, List.of());
        ids.add(20L);

        assertThat(event.staffIds()).containsExactly(10L);
        assertThat(event).isInstanceOf(HasNotificationRequests.class);
        assertThat(event.notificationRequests()).isEmpty();
    }

    @Test
    void FR_053_absenceEventCannotCarryPrivateReasons() {
        assertThat(StaffAbsent.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("reason", "absenceReason", "terminationReason");
        assertThat(ShiftEnded.class).isRecord();
    }
}
