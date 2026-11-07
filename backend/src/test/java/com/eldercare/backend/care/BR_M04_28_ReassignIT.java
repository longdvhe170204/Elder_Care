package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.care.api.events.TaskBecameShared;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.SharedCareTasks;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
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

    @Test
    void BR_M04_28_assignmentChangeKeepsCurrentPerformerWhoIsStillAssigned() throws Exception {
        long id = create();
        var other = second();
        shifts.assignments(
                residentId,
                shiftId,
                List.of(
                        new CareAssignmentView(other.staffId(), Role.CAREGIVER, true),
                        new CareAssignmentView(caregiver.staffId(), Role.CAREGIVER, false)));
        api().onAssignmentChanged(residentId, shiftId);
        assertThat(store().get(id, false).performerId()).isEqualTo(caregiver.staffId());
    }

    @Test
    void FR_032_assignmentEndedWithoutRemainingAssigneeMakesTaskShared() throws Exception {
        long id = create();
        shifts.assignments(residentId, shiftId, List.of());
        api().onAssignmentEnded(TestUsers.nextId(), shiftId);
        assertThat(store().get(id, false).performerId()).isNull();
        assertThat(recorded.stream(TaskBecameShared.class))
                .extracting(TaskBecameShared::taskId)
                .containsExactly(id);
    }

    @Test
    void FR_032_bedTransferToAnotherFloorMovesOpenTasksToTheNewFloorAssignment() throws Exception {
        long id = create();
        long nextFloor = TestUsers.nextId();
        long nextShift = TestUsers.nextId();
        var other = second();
        shifts.shift(new ShiftView(
                nextShift, nextFloor, time.now().minusSeconds(3600), time.now().plusSeconds(14400), true));
        shifts.assignments(
                residentId, nextShift, List.of(new CareAssignmentView(other.staffId(), Role.CAREGIVER, true)));
        var event =
                new BedTransferred(residentId, TestUsers.nextId(), TestUsers.nextId(), floor, nextFloor, time.now());

        context.getBean(SharedCareTasks.class).onBedTransferred(event);
        context.getBean(SharedCareTasks.class).onBedTransferred(event);

        var after = store().get(id, false);
        assertThat(after.floorAreaId()).isEqualTo(nextFloor);
        assertThat(after.shiftId()).isEqualTo(nextShift);
        assertThat(after.performerId()).isEqualTo(other.staffId());
        assertThat(after.status()).isEqualTo(TaskStatus.NOT_YET_DUE);
    }

    @Test
    void FR_032_bedTransferWithinTheSameFloorKeepsTasks() throws Exception {
        long id = create();
        context.getBean(SharedCareTasks.class)
                .onBedTransferred(new BedTransferred(
                        residentId, TestUsers.nextId(), TestUsers.nextId(), floor, floor, time.now()));
        assertThat(store().get(id, false).performerId()).isEqualTo(caregiver.staffId());
    }

    @Test
    void FR_031_floorManagerReassignsToQualifiedStaffOnShift() throws Exception {
        long id = create();
        var other = second();
        shifts.members(
                shiftId,
                List.of(
                        new ShiftMemberView(caregiver.staffId(), Role.CAREGIVER),
                        new ShiftMemberView(other.staffId(), Role.CAREGIVER)));
        shifts.onShift(other.staffId(), store().get(id, false).plannedAt(), true);

        reassign(id, other.staffId()).andExpect(status().isOk());

        assertThat(store().get(id, false).performerId()).isEqualTo(other.staffId());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M04.TASK' AND entity_id=? AND action='CARE_TASK_REASSIGNED'",
                        Long.class,
                        id))
                .isEqualTo(1L);
    }

    @Test
    void BR_M09_01_reassignToUnqualifiedStaffIsBlocked() throws Exception {
        long id = create();
        var other = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        shifts.members(shiftId, List.of(new ShiftMemberView(other.staffId(), Role.CAREGIVER)));
        shifts.onShift(other.staffId(), store().get(id, false).plannedAt(), true);

        reassign(id, other.staffId()).andExpect(status().isConflict());

        assertThat(store().get(id, false).performerId()).isEqualTo(caregiver.staffId());
    }

    @Test
    void FR_031_reassignToStaffWithoutOverlappingShiftIsBlocked() throws Exception {
        long id = create();
        var other = second();
        reassign(id, other.staffId()).andExpect(status().isConflict());
    }

    @Test
    void FR_031_caregiverCannotReassign() throws Exception {
        long id = create();
        var other = second();
        user.set(caregiver.user());
        reassign(id, other.staffId()).andExpect(status().isForbidden());
    }

    org.springframework.test.web.servlet.ResultActions reassign(long id, long recipient) throws Exception {
        return mvc.perform(post("/api/v1/care-tasks/" + id + "/reassign")
                .header("Idempotency-Key", "reassign-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientId\":" + recipient + ",\"reason\":\"Điều phối trong ca\"}"));
    }
}
