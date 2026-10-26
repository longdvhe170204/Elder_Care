package com.cambae.backend.m09.scheduling;

import com.cambae.backend.m09.shift.internal.service.port.ShiftCommandPortFixture;

/** Hai trường hợp thử của Q-306: tạo bản ghi bị cấm, chuyển trạng thái được phép. */
public final class M09SchedulingFixtures {
    private M09SchedulingFixtures() {}

    public static class CreatesRecords {
        public void run(ShiftCommandPortFixture port) {
            port.createSchedule();
            port.createShift();
            port.createAssignment();
            port.createHandover();
            port.addHandoverItem();
        }
    }

    public static class ClosesShift {
        public void run(ShiftCommandPortFixture port) {
            port.closeShift();
        }
    }
}
