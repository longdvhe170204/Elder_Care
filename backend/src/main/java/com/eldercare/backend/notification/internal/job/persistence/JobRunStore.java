package com.eldercare.backend.notification.internal.job.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ngày đã chạy xong của job hằng ngày M13 ({@code lan_chay_m13}) ở {@code reminder_marker} loại {@code M13.JOB_DONE}
 * (009 T043, data-model mục 0.1): đối tượng là mã job, dấu là ngày nghiệp vụ; lần chạy ghi ở {@code logs/jobs.log}.
 */
@Repository
public class JobRunStore {

    static final String KIND = "M13.JOB_DONE";

    private final ReminderMarkers markers;

    public JobRunStore(ReminderMarkers markers) {
        this.markers = markers;
    }

    public boolean completed(String job, LocalDate day) {
        return markers.isMarked(KIND, job, 0, day.toString());
    }

    /** Lần chạy ({@code schedulerRunId}) đã ghi ở {@code jobs.log}; ở đây chỉ giữ ngày đã xong. */
    @Transactional
    public void markCompleted(String job, LocalDate day, String schedulerRunId, Instant at) {
        markers.mark(KIND, job, 0, day.toString(), null);
    }
}
