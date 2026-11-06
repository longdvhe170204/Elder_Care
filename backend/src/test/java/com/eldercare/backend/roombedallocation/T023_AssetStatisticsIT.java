package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.AssetStatisticsApi;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.VehicleScheduleStatus;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

/** 019 T023, contracts mục 2.3: dấu hiện tại theo ngày và tầng; lịch xe theo ngày nghiệp vụ, toàn viện. */
class T023_AssetStatisticsIT extends VehicleTestBase {
    @Autowired
    ObjectProvider<AssetStatisticsApi> providers;

    private static final LocalDate DAY = LocalDate.of(2099, 1, 5);

    private AssetStatisticsApi statistics() {
        var api = providers.getIfAvailable();
        assertThat(api).as("T023 cần bản cài AssetStatisticsApi thật").isNotNull();
        return api;
    }

    @Test
    void T023_overdueUsesCurrentFlagsStrictDueDateAndFloorScope() {
        jdbc.update(
                "UPDATE asset SET floor_area_id=?, maintenance_overdue=TRUE, maintenance_due_on=?,"
                        + "vehicle_inspection_overdue=TRUE, vehicle_inspection_due_on=?, insurance_due_on=? WHERE id=?",
                floor,
                DAY.minusDays(1),
                DAY,
                DAY.minusDays(1),
                vehicle);
        long elsewhere = vehicle(DAY.minusDays(1), null);
        jdbc.update(
                "UPDATE asset SET vehicle_inspection_overdue=TRUE, floor_area_id=? WHERE id=?", otherFloor, elsewhere);
        var scope = new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
        try (var rows = statistics().overdue(scope, DAY)) {
            assertThat(rows.toList()).singleElement().satisfies(row -> {
                assertThat(row.assetId()).isEqualTo(vehicle);
                assertThat(row.code()).isEqualTo("XE-" + vehicle);
                assertThat(row.departmentId()).isEqualTo(fleet);
                assertThat(row.floorAreaId()).isEqualTo(floor);
                assertThat(row.flag()).isEqualTo(AssetFlag.MAINTENANCE_OVERDUE);
                assertThat(row.dueOn()).isEqualTo(DAY.minusDays(1));
            });
        }
        assertThat(statistics().countOverdue(new ScopeFilter(false, Set.of(), Set.of(), Set.of()), DAY))
                .isEmpty();
    }

    @Test
    void T023_countGroupsEachCurrentFlagAndFacilityScopeIncludesVehiclesWithoutFloor() {
        jdbc.update(
                "UPDATE asset SET floor_area_id=?, maintenance_overdue=TRUE, maintenance_due_on=?,"
                        + "vehicle_inspection_overdue=TRUE, vehicle_inspection_due_on=?, insurance_overdue=TRUE,"
                        + "insurance_due_on=? WHERE id=?",
                floor,
                DAY.minusDays(1),
                DAY.minusDays(1),
                DAY.minusDays(1),
                vehicle);
        long second = vehicle(null, null);
        jdbc.update(
                "UPDATE asset SET floor_area_id=?, maintenance_overdue=TRUE, maintenance_due_on=? WHERE id=?",
                floor,
                DAY.minusDays(1),
                second);
        var scope = new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
        assertThat(statistics().countOverdue(scope, DAY))
                .isEqualTo(Map.of(
                        AssetFlag.MAINTENANCE_OVERDUE,
                        2L,
                        AssetFlag.VEHICLE_INSPECTION_OVERDUE,
                        1L,
                        AssetFlag.INSURANCE_OVERDUE,
                        1L));
        jdbc.update("UPDATE asset SET floor_area_id=NULL WHERE id=?", vehicle);
        try (var rows = statistics().overdue(ScopeFilter.FACILITY, DAY)) {
            assertThat(rows.filter(r -> r.assetId() == vehicle).toList())
                    .hasSize(3)
                    .allSatisfy(r -> assertThat(r.floorAreaId()).isNull());
        }
        assertThat(statistics().countOverdue(scope, DAY))
                .containsExactlyEntriesOf(Map.of(AssetFlag.MAINTENANCE_OVERDUE, 1L));
    }

    @Test
    void T023_vehicleSchedulesUseInclusiveBusinessDatesAndIncludeCancelledRecords() throws Exception {
        var start = DAY.atStartOfDay(BusinessTime.ZONE).toInstant();
        var end = DAY.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        long before = booked(booking(vehicle, start.minusSeconds(3600), start, "OTHER", null));
        long first = booked(booking(vehicle, start, start.plusSeconds(3600), "OTHER", null));
        long last = booked(booking(
                vehicle, end.minusSeconds(3600), end, "TRIP", com.eldercare.backend.support.TestUsers.nextId()));
        long after = booked(booking(vehicle, end, end.plusSeconds(3600), "OTHER", null));
        schedule(first, "cancel", "{\"reason\":\"Đổi kế hoạch\"}").andExpect(status().isOk());
        try (var rows = statistics().vehicleSchedules(DAY, DAY)) {
            var own = rows.filter(r -> r.vehicleId() == vehicle).toList();
            assertThat(own)
                    .extracting(r -> r.scheduleId())
                    .containsExactlyInAnyOrder(first, last)
                    .doesNotContain(before, after);
            assertThat(own)
                    .filteredOn(r -> r.scheduleId() == first)
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.status()).isEqualTo(VehicleScheduleStatus.CANCELLED);
                        assertThat(r.plannedDepartureAt()).isEqualTo(start);
                        assertThat(r.departedAt()).isNull();
                        assertThat(r.returnedAt()).isNull();
                    });
        }
    }

    @Autowired
    com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi commands;

    @Test
    void T023_vehicleScheduleViewsIncludeActualTimesInUseAndAfterCompletion() throws Exception {
        long trip = com.eldercare.backend.support.TestUsers.nextId();
        long id = booked(booking(vehicle, TEN, ELEVEN, "TRIP", trip));
        var departed = TEN.plusSeconds(60);
        var returned = ELEVEN.plusSeconds(120);
        commands.followTrip(
                trip,
                com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType.FIRST_DEPARTURE,
                TEN,
                ELEVEN,
                java.util.Optional.of(departed));
        try (var rows = statistics().vehicleSchedules(DAY, DAY)) {
            assertThat(rows.filter(r -> r.scheduleId() == id).toList())
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.status()).isEqualTo(VehicleScheduleStatus.IN_USE);
                        assertThat(r.tripId()).isEqualTo(trip);
                        assertThat(r.departedAt()).isEqualTo(departed);
                        assertThat(r.returnedAt()).isNull();
                    });
        }
        commands.followTrip(
                trip,
                com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType.RETURN_FINISHED,
                TEN,
                ELEVEN,
                java.util.Optional.of(returned));
        try (var rows = statistics().vehicleSchedules(DAY, DAY)) {
            assertThat(rows.filter(r -> r.scheduleId() == id).toList())
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.status()).isEqualTo(VehicleScheduleStatus.COMPLETED);
                        assertThat(r.departedAt()).isEqualTo(departed);
                        assertThat(r.returnedAt()).isEqualTo(returned);
                    });
        }
    }
}
