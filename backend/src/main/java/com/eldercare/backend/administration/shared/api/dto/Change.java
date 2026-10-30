package com.eldercare.backend.administration.shared.api.dto;

/** Giá trị trước và sau một lệnh, để module gọi ghi nhật ký (FR-042). */
public record Change<V>(V before, V after) {}
