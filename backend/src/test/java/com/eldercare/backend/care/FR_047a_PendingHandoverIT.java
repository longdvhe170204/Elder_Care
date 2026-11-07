package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftView;
import com.eldercare.backend.care.internal.task.service.CareTaskDueStep;
import com.eldercare.backend.support.TestUsers;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_047a_PendingHandoverIT extends CareOverdueTestBase {
    @Test
    void FR_047a_nextShiftStartReleasesPreviousPerformerUntilConfirmation() throws Exception {
        long id = create();
        var old = shifts.shift(shiftId).orElseThrow();
        long next = TestUsers.nextId();
        shifts.shift(new ShiftView(next, floor, old.endsAt(), old.endsAt().plusSeconds(28800), true));
        shifts.onShift(caregiver.staffId(), old.endsAt(), false);
        context.getBean(CareTaskDueStep.class).run(old.endsAt());
        var task = taskStore.get(id, false);
        assertThat(task.carryover()).isEqualTo("AWAITING_HANDOVER_CONFIRMATION");
        assertThat(task.performerId()).isNull();
        assertThat(task.plannedAt()).isEqualTo(taskStore.get(id, false).plannedAt());
        context.getBean(CareTaskDueStep.class).run(old.endsAt());
        assertThat(taskStore.get(id, false).version()).isEqualTo(task.version());
    }
}
