package com.cambae.backend.m15.account.api.events;

/** Lần đầu gặp dữ liệu quyền thiếu hoặc mâu thuẫn; báo Quản lý viện (FR-041b). */
public record PermissionDataError(String source, Long recordId, String errorKind) {}
