package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.events.TaskBecameShared;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** T054, T056: đổi/kết thúc phân công, chuyển giường sang khu khác và lệnh phân lại (FR-024, FR-031, FR-032). */
class BR_M04_28_ReassignIT extends CareOverdueTestBase {
    ShiftTaskApi api() {
        return context.getBean("careTaskHandover", ShiftTaskApi.class);
    }

    CareTaskStore store() {
        return context.getBean(CareTaskStore.class);
    }

    TestUsers.TestUser second() {
        var other = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        shifts.qualified(other.staffId(), Set.of(), time.today(), true);
        return other;
    }

    @Test
    void BR_M04_28_assignmentChangeReassignsOpenTasksToTheNewAssignee() throws Exception {
        long open = create();
        long closed = createAnother();
        jdbc.update("UPDATE care_task SET status='COMPLETED' WHERE id=?", closed);
        var other = second();
        shifts.assignments(residentId, shiftId, List.of(new CareAssignmentView(other.staffId(), Role.CAREGIVER, true)));

        api().onAssignmentChanged(residentId, shiftId);

        assertThat(store().get(open, false).performerId()).isEqualTo(other.staffId());
        assertThat(store().get(closed, false).performerId()).isEqualTo(caregiver.staffId());
        assertThat(recorded.stream(TaskBecameShared.class)).isEmpty();
    }

    org.springframework.test.web.servlet.ResultActions reassign(long id, long recipient) throws Exception {
        return mvc.perform(post("/api/v1/care-tasks/" + id + "/reassign")
                .header("Idempotency-Key", "reassign-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":" + recipient + ",\"reason\":\"Điều phối trong ca\"}"));
    }
}
