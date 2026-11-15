package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.session.service.FamilyActivityReadService.FamilyActivities;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** api-014 {@code FamilyActivities} (FR-068; chủ dự án duyệt 2026-11-10): loại thông tin "chung" của cổng người thân. */
@Schema(name = "FamilyActivities")
public record FamilyActivitiesResponse(
        LocalDate from,
        LocalDate to,
        List<RegisteredSession> registeredSessions,
        List<Trip> trips,
        Integer attendedSessionCount) {

    @Schema(name = "FamilyRegisteredSession")
    public record RegisteredSession(
            Long sessionId,
            String activityName,
            Instant startsAt,
            Instant endsAt,

            @Schema(allowableValues = {"REGISTERED", "ATTENDED"})
            String registrationStatus,

            Boolean lateReturnConsentEligible,
            Long activeLateReturnConsentId) {}

    @Schema(name = "FamilyTrip")
    public record Trip(
            Long sessionId,
            String destination,
            Instant departureAt,
            Instant expectedReturnAt,

            @Schema(allowableValues = {"SCHEDULED", "IN_PROGRESS", "RETURNED", "CANCELLED"})
            String status) {}

    public static FamilyActivitiesResponse of(FamilyActivities a) {
        return new FamilyActivitiesResponse(
                a.from(),
                a.to(),
                a.registeredSessions().stream()
                        .map(s -> new RegisteredSession(
                                s.sessionId(),
                                s.activityName(),
                                s.startsAt(),
                                s.endsAt(),
                                s.registrationStatus(),
                                s.lateReturnConsentEligible(),
                                s.activeLateReturnConsentId()))
                        .toList(),
                a.trips().stream()
                        .map(t -> new Trip(
                                t.sessionId(), t.destination(), t.departureAt(), t.expectedReturnAt(), t.status()))
                        .toList(),
                a.attendedSessionCount());
    }
}
