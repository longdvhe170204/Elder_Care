package com.eldercare.backend.health.internal.scheduling.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.time.LocalDate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Chống chạy lặp việc hằng ngày M06 bằng bảng kỹ thuật reminder_marker theo R-15. */
@Repository
public class M06JobRunStore {
    static final String KIND = "M06.JOB_DONE";

    private final ReminderMarkers markers;

    public M06JobRunStore(ReminderMarkers markers) {
        this.markers = markers;
    }

    public boolean completed(String job, LocalDate day) {
        return markers.isMarked(KIND, job, 0, day.toString());
    }

    @Transactional
    public void markCompleted(String job, LocalDate day, String schedulerRunId) {
        markers.mark(KIND, job, 0, day.toString(), null);
    }
}
