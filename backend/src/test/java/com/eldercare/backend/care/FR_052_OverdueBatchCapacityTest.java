package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.internal.task.domain.CareTask;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.*;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_052_OverdueBatchCapacityTest {
    @Test
    void FR_052_largeRecipientGroupRunsWithoutSplittingAndOtherGroupsStillRun() {
        var store = mock(CareTaskStore.class);
        var processor = mock(CareTaskClockProcessor.class);
        var notices = mock(CareTaskNotices.class);
        var at = Instant.parse("2026-11-02T03:00:00Z");
        var tasks = new ArrayList<CareTask>();
        for (long id = 1; id <= 34; id++) {
            var task = mock(CareTask.class);
            when(task.id()).thenReturn(id);
            when(task.floorAreaId()).thenReturn(id == 34 ? 2L : 1L);
            when(task.status()).thenReturn(TaskStatus.DUE);
            when(task.windowTo()).thenReturn(at.minusSeconds(180));
            when(notices.performer(task))
                    .thenReturn(List.of(new RecipientGroup.Staff(id == 34 ? 2L : 1L, "CARE_TASK_PERFORMER")));
            tasks.add(task);
        }
        when(store.waiting(at)).thenReturn(tasks);
        var step = new CareTaskDueStep(store, processor, notices);
        assertThatCode(() -> step.run(at)).doesNotThrowAnyException();
        verify(processor)
                .advanceBatch(
                        java.util.stream.LongStream.rangeClosed(1, 33).boxed().toList(), at);
        verify(processor).advanceBatch(List.of(34L), at);
        verifyNoMoreInteractions(processor);
    }
}
