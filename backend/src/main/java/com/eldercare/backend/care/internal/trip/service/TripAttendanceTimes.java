package com.eldercare.backend.care.internal.trip.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.care.internal.trip.domain.TripKinds;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Thời điểm hiện hành của bản ghi điểm danh chuyến (014 FR-031a (a)): bản ghi nhóm 3 không sửa, thời điểm đã đính chính lấy
 * từ bản đính chính mới nhất ({@code CorrectionApi.current}, FR-030).
 */
@Component
class TripAttendanceTimes {
    static final String AT = "at";

    private final ActivityTripStore trips;
    private final RecordEventApi records;
    // Lấy lúc dùng: CorrectionService cần mọi CorrectableRecordProvider, trong đó có provider dùng lớp này.
    private final ObjectProvider<CorrectionApi> corrections;
    private final JsonMapper json;

    TripAttendanceTimes(
            ActivityTripStore trips,
            RecordEventApi records,
            ObjectProvider<CorrectionApi> corrections,
            JsonMapper json) {
        this.trips = trips;
        this.records = records;
        this.corrections = corrections;
        this.json = json;
    }

    /** Thời điểm rời viện hiện hành của lần rời viện mới nhất của đăng ký. */
    Optional<Instant> departedAt(long registrationId) {
        return trips.latestAttendanceId(registrationId, "DEPARTURE").map(this::currentAt);
    }

    /** Thời điểm về hiện hành của bản ghi về mới nhất của đăng ký. */
    Optional<Instant> returnedAt(long registrationId) {
        return trips.latestAttendanceId(registrationId, "RETURN").map(this::currentAt);
    }

    Instant currentAt(long recordId) {
        var current = corrections
                .getObject()
                .current(TripKinds.ATTENDANCE.code(), List.of(recordId))
                .get(recordId);
        if (current != null && current.corrected() && current.content() != null) {
            // Nội dung đính chính đọc lại từ JSON, kiểu Java tùy cách lưu: đổi về Map trước khi lấy trường.
            Map<?, ?> content = json.convertValue(current.content(), Map.class);
            if (content.get(AT) != null) return Instant.parse(content.get(AT).toString());
        }
        return records.require(TripKinds.ATTENDANCE, recordId).content().at();
    }
}
