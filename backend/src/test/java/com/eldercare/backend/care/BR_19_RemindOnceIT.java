package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.events.BatchedOverdueTaskReminder;
import com.eldercare.backend.care.api.events.OverdueTaskReminder;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskDueStep;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T063, BR-M04-29: nhắc quá hạn chỉ một lần cho mỗi công việc, kể cả khi đổi người, thành chung hay sang ca sau. */
class BR_19_RemindOnceIT extends CareOverdueTestBase {
    CareTaskStore store() {
        return context.getBean(CareTaskStore.class);
    }

    long overdue() throws Exception {
        long id = create();
        var step = context.getBean(CareTaskDueStep.class);
        var task = store().get(id, false);
        step.run(task.windowFrom());
        step.run(task.windowTo());
        return id;
    }

    long reminders(long id) {
        return recorded.stream(OverdueTaskReminder.class)
                        .filter(e -> e.taskId() == id)
                        .count()
                + recorded.stream(BatchedOverdueTaskReminder.class)
                        .filter(e -> e.context().sourceId() == id)
                        .count();
    }

    @Test
    void BR_19_sharedAndReassignedTaskIsNotRemindedAgain() throws Exception {
        long id = overdue();
        assertThat(reminders(id)).isEqualTo(1);
        var api = context.getBean("careTaskHandover", ShiftTaskApi.class);
        api.onAbsence(caregiver.staffId(), shiftId);
        var other = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        shifts.qualified(other.staffId(), Set.of(), time.today(), true);
        shifts.assignments(residentId, shiftId, List.of(new CareAssignmentView(other.staffId(), Role.CAREGIVER, true)));
        api.onAssignmentChanged(residentId, shiftId);
        var task = store().get(id, false);
        assertThat(task.performerId()).isEqualTo(other.staffId());

        context.getBean(CareTaskDueStep.class).run(task.windowTo().plusSeconds(600));

        assertThat(reminders(id)).isEqualTo(1);
    }
}
