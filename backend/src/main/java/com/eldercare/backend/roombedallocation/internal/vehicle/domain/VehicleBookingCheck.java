package com.eldercare.backend.roombedallocation.internal.vehicle.domain;

import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 019 T022 (FR-017, BR-M03-16, Q-236; research W-07 {@code KiemTraDatLich}): kiểm đặt lịch xe, hàm thuần. Xe Hỏng, Đang
 * bảo trì, Đã thanh lý xét theo hiện tại; đăng kiểm, bảo hiểm xét theo ngày đi: đang mang dấu quá hạn hoặc hạn trước
 * ngày đi (chủ dự án chốt 2026-11-06); chồng giờ với lịch chưa hủy khác của cùng xe.
 */
public final class VehicleBookingCheck {
    private VehicleBookingCheck() {}

    public record Vehicle(
            AssetStatus status,
            LocalDate inspectionDueOn,
            LocalDate insuranceDueOn,
            boolean inspectionOverdue,
            boolean insuranceOverdue) {}

    /** Lịch chưa hủy khác của cùng xe, khoảng [from, to). */
    public record Slot(long id, Instant from, Instant to) {}

    public static List<UnmetCondition> check(Vehicle vehicle, List<Slot> others, Instant from, Instant to) {
        var unmet = new ArrayList<UnmetCondition>();
        if (Set.of(AssetStatus.DAMAGED, AssetStatus.UNDER_MAINTENANCE, AssetStatus.DISPOSED)
                .contains(vehicle.status()))
            unmet.add(new UnmetCondition("VEHICLE_NOT_AVAILABLE", "Xe đang Hỏng, Đang bảo trì hoặc Đã thanh lý"));
        var day = LocalDate.ofInstant(from, BusinessTime.ZONE);
        boolean overdue = vehicle.inspectionOverdue()
                || vehicle.insuranceOverdue()
                || (vehicle.inspectionDueOn() != null && day.isAfter(vehicle.inspectionDueOn()))
                || (vehicle.insuranceDueOn() != null && day.isAfter(vehicle.insuranceDueOn()));
        if (overdue)
            unmet.add(new UnmetCondition(
                    "VEHICLE_INSPECTION_OR_INSURANCE_OVERDUE", "Xe quá hạn đăng kiểm hoặc bảo hiểm tại ngày đi"));
        if (!overlaps(others, from, to).isEmpty())
            unmet.add(new UnmetCondition("VEHICLE_SCHEDULE_OVERLAP", "Chồng giờ với lịch khác của cùng xe"));
        return List.copyOf(unmet);
    }

    /** Lịch chồng giờ với [from, to); dùng cả khi gia hạn để báo, không chặn (Q-236). */
    public static List<Slot> overlaps(List<Slot> others, Instant from, Instant to) {
        return others.stream()
                .filter(s -> s.from().isBefore(to) && from.isBefore(s.to()))
                .toList();
    }
}
