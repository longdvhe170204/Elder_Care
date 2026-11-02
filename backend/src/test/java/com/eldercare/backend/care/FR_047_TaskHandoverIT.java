package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.support.TestUsers;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T043: gọi bean production cụ thể, không lấy FakeCareRequirements làm bằng chứng. */
class FR_047_TaskHandoverIT extends CareOverdueTestBase {
    @Test
    void BR_M04_07_listsOnlyOpenTasksOfTheRequestedFloorAndCurrentStatus() throws Exception {
        long id = create();
        var api = context.getBean("careHandoverQueries", HandoverTaskApi.class);
        assertThat(api.unclosed(floor, shiftId))
                .extracting(HandoverTaskView::id)
                .containsExactly(id);
        assertThat(api.unclosed(floor + 1, shiftId)).isEmpty();
        api.recordedInHandover(TestUsers.nextId(), Set.of(id));
        assertThat(api.currentStates(Set.of(id)).get(id).status()).isEqualTo(TaskStatus.NOT_YET_DUE);
    }

    @Test
    void FR_047_confirmationClosesListedNormalOverdueAndIsIdempotent() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='OVERDUE' WHERE id=?", id);
        var api = context.getBean("careTaskHandover", ShiftTaskApi.class);
        long handover = TestUsers.nextId();
        var result = api.onHandoverConfirmed(handover, shiftId, null, Set.of(id), time.now());
        assertThat(result.closedTaskIds()).containsExactly(id);
        assertThat(context.getBean(CareTaskStore.class).get(id, false).status()).isEqualTo(TaskStatus.NOT_PERFORMED);
        assertThat(api.onHandoverConfirmed(handover, shiftId, null, Set.of(id), time.now())
                        .closedTaskIds())
                .isEmpty();
    }

    @Test
    void FR_047_importantTaskTransfersWithOriginalTimeAndNewAssignment() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='OVERDUE',importance='IMPORTANT' WHERE id=?", id);
        long next = TestUsers.nextId();
        shifts.shift(new ShiftView(next, floor, time.now(), time.now().plusSeconds(28800), true));
        shifts.assignments(
                residentId,
                next,
                List.of(new com.eldercare.backend.care.api.spi.ShiftStaffPort.CareAssignmentView(
                        manager.staffId(), com.eldercare.backend.common.Role.CAREGIVER, true)));
        shifts.qualified(manager.staffId(), Set.of(), time.today(), true);
        var api = context.getBean("careTaskHandover", ShiftTaskApi.class);
        var original = context.getBean(CareTaskStore.class).get(id, false);
        var result = api.onHandoverConfirmed(TestUsers.nextId(), shiftId, next, Set.of(id), time.now());
        assertThat(result.transferredTaskIds()).containsExactly(id);
        var after = context.getBean(CareTaskStore.class).get(id, false);
        assertThat(after.status()).isEqualTo(TaskStatus.OVERDUE);
        assertThat(after.plannedAt()).isEqualTo(original.plannedAt());
        assertThat(after.performerId()).isEqualTo(manager.staffId());
        assertThat(after.carryover()).isEqualTo("FROM_PREVIOUS_SHIFT");
    }
}
