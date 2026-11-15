package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.roombedallocation.api.events.VehicleScheduleCorrected;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 nghe đính chính lịch xe {@code M03.VEHICLE_SCHEDULE} của 019 (chủ dự án chốt 2026-11-09): chỉ chuyến Đã về, đổi
 * giờ rời, về dự kiến hiện hành; giữ nguyên tác động đã xảy ra (001, 005, đồng ý về muộn, mốc quá giờ đã báo, lịch sử
 * gia hạn), không phát {@code TripChanged}, không là đính chính của 014. Đồng bộ trong lệnh của 019: chuyến không có
 * (404) hoặc không ở Đã về (409) thì cả lệnh đính chính rollback. Cùng giá trị thì không ghi.
 */
@Component
class VehicleScheduleCorrectionListener {
    private final ActivityTripStore trips;

    VehicleScheduleCorrectionListener(ActivityTripStore trips) {
        this.trips = trips;
    }

    @EventListener
    @Transactional
    void on(VehicleScheduleCorrected event) {
        var trip = trips.lock(event.tripId());
        if (!"RETURNED".equals(trip.status()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "STATE_CHANGED", "Chỉ đồng bộ giờ lịch xe đã đính chính cho chuyến Đã về");
        var plan = trip.plan();
        if (plan.plannedDepartureAt().equals(event.plannedDepartureAt())
                && plan.plannedReturnAt().equals(event.plannedReturnAt())) return;
        trips.correctPlannedTimes(trip.id(), event.plannedDepartureAt(), event.plannedReturnAt());
    }
}
