package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.events.SharedTaskWithoutQualifiedStaff;
import com.eldercare.backend.care.api.events.TaskBecameShared;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Role;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T054: callback của 008, 015 trong transaction của lệnh nguồn (FR-032, BR-M04-13, Q-91). */
class BR_M04_13_SharedTaskIT extends CareOverdueTestBase {
    ShiftTaskApi api() {
        return context.getBean("careTaskHandover", ShiftTaskApi.class);
    }

    CareTaskStore store() {
        return context.getBean(CareTaskStore.class);
    }

    @Test
    void BR_M04_13_absenceMakesOpenTasksOfThatShiftSharedAndNotifiesFloorManager() throws Exception {
        long open = create();
        long closed = createAnother();
        jdbc.update("UPDATE care_task SET status='COMPLETED' WHERE id=?", closed);
        shifts.qualifiedStaff(shiftId, Role.CAREGIVER, Set.of(), true);

        api().onAbsence(caregiver.staffId(), shiftId);

        assertThat(store().get(open, false).performerId()).isNull();
        assertThat(store().get(open, false).status()).isEqualTo(TaskStatus.NOT_YET_DUE);
        assertThat(store().get(closed, false).performerId()).isEqualTo(caregiver.staffId());
        assertThat(recorded.stream(TaskBecameShared.class))
                .extracting(TaskBecameShared::taskId)
                .containsExactly(open);
        assertThat(recorded.stream(SharedTaskWithoutQualifiedStaff.class)).isEmpty();
    }
}
