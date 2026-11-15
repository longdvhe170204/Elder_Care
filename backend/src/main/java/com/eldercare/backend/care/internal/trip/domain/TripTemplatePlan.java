package com.eldercare.backend.care.internal.trip.domain;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;

/** Một bộ giờ địa phương dùng cho mọi ngày tạo buổi; chủ dự án duyệt 08/11/2026. */
public record TripTemplatePlan(
        String destination,
        String plannedDepartureTime,
        String plannedReturnTime,
        String transport,
        boolean facilityVehicle) {
    public TripPlan on(LocalDate day) {
        if (plannedDepartureTime == null
                || plannedReturnTime == null
                || !plannedDepartureTime.matches("\\d{2}:\\d{2}")
                || !plannedReturnTime.matches("\\d{2}:\\d{2}"))
            throw new InvalidValueException("Giờ chuyến phải theo HH:mm");
        try {
            var from = LocalTime.parse(plannedDepartureTime);
            var to = LocalTime.parse(plannedReturnTime);
            if (!to.isAfter(from)) throw new InvalidValueException("Giờ về phải sau giờ rời trong cùng ngày");
            var plan = new TripPlan(
                    destination,
                    day.atTime(from).atZone(BusinessTime.ZONE).toInstant(),
                    day.atTime(to).atZone(BusinessTime.ZONE).toInstant(),
                    transport,
                    facilityVehicle);
            plan.validate();
            return plan;
        } catch (DateTimeException e) {
            throw new InvalidValueException("Giờ chuyến phải theo HH:mm hợp lệ");
        }
    }
}
