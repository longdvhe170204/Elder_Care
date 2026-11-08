package com.eldercare.backend.roombedallocation.internal.vehicle.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * 019 data-model mục 5 (R-15: {@code lich_xe} → {@code simple_record} loại {@code M03.VEHICLE_SCHEDULE}, đối tượng cha
 * là tài sản xe; glossary 25): nội dung một lịch xe. Trạng thái ở cột {@code status}: BOOKED, IN_USE, COMPLETED,
 * CANCELLED; hai trạng thái cuối khóa dòng. Lịch của chuyến đi có khóa duy nhất {@code TRIP:<id>} (tối đa một lịch chưa
 * hủy mỗi chuyến, chủ dự án chốt 2026-11-06), nhả khi hủy.
 */
public record VehicleSchedule(
        long vehicleId,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        String purpose,
        Long tripId,
        Long residentId,
        long driverStaffId,
        long bookedByStaffId,
        String note,
        Instant departedAt,
        Instant returnedAt,
        String cancellationReason) {

    public static final SharedKind<VehicleSchedule> KIND = SharedKind.of("M03.VEHICLE_SCHEDULE", VehicleSchedule.class);
    public static final String BOOKED = "BOOKED";
    public static final String IN_USE = "IN_USE";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    public static String tripKey(long tripId) {
        return "TRIP:" + tripId;
    }

    public VehicleSchedule withTimes(Instant from, Instant to) {
        return new VehicleSchedule(
                vehicleId,
                from,
                to,
                purpose,
                tripId,
                residentId,
                driverStaffId,
                bookedByStaffId,
                note,
                departedAt,
                returnedAt,
                cancellationReason);
    }

    public VehicleSchedule departed(Instant at) {
        return new VehicleSchedule(
                vehicleId,
                plannedDepartureAt,
                plannedReturnAt,
                purpose,
                tripId,
                residentId,
                driverStaffId,
                bookedByStaffId,
                note,
                at,
                returnedAt,
                cancellationReason);
    }

    public VehicleSchedule returned(Instant at) {
        return new VehicleSchedule(
                vehicleId,
                plannedDepartureAt,
                plannedReturnAt,
                purpose,
                tripId,
                residentId,
                driverStaffId,
                bookedByStaffId,
                note,
                departedAt,
                at,
                cancellationReason);
    }

    public VehicleSchedule cancelled(String reason) {
        return new VehicleSchedule(
                vehicleId,
                plannedDepartureAt,
                plannedReturnAt,
                purpose,
                tripId,
                residentId,
                driverStaffId,
                bookedByStaffId,
                note,
                departedAt,
                returnedAt,
                reason);
    }
}
