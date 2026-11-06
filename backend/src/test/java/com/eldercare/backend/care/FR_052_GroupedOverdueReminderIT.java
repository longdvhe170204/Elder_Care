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
    void FR_052_largeCatchUpUsesCountAndTimeRangeWithoutTaskIdPayload() throws Exception {
        var ids = new java.util.ArrayList<Long>();
        for (int i = 0; i < 40; i++) {
            ids.add(create());
            sourceId++;
            meals.validation(
                    SourceType.MEAL, sourceId, residentId, planned, new SourceValidation(true, true, true, false));
        }
        var task = taskStore.get(ids.getFirst(), false);
        var at = task.windowTo().plusSeconds(180);
        context.getBean(CareTaskDueStep.class).run(at);
        assertThat(recorded.stream(BatchedOverdueTaskReminder.class)
                        .filter(e -> e.context().floorAreaId().orElse(-1L) == floor))
                .singleElement()
                .satisfies(e -> {
                    var payload = json.readTree(json.writeValueAsString(e));
                    assertThat(payload.has("taskIds")).isFalse();
                    assertThat(payload.path("floorAreaId").asLong()).isEqualTo(floor);
                    assertThat(payload.path("taskCount").asInt()).isEqualTo(40);
                    assertThat(payload.path("from").asString())
                            .isEqualTo(task.plannedAt().toString());
                    assertThat(payload.path("to").asString())
                            .isEqualTo(task.plannedAt().toString());
                    assertThat(payload.path("catchUpAt").asString()).isEqualTo(at.toString());
                    assertThat(e.notificationRequests()
                                    .getFirst()
                                    .displayParams()
                                    .get("path"))
                            .isEqualTo("/care-tasks/overdue?floorAreaId=" + floor);
                });
        assertThat(ids)
                .allSatisfy(id -> assertThat(taskStore.get(id, false).status()).isEqualTo(TaskStatus.OVERDUE));
        context.getBean(CareTaskDueStep.class).run(at.plusSeconds(60));
        assertThat(recorded.stream(BatchedOverdueTaskReminder.class)
                        .filter(e -> e.context().floorAreaId().orElse(-1L) == floor))
                .hasSize(1);
    }

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
