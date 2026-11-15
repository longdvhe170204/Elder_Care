package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftMemberView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 005 T038 (FR-030, BR-M04-13): checklist theo ca có công việc của người xem và công việc chung của ca; người có tên
 * trong ca nhận việc chung, người ngoài phạm vi không thấy công việc (404).
 */
class FR_030_CareChecklistIT extends CareOverdueTestBase {

    @Test
    void FR_030_checklistHasSharedTask_memberClaimsIt_outsiderIsRejected() throws Exception {
        long task = create();
        // Người được giao vắng: công việc chưa đóng của ca thành việc chung (BR-M04-13)
        context.getBean("careTaskHandover", ShiftTaskApi.class).onAbsence(caregiver.staffId(), shiftId);
        assertThat(taskStore.get(task, false).performerId()).isNull();

        var users = new TestUsers(jdbc, time);
        var colleague = users.staff(Role.CAREGIVER);
        var outsider = users.staff(Role.CAREGIVER);
        shifts.members(
                shiftId,
                List.of(
                        new ShiftMemberView(caregiver.staffId(), Role.CAREGIVER),
                        new ShiftMemberView(colleague.staffId(), Role.CAREGIVER)));
        shifts.onShift(colleague.staffId(), taskStore.get(task, false).plannedAt(), true);
        // Phạm vi của thành viên ca theo tầng của ca (008)
        assignments.assign(
                colleague.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        null,
                        shiftId,
                        time.now().minusSeconds(3600),
                        null));
        shifts.qualified(
                colleague.staffId(), Set.of(), planned.atZone(BusinessTime.ZONE).toLocalDate(), true);

        user.set(colleague.user());
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shiftId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[?(@.id==" + task + ")]").isNotEmpty());

        user.set(outsider.user());
        // Ngoài phạm vi: không lộ công việc
        claim(task).andExpect(status().isNotFound());

        user.set(colleague.user());
        claim(task).andExpect(status().isOk());
        assertThat(taskStore.get(task, false).performerId()).isEqualTo(colleague.staffId());
    }

    private org.springframework.test.web.servlet.ResultActions claim(long task) throws Exception {
        return mvc.perform(
                post("/api/v1/care-tasks/" + task + "/claim").header("Idempotency-Key", "claim-" + TestUsers.nextId()));
    }
}
