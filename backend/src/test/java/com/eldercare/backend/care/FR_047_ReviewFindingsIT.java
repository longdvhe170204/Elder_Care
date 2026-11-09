package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.internal.task.service.CareTaskDueStep;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_047_ReviewFindingsIT extends CareOverdueTestBase {
    @Test
    void FR_047a_thirdShiftMakesPreviouslyTransferredTaskSharedAgain() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='OVERDUE',importance='IMPORTANT' WHERE id=?", id);
        var old = shifts.shift(shiftId).orElseThrow();
        long next = TestUsers.nextId();
        shifts.shift(new ShiftView(next, floor, old.endsAt(), old.endsAt().plusSeconds(28800), true));
        shifts.assignments(
                residentId, next, List.of(new CareAssignmentView(caregiver.staffId(), Role.CAREGIVER, true)));
        shifts.qualified(
                caregiver.staffId(),
                Set.of(),
                old.endsAt()
                        .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .toLocalDate(),
                true);
        context.getBean("careTaskHandover", ShiftTaskApi.class)
                .onHandoverConfirmed(TestUsers.nextId(), shiftId, next, Set.of(id), old.endsAt());
        var second = shifts.shift(next).orElseThrow();
        shifts.shift(new ShiftView(
                TestUsers.nextId(), floor, second.endsAt(), second.endsAt().plusSeconds(28800), true));
        shifts.onShift(caregiver.staffId(), second.endsAt(), false);
        context.getBean(CareTaskDueStep.class).run(second.endsAt());
        assertThat(taskStore.get(id, false).carryover()).isEqualTo("AWAITING_HANDOVER_CONFIRMATION");
        assertThat(taskStore.get(id, false).performerId()).isNull();
    }
}
