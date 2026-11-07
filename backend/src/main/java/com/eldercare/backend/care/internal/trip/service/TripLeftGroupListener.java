package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T020 (FR-045): người đang ở ngoài theo chuyến chuyển Điều trị tại bệnh viện (007) hoặc Qua đời (004) thì Rời đoàn,
 * trong cùng transaction lệnh trạng thái của 001; không cần điểm danh về, không tính thiếu người.
 */
@Component
public class TripLeftGroupListener {
    private final ActivityTripStore trips;

    public TripLeftGroupListener(ActivityTripStore trips) {
        this.trips = trips;
    }

    @EventListener
    @Transactional
    public void on(ResidentStatusChanged event) {
        if (event.from() != ResidentStatus.OFF_SITE_ACTIVITY) return;
        if (event.to() != ResidentStatus.HOSPITALIZED && event.to() != ResidentStatus.DECEASED) return;
        for (long registration : trips.outsideRegistrations(event.residentId()))
            trips.leftGroup(registration, event.to().name());
    }
}
