package com.eldercare.backend.staffing.credential.internal.domain;

import java.util.Set;

/** Mục danh mục dùng nội bộ; code là mã đối chiếu của SPI 002, không thêm trường vào hợp đồng HTTP. */
public record CredentialType(
        long id,
        String code,
        String name,
        String group,
        boolean hasExpiry,
        Set<String> requiredForRoles,
        boolean hasPracticeScopes,
        boolean active) {}
