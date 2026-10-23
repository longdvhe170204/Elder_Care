package com.cambae.backend.m03.internal.allocation.domain;
/** Liên kết phân bổ với hồ sơ; ngày trạng thái cuối lấy qua API, không JOIN bảng module khác. */
public record AllocationResident(long allocationId, long residentId) {}
