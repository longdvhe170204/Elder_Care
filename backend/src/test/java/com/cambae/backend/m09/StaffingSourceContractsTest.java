package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.AttendanceScheduleView;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.events.*;
import com.cambae.backend.support.fake.m02.FakeContractSchedules;
import com.cambae.backend.support.fake.m03.FakeLocations;
import java.time.*;
import org.junit.jupiter.api.Test;

class StaffingSourceContractsTest {
    @Test
    void FR_037a_sharedStatisticsFakePreservesFloorManagerHistoryWithoutShift() {
        var at = Instant.parse("2026-10-25T00:00:00Z");
        var row = new com.cambae.backend.m09.api.dto.AssignmentHistoryView(
                1, 2, null, "FLOOR", 3, at, null, "FLOOR_MANAGER");
        var fake = new com.cambae.backend.support.fake.m09.FakeStaffStatistics().history(java.util.List.of(row));
        assertThat(fake.assignmentHistory(
                        com.cambae.backend.m15.account.api.dto.ScopeFilter.FACILITY, at, at.plusSeconds(1)))
                .singleElement()
                .satisfies(r -> assertThat(r.shiftId()).isNull());
    }

    @Test
    void FR_034_sharedAttendanceFakeDistinguishesEmptyAndUnavailableSource() {
        var fake = new FakeContractSchedules();
        ContractReadApi api = fake;
        var day = LocalDate.of(2026, 10, 25);
        assertThat(api.attendanceOn(1, day)).isEmpty();
        var schedule = new AttendanceScheduleView(day, LocalTime.of(7, 30), LocalTime.of(16, 30));
        fake.schedule(1, schedule);
        assertThat(api.attendanceOn(1, day)).containsExactly(schedule);
        assertThat(api.attendanceOn(1, day.plusDays(1))).isEmpty();
        fake.available(false);
        assertThatThrownBy(() -> api.attendanceOn(1, day)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void FR_035_allocationEventsPreserveResidentOldAndNewFloorsAndEffectiveInstant() {
        var at = Instant.parse("2026-10-25T00:00:00Z");
        assertThat(new AllocationStarted(1, 2, 3, at).residentId()).isEqualTo(2);
        assertThat(new AllocationClosed(1, 2, 3, at, "Kết thúc").at()).isEqualTo(at);
        var moved = new BedChanged(2, 10, 11, 20L, 21L, at);
        assertThat(moved.previousFloorId()).isEqualTo(20);
        assertThat(moved.newFloorId()).isEqualTo(21);
        var locations = new FakeLocations();
        assertThat(locations.dayCareRestAreaLocation()).isEmpty();
        locations.daycareRestLocation(new LocationRef("FLOOR", 20));
        assertThat(locations.dayCareRestAreaLocation()).contains(new LocationRef("FLOOR", 20));
        locations.floor(20, null).room(22, 20).stay(2, 22, at.minusSeconds(3600), null);
        locations.closeStays(2, at);
        assertThat(locations.at(2, at.minusSeconds(1))).isPresent();
        assertThat(locations.at(2, at)).isEmpty();
    }
}
