package com.cambae.backend.m04.api;
/** CongViecCaApi mục 1.4; bản thật của C phải tham gia transaction lệnh gọi (MANDATORY). */
public interface ShiftTaskApi {
    void onAssignmentChanged(long residentId, long shiftId);

    void onAssignmentEnded(long assignmentId, long shiftId);

    /** MANDATORY, FR-047/047a: chuyển công việc trong transaction xác nhận bàn giao. */
    com.cambae.backend.m04.api.dto.HandoverTransferResult onHandoverConfirmed(
            long handoverId, long previousShiftId, Long nextShiftId, java.util.Set<Long> taskIds, java.time.Instant at);
}
