package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.DayCareSessionView;
import com.cambae.backend.m02.api.dto.ResidencyDayView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Ngày lưu trú cho 010 (tên logic {@code NgayLuuTruApi}; contracts mục 1.4; FR-052, FR-060, FR-074). */
public interface ResidencyDayApi {

    List<ResidencyDayView> daysBetween(long residentId, LocalDate from, LocalDate to);

    /** Mỗi buổi có lịch; buổi chưa xác định không có hệ số. */
    List<DayCareSessionView> dayCareSessions(long residentId, LocalDate from, LocalDate to);

    /** Ngày kết thúc dự kiến (FR-066), ngày qua đời (FR-070) hoặc ngày Hủy tiếp nhận (FR-075a). */
    Optional<LocalDate> billingStopDate(long residentId);
}
