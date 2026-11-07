package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.*;
import com.eldercare.backend.care.internal.task.service.CareTaskDueStep;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_052_GroupedOverdueReminderIT extends CareOverdueTestBase {

    @Test
    void FR_052_catchUpGroupsMissedRemindersPerRecipientAndDoesNotRepeat() throws Exception {
        long a = create();
        sourceId++;
        meals.validation(SourceType.MEAL, sourceId, residentId, planned, new SourceValidation(true, true, true, false));
        long b = create();
        var task = taskStore.get(a, false);
        var step = context.getBean(CareTaskDueStep.class);
        step.run(task.windowTo().plusSeconds(180));
        assertThat(recorded.stream(BatchedOverdueTaskReminder.class).filter(e -> e.floorAreaId() == floor))
                .singleElement()
                .satisfies(e -> assertThat(e.taskCount()).isEqualTo(2));
        assertThat(recorded.stream(OverdueTaskReminder.class)
                        .filter(e -> Set.of(a, b).contains(e.taskId())))
                .isEmpty();
        step.run(task.windowTo().plusSeconds(240));
        assertThat(recorded.stream(BatchedOverdueTaskReminder.class).filter(e -> e.floorAreaId() == floor))
                .hasSize(1);
        assertThat(taskStore.get(a, false).status()).isEqualTo(TaskStatus.OVERDUE);
    }
}
