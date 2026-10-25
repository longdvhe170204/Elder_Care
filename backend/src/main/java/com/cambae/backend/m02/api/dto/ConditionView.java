package com.cambae.backend.m02.api.dto;

/**
 * Một dòng điều kiện kèm căn cứ và module sở hữu. {@code exceptionRequestId} rỗng khi không có ngoại lệ được duyệt
 * (FR-064).
 */
public record ConditionView(
        String code, String name, ConditionStatus status, String basis, String module, Long exceptionRequestId) {}
