package com.eldercare.backend.incident.api.dto;

/** Nhân viên đang trong ca có thể chọn làm người bàn giao ở lệnh Chuyển viện (FR-049); chỉ mã, họ tên, chức vụ. */
public record HandoverCandidateView(long id, String fullName, String position) {}
