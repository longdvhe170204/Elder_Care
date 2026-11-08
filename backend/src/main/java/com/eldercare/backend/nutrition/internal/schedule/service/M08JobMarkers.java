package com.eldercare.backend.nutrition.internal.schedule.service;

import com.eldercare.backend.common.data.ReminderMarkers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dấu "đã làm lúc" của việc định kỳ M08 trên {@code reminder_marker}, loại {@code M08.JOB.<khóa việc>} (data-model mục 0.1;
 * NFR-04). Một dòng nguồn, một mốc, đúng một lần: chạy bù hay chạy trùng không làm lại.
 */
@Component
public class M08JobMarkers {

    private static final String KIND_PREFIX = "M08.JOB.";

    private final ReminderMarkers markers;

    public M08JobMarkers(ReminderMarkers markers) {
        this.markers = markers;
    }

    /** {@code true} khi vừa ghi dấu (làm việc này); {@code false} khi mốc này của dòng đã làm. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(String jobKey, String sourceType, long sourceId, String checkpoint) {
        return markers.mark(KIND_PREFIX + jobKey, sourceType, sourceId, checkpoint, null);
    }
}
