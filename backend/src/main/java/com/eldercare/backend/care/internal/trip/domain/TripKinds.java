package com.eldercare.backend.care.internal.trip.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/** Loại bảng chung của T016, R-15; bản ghi đã xác nhận không sửa/xóa. */
public final class TripKinds {
    private TripKinds() {}

    public static final HistoryTrack<TripDuties> DUTIES =
            new HistoryTrack<>("M04.ACTIVITY_SESSION", "TRIP_DUTIES", TripDuties.class);
    public static final SharedKind<Assessment> ASSESSMENTS = SharedKind.of("M04.TRIP_ELIGIBILITY", Assessment.class);
    public static final SharedKind<Attendance> ATTENDANCE = SharedKind.of("M04.TRIP_ATTENDANCE", Attendance.class);

    public record Assessment(long tripId, long registrationId, long residentId, String result, String reason) {}

    public record Attendance(
            long tripId, long registrationId, long residentId, String type, Instant at, String substituteReason) {}
}
