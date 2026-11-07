package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskDueStep;
import java.time.*;
import org.junit.jupiter.api.Test;

/** T042: mốc đọc lại từ MySQL, không giả định schema test rỗng. */
class BR_M04_05_OverdueReminderIT extends CareOverdueTestBase {

    @Test
    void BR_M04_05_dueTaskBecomesOverdueAtWindowEndAndRemindsOnlyOnce() throws Exception {
        long id = create();
        var store = context.getBean(CareTaskStore.class);
        var step = context.getBean(CareTaskDueStep.class);
        var task = store.get(id, false);
        step.run(task.windowFrom());
        assertThat(store.get(id, false).status()).isEqualTo(TaskStatus.DUE);
        step.run(task.windowTo().minusNanos(1000));
        assertThat(store.get(id, false).status()).isEqualTo(TaskStatus.DUE);
        step.run(task.windowTo());
        assertThat(store.get(id, false).status()).isEqualTo(TaskStatus.OVERDUE);
        step.run(task.windowTo().plusSeconds(60));
        assertThat(recorded.stream(OverdueTaskReminder.class).filter(e -> e.taskId() == id))
                .hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isEqualTo(1);
    }
}
