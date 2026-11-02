package com.eldercare.backend.medication.internal.schedule.persistence;

import com.eldercare.backend.common.data.ReminderMarkers;
import java.util.Map;
import org.springframework.stereotype.Repository;

/**
 * Mốc idempotent của job M07 trên bảng chung {@code reminder_marker} (research D-19, R-15).
 */
@Repository
public class M07JobRunStore {

    private static final String ENTITY_TYPE = "M07.JOB";
    private static final long ENTITY_ID = 0L;

    private final ReminderMarkers markers;

    public M07JobRunStore(ReminderMarkers markers) {
        this.markers = markers;
    }

    /**
     * Check whether a job at a specific checkpoint has completed.
     */
    public boolean isCompleted(String jobKey, String checkpoint) {
        return markers.isMarked(kind(jobKey), ENTITY_TYPE, ENTITY_ID, checkpoint);
    }

    /**
     * Record completion of a job at a specific checkpoint.
     */
    public void recordCompletion(String jobKey, String checkpoint, String result) {
        markers.mark(kind(jobKey), ENTITY_TYPE, ENTITY_ID, checkpoint, Map.of("result", result));
    }

    private String kind(String jobKey) {
        return "M07.JOB." + jobKey;
    }
}
