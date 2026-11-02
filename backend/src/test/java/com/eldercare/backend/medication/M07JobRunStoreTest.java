package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.medication.internal.schedule.persistence.M07JobRunStore;
import java.util.Map;
import org.junit.jupiter.api.Test;

class M07JobRunStoreTest {

    @Test
    void R_15_jobCheckpointUsesSharedReminderMarker() {
        ReminderMarkers markers = mock(ReminderMarkers.class);
        M07JobRunStore store = new M07JobRunStore(markers);
        when(markers.isMarked("M07.JOB.DAILY_CHECK", "M07.JOB", 0L, "2026-10-30"))
                .thenReturn(true);

        assertThat(store.isCompleted("DAILY_CHECK", "2026-10-30")).isTrue();
        store.recordCompletion("DAILY_CHECK", "2026-10-30", "COMPLETED");

        verify(markers).mark("M07.JOB.DAILY_CHECK", "M07.JOB", 0L, "2026-10-30", Map.of("result", "COMPLETED"));
    }
}
