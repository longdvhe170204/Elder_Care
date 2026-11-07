package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.events.ShiftWithoutTasks;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import org.junit.jupiter.api.Test;

/** T024/T019, FR-017/052: quét nguồn ca, không suy tầng từ công việc hoặc giường. */
class FR_017_ShiftWithoutTasksIT extends CareOverdueTestBase {
    private void run(java.time.Instant at) {
        context.getBean("careShiftReminders", CareMinuteWork.class).run(at);
    }

    @Test
    void FR_017_shiftWithWorkDoesNotReceiveEmptyShiftReminder() throws Exception {
        create();
        run(time.now());
        assertThat(recorded.stream(ShiftWithoutTasks.class).filter(e -> e.shiftId() == shiftId))
                .isEmpty();
    }
}
