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
}
