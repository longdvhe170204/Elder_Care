package com.eldercare.backend.staffing.internal.swap.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Port 015 cài, 008 dùng (contracts/module-interfaces.md mục 1.2, {@code NghiCoDuyetPort}): nghỉ theo ngày đã duyệt (008
 * FR-012: cảnh báo {@code APPROVED_LEAVE_DAY} khi xếp tay lịch Nháp) và nhân viên còn nghỉ theo ngày chờ duyệt của lịch tháng
 * (008 FR-016: hiển thị khi công bố). Chỉ đọc; 008 gọi khi cài (team-plan mục 8, dòng 015).
 */
public interface ApprovedLeaveDayPort {

    boolean approvedLeaveDay(long staffId, LocalDate date);

    List<Long> pendingLeaveDayStaff(String scopeType, Long scopeId, YearMonth month);
}
