package com.eldercare.backend.staffing.internal.shift.service;

/** T014: phần bàn giao (T026) cài; Đã lập/Đã xác nhận là hoàn tất, Bản nháp/Có ý kiến thì chưa. */
public interface ShiftHandoverState {
    boolean prepared(long shiftId);
}
