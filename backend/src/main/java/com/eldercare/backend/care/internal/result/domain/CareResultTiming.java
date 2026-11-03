package com.eldercare.backend.care.internal.result.domain;

import com.eldercare.backend.care.api.dto.TaskStatus;
import java.time.Instant;

/** FR-035: xét thời điểm thực hiện, kể cả khi đồng bộ sau cuối khung. */
public final class CareResultTiming {
    private CareResultTiming() {}

    public static TaskStatus completedStatus(Instant windowFrom, Instant windowTo, Instant performedAt) {
        if (windowFrom.isAfter(windowTo)) throw new IllegalArgumentException("Đầu khung phải không sau cuối khung");
        if (performedAt.isBefore(windowFrom)) throw new IllegalArgumentException("Không thực hiện trước đầu khung");
        return performedAt.isAfter(windowTo) ? TaskStatus.COMPLETED_LATE : TaskStatus.COMPLETED;
    }
}
