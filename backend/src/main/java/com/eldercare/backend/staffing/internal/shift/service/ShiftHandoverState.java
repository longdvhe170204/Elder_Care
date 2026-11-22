package com.eldercare.backend.staffing.internal.shift.service;

/** T014: phần bàn giao (T026) cài; Đã lập/Đã xác nhận là hoàn tất, Bản nháp/Có ý kiến thì chưa. */
public interface ShiftHandoverState {
    boolean prepared(long shiftId);

    /** Q-417 (FR-061): bàn giao chỉ bắt buộc khi phạm vi ca còn công việc chưa đóng hoặc cảnh báo đang mở. */
    default boolean mandatory(long shiftId) {
        return true;
    }
}
