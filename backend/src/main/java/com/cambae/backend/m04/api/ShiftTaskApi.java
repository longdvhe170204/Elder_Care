package com.cambae.backend.m04.api;
/** CongViecCaApi mục 1.4; bản thật của C phải tham gia transaction lệnh gọi (MANDATORY). */
public interface ShiftTaskApi {
    void onAssignmentChanged(long residentId, long shiftId);

    void onAssignmentEnded(long assignmentId, long shiftId);
}
