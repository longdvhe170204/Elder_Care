package com.cambae.backend.m04.api;
/** CongViecCaApi mục 1.4; bản thật của C phải tham gia transaction lệnh gọi (MANDATORY). */
public interface ShiftTaskApi {
    /** MANDATORY, FR-032/BR-M04-13: chỉ công việc của nhân viên trong ca được ghi nhận vắng thành việc chung. */
    void onAbsence(long staffId, long shiftId);

    /** MANDATORY, FR-032/BR-M09-04: nhân viên nghỉ việc, công việc còn mở thành việc chung. */
    void onResignation(long staffId, java.time.Instant effectiveAt);

    void onAssignmentChanged(long residentId, long shiftId);

    void onAssignmentEnded(long assignmentId, long shiftId);

    /** MANDATORY, FR-047/047a: chuyển công việc trong transaction xác nhận bàn giao. */
    com.cambae.backend.m04.api.dto.HandoverTransferResult onHandoverConfirmed(
            long handoverId, long previousShiftId, Long nextShiftId, java.util.Set<Long> taskIds, java.time.Instant at);
}
