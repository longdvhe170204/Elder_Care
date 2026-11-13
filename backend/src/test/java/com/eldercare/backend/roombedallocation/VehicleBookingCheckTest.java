package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleBookingCheck;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleBookingCheck.Slot;
import com.eldercare.backend.roombedallocation.internal.vehicle.domain.VehicleBookingCheck.Vehicle;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 019 T021 (FR-017, BR-M03-16, Q-236; chủ dự án chốt 2026-11-06 cách kiểm quá hạn theo ngày đi): kiểm đặt lịch xe. */
class VehicleBookingCheckTest {
    static final Instant SEVEN = Instant.parse("2026-10-06T00:00:00Z"); // 07:00 giờ Việt Nam
    static final Instant TEN = Instant.parse("2026-10-06T03:00:00Z");
    static final Instant ELEVEN = Instant.parse("2026-10-06T04:00:00Z");
    static final Instant THIRTEEN = Instant.parse("2026-10-06T06:00:00Z");
    static final Instant FOURTEEN = Instant.parse("2026-10-06T07:00:00Z");

    static Vehicle ready() {
        return new Vehicle(AssetStatus.READY, null, null, false, false);
    }

    static List<String> codes(List<UnmetCondition> unmet) {
        return unmet.stream().map(UnmetCondition::code).toList();
    }

    @Test
    void BR_M03_16_overlapWithAnUncancelledScheduleIsBlockedButAdjacentIsNot() {
        var c1 = new Slot(1, SEVEN, THIRTEEN);
        assertThat(codes(VehicleBookingCheck.check(ready(), List.of(c1), TEN, ELEVEN)))
                .containsExactly("VEHICLE_SCHEDULE_OVERLAP");
        assertThat(VehicleBookingCheck.check(ready(), List.of(c1), THIRTEEN, FOURTEEN))
                .isEmpty();
    }

    @Test
    void FR_017_damagedUnderMaintenanceOrDisposedVehicleIsNotAvailable() {
        for (var status : List.of(AssetStatus.DAMAGED, AssetStatus.UNDER_MAINTENANCE, AssetStatus.DISPOSED))
            assertThat(codes(VehicleBookingCheck.check(
                            new Vehicle(status, null, null, false, false), List.of(), TEN, ELEVEN)))
                    .as(status.name())
                    .containsExactly("VEHICLE_NOT_AVAILABLE");
        assertThat(VehicleBookingCheck.check(
                        new Vehicle(AssetStatus.IN_USE, null, null, false, false), List.of(), TEN, ELEVEN))
                .isEmpty();
    }

    @Test
    void FR_017_inspectionOrInsuranceOverdueAtTheDepartureDateIsBlocked() {
        var day = LocalDate.of(2026, 10, 6);
        assertThat(VehicleBookingCheck.check(
                        new Vehicle(AssetStatus.READY, day, day, false, false), List.of(), TEN, ELEVEN))
                .as("hạn đúng ngày đi chưa quá hạn")
                .isEmpty();
        assertThat(codes(VehicleBookingCheck.check(
                        new Vehicle(AssetStatus.READY, day.minusDays(1), null, false, false), List.of(), TEN, ELEVEN)))
                .containsExactly("VEHICLE_INSPECTION_OR_INSURANCE_OVERDUE");
        assertThat(codes(VehicleBookingCheck.check(
                        new Vehicle(AssetStatus.READY, null, day.minusDays(1), false, false), List.of(), TEN, ELEVEN)))
                .containsExactly("VEHICLE_INSPECTION_OR_INSURANCE_OVERDUE");
        assertThat(codes(VehicleBookingCheck.check(
                        new Vehicle(AssetStatus.READY, null, null, false, true), List.of(), TEN, ELEVEN)))
                .as("đang mang dấu quá hạn")
                .containsExactly("VEHICLE_INSPECTION_OR_INSURANCE_OVERDUE");
    }
}
