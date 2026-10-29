package com.eldercare.backend.staffing.api.dto;

import java.time.Instant;

/** Kiểu đọc giữa module, theo 008 contracts mục 1.3/1.4; không chứa thông tin liên hệ hoặc lý do riêng tư. */
public record ShiftView(long id, Long floorAreaId, Instant startsAt, Instant endsAt, String status, Long leadStaffId) {}
