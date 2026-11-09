package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.*;
import com.eldercare.backend.care.internal.task.service.*;
import com.eldercare.backend.support.TestUsers;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_047_HandoverRegressionIT extends CareOverdueTestBase {

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
}
