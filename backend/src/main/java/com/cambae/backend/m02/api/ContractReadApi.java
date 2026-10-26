package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.AttendanceScheduleView;
import java.time.LocalDate;
import java.util.List;

/** HopDongDocApi.lichDenTaiNgay, 004 contracts mục 1.2; không trả trường tài chính. */
public interface ContractReadApi {
    List<AttendanceScheduleView> attendanceOn(long residentId, LocalDate date);
}
