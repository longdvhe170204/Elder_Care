package com.eldercare.backend.roombedallocation.api.dto;

import java.time.Instant;

/** Phân bổ chờ bác sĩ xác nhận vùng cách ly, 003 mục 1.9.
 * createdAt là mốc đặt dấu; null với dữ liệu cũ chưa lưu mốc, không suy từ giờ bắt đầu phân bổ.
 */
public record AllocationHandoverView(long id, long residentId, Long roomId, String description, Instant createdAt) {}
