package com.eldercare.backend.health.internal.governance.persistence;

/** Truy vấn trả id bản ghi loại {@code dataType} cùng chuỗi lưu giữ với bản ghi {@code ?} (FR-051a). */
public record HealthRetentionLink(String dataType, String sql) {}
