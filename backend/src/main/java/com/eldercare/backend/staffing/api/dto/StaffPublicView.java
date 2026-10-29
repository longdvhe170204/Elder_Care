package com.eldercare.backend.staffing.api.dto;

import com.eldercare.backend.administration.account.api.spi.StaffStatus;

/** Kiểu đọc giữa module, theo 008 contracts mục 1.3/1.4; không chứa thông tin liên hệ hoặc lý do riêng tư. */
public record StaffPublicView(long id, String fullName, String position, String specialty, StaffStatus status) {}
