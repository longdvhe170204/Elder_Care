package com.eldercare.backend.administration.shared.api.dto;

/** Đối tượng cha của dòng bảng dùng chung; {@code entityType} có tiền tố module, ví dụ {@code M01.RESIDENT}. */
public record EntityRef(String entityType, long entityId) {}
