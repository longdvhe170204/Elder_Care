package com.eldercare.backend.resident.api.dto;

import java.util.Optional;

/** Nhận dạng tối thiểu: họ tên, ảnh, mã hồ sơ (FR-044a của 002: người ngoài phạm vi chỉ thấy phần này). */
public record IdentityView(long residentId, String profileCode, String fullName, Optional<Long> photoFileId) {}
