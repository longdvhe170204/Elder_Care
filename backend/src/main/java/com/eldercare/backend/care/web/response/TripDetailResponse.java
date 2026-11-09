package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.trip.service.TripPreparationService;
import java.time.Instant;
import java.util.List;

/** FR-039: phần dữ liệu chuyến đã có hợp đồng; nguồn lâm sàng bổ sung sau khi được duyệt. */
public record TripDetailResponse(
        long id,

        @io.swagger.v3.oas.annotations.media.Schema(
                allowableValues = {"SCHEDULED", "IN_PROGRESS", "RETURNED", "CANCELLED"})
        String status,

        int version,
        String destination,
        Instant plannedDepartureAt,
        Instant plannedReturnAt,
        String transport,
        boolean facilityVehicle,
        Long leaderId,
        List<Long> companionIds,
        List<Participant> participants) {
    public record Participant(
            long id,
            long residentId,

            @io.swagger.v3.oas.annotations.media.Schema(
                    allowableValues = {
                        "REGISTERED",
                        "ELIGIBLE",
                        "NOT_GOING",
                        "DEPARTED",
                        "RETURNED",
                        "LEFT_GROUP",
                        "MISSING_ON_RETURN"
                    })
            String status,

            String reason) {}

    public static TripDetailResponse of(TripPreparationService.Detail detail) {
        var trip = detail.trip();
        var plan = trip.plan();
        return new TripDetailResponse(
                trip.id(),
                trip.status(),
                trip.version(),
                plan.destination(),
                plan.plannedDepartureAt(),
                plan.plannedReturnAt(),
                plan.transport(),
                plan.facilityVehicle(),
                detail.duties().leaderId(),
                detail.duties().companionIds(),
                detail.participants().stream()
                        .map(p -> new Participant(p.registrationId(), p.residentId(), p.status(), p.reason()))
                        .toList());
    }
}
