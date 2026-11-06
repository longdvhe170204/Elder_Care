package com.eldercare.backend.resident.internal.assessment.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Cột {@code details} của dòng lịch sử mức chăm sóc ở {@code status_history} (001 T084, data-model 001 mục 0.1; FR-052,
 * FR-053): thuộc tính {@code CARE_LEVEL} của {@code M01.RESIDENT}, giá trị là mã mức, {@code starts_at} là thời điểm hiệu
 * lực, căn cứ ở cột chung; {@code recordedAt} là lúc ghi.
 */
public record CareLevelChange(Instant recordedAt) {

    public static final HistoryTrack<CareLevelChange> TRACK =
            new HistoryTrack<>("M01.RESIDENT", "CARE_LEVEL", CareLevelChange.class);

    private static final DateTimeFormatter MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC);

    /** {@code M01.CLH:<residentId>:<từ>} tới micro giây UTC, cùng dạng với dòng {@code V001_95} chép sang (FR-053). */
    public static String uniqueKey(long residentId, Instant effectiveFrom) {
        return "M01.CLH:" + residentId + ":" + MICROS.format(effectiveFrom);
    }
}
