package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftView;
import com.eldercare.backend.care.api.spi.TaskSourcePort.*;
import com.eldercare.backend.care.internal.task.service.*;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.care.FakeCareRequirements;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_047_HandoverRegressionIT extends CareOverdueTestBase {
    @Test
    void FR_047_fakeRemainsOptInForConsumerTestsWhenProductionApiExists() {
        assertThat(context.getBean(ShiftTaskApi.class)).isInstanceOf(FakeCareRequirements.class);
        assertThat(context.getBean(TaskHandoverApi.class)).isInstanceOf(FakeCareRequirements.class);
    }

    @Test
    void FR_047a_repeatedConfirmationWithoutNextShiftNeverClosesClaimedTask() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='OVERDUE',carryover='AWAITING_HANDOVER_CONFIRMATION' WHERE id=?", id);
        var api = context.getBean("careTaskHandover", ShiftTaskApi.class);
        long handover = TestUsers.nextId();
        api.onHandoverConfirmed(handover, shiftId, null, Set.of(id), time.now());
        var version = taskStore.get(id, false).version();
        assertThat(api.onHandoverConfirmed(handover, shiftId, null, Set.of(id), time.now())
                        .closedTaskIds())
                .isEmpty();
        assertThat(taskStore.get(id, false).version()).isEqualTo(version);
    }

    @Test
    void FR_046_recordingTheSameTaskInTheSameHandoverIsIdempotent() throws Exception {
        long id = create();
        var api = context.getBean("careHandoverQueries", HandoverTaskApi.class);
        long handover = TestUsers.nextId();
        api.recordedInHandover(handover, Set.of(id));
        assertThatCode(() -> api.recordedInHandover(handover, Set.of(id))).doesNotThrowAnyException();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type='M04.TASK' AND entity_id=? AND command='RECORDED_IN_HANDOVER'",
                        Integer.class,
                        id))
                .isEqualTo(1);
    }

    @Test
    void FR_052_batchUsesEachCurrentPerformerInsteadOfTheFirstRecipient() throws Exception {
        long a = create();
        sourceId++;
        meals.validation(SourceType.MEAL, sourceId, residentId, planned, new SourceValidation(true, true, true, false));
        long b = create();
        jdbc.update("UPDATE care_task SET status='OVERDUE' WHERE id IN (?,?)", a, b);
        jdbc.update("UPDATE care_task SET performer_id=? WHERE id=?", manager.staffId(), b);
        context.getBean(CareTaskNotices.class)
                .overdueBatch(List.of(taskStore.get(a, false), taskStore.get(b, false)), time.now());
        assertThat(recorded.stream(BatchedOverdueTaskReminder.class)
                        .filter(e -> e.context().sourceId() == a || e.context().sourceId() == b))
                .hasSize(2);
    }

    @Test
    void FR_047a_becomingSharedPublishesNoticeOnlyOnce() throws Exception {
        long id = create();
        var old = shifts.shift(shiftId).orElseThrow();
        shifts.shift(new ShiftView(
                TestUsers.nextId(), floor, old.endsAt(), old.endsAt().plusSeconds(28800), true));
        context.getBean(CareTaskDueStep.class).run(old.endsAt());
        context.getBean(CareTaskDueStep.class).run(old.endsAt());
        assertThat(recorded.stream(TaskBecameShared.class).filter(e -> e.taskId() == id))
                .hasSize(1);
    }
}
