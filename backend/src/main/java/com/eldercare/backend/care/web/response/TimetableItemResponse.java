package com.eldercare.backend.care.web.response;

import com.eldercare.backend.care.api.spi.TimetableSourcePort.TimetableItemView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** T058: mục thời khóa biểu (MucThoiKhoaBieu, bản dịch duyệt 2026-11-05). */
public record TimetableItemResponse(
        Instant at,

        @Schema(
                allowableValues = {
                    "CARE_TASK",
                    "MEDICATION_DOSE",
                    "ACTIVITY_SESSION",
                    "VISIT",
                    "DAY_CARE_ARRIVAL_DEPARTURE"
                })
        String source,

        String description,
        String status) {
    public static TimetableItemResponse of(TimetableItemView item) {
        return new TimetableItemResponse(item.at(), item.source(), item.description(), item.status());
    }
}
