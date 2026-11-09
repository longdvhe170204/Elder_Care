package com.eldercare.backend.staffing.internal.duty.domain;

/** Thông tin tối thiểu để kiểm và thay nhiệm vụ Phụ trách bộ phận. */
public record DepartmentDuty(long id, String name, long leadStaffId) {}
