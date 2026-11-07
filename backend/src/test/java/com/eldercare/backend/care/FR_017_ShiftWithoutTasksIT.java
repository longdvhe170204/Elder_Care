package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.events.ShiftWithoutTasks;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftView;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/** T024/T019, FR-017/052: quét nguồn ca, không suy tầng từ công việc hoặc giường. */
class FR_017_ShiftWithoutTasksIT extends CareOverdueTestBase {
    private void run(java.time.Instant at) {
        context.getBean("careShiftReminders", CareMinuteWork.class).run(at);
    }

    @Test
    void FR_017_emptyFloorWithoutBedsOrTasksIsRemindedAtStartOnlyOnce() {
        long emptyFloor = TestUsers.nextId();
        long emptyShift = TestUsers.nextId();
        var start = time.now().plusSeconds(60);
        shifts.shift(new ShiftView(emptyShift, emptyFloor, start, start.plusSeconds(3600), true));
        run(start.minusNanos(1000));
        assertThat(recorded.stream(ShiftWithoutTasks.class).filter(e -> e.shiftId() == emptyShift))
                .isEmpty();
        run(start);
        run(start.plusSeconds(60));
        assertThat(recorded.stream(ShiftWithoutTasks.class).filter(e -> e.shiftId() == emptyShift))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.floorAreaId()).isEqualTo(emptyFloor);
                    assertThat(e.context().recipients())
                            .containsExactly(
                                    new RecipientGroup.ShiftLead(
                                            emptyFloor,
                                            new com.eldercare.backend.common.notification.Substitution.Default()),
                                    new RecipientGroup.FloorManager(
                                            emptyFloor,
                                            new com.eldercare.backend.common.notification.Substitution.Default()));
                });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM reminder_marker WHERE kind='M04.SHIFT_WITHOUT_TASKS' AND entity_id=?",
                        Integer.class,
                        emptyShift))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, emptyShift))
                .isZero();
    }

    @Test
    void FR_052_catchUpRemindsOnlyOngoingPublishedShifts() {
        long expired = TestUsers.nextId(), draft = TestUsers.nextId();
        shifts.shift(new ShiftView(expired, TestUsers.nextId(), time.now().minusSeconds(3600), time.now(), true));
        shifts.shift(new ShiftView(
                draft,
                TestUsers.nextId(),
                time.now().minusSeconds(3600),
                time.now().plusSeconds(3600),
                false));
        run(time.now());
        assertThat(recorded.stream(ShiftWithoutTasks.class).filter(e -> e.shiftId() == shiftId))
                .hasSize(1);
        assertThat(recorded.stream(ShiftWithoutTasks.class).filter(e -> e.shiftId() == expired || e.shiftId() == draft))
                .isEmpty();
    }

    @Test
    void FR_017_shiftWithWorkDoesNotReceiveEmptyShiftReminder() throws Exception {
        create();
        run(time.now());
        assertThat(recorded.stream(ShiftWithoutTasks.class).filter(e -> e.shiftId() == shiftId))
                .isEmpty();
    }
}
