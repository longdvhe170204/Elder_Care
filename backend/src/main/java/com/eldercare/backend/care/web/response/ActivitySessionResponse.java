package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.internal.session.domain.ListedSession;
import java.time.Instant;

/** 014 T009: schema {@code ActivitySession} của api-014; chuyến đi (US4) chưa có nên {@code isTrip} luôn sai. */
public record ActivitySessionResponse(
        long id,
        long activityId,
        Instant startsAt,
        Instant endsAt,
        String status,
        int registrationCount,
        boolean isTrip) {
    public static ActivitySessionResponse of(ListedSession row) {
        var s = row.session();
        return new ActivitySessionResponse(
                s.id(), s.activityId(), s.startsAt(), s.endsAt(), s.status(), row.registrationCount(), false);
    }
}
