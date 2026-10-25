package com.cambae.backend.m02.api.spi;

import com.cambae.backend.m02.api.dto.Attendance;
import java.time.LocalDate;
import java.util.Optional;

/** SPI 005 cài (tên logic {@code TrangThaiCoMatBanTruPort}). Provisional: tên chờ C duyệt. */
public interface DayCareAttendancePort {

    /** FR-060: trạng thái có mặt của một buổi; rỗng khi buổi chưa xác định. */
    Optional<Attendance> status(long residentId, LocalDate date, long sessionId);
}
