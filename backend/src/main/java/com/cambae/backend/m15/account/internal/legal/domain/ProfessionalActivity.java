package com.cambae.backend.m15.account.internal.legal.domain;

import com.cambae.backend.m15.account.api.dto.Function;
import java.util.Set;

/**
 * Một mục danh mục nghiệp vụ chuyên môn (data-model mục 4): thao tác {@code (function, PROFESSIONAL)} cần giấy phép loại
 * {@code requiredLicenseType} (rỗng: chưa cấu hình) có mọi mục phạm vi hành nghề {@code practiceScopes}, và nếu
 * {@code requiresFacilityScope} thì cơ sở có phạm vi hoạt động mã {@code code} còn hiệu lực.
 */
public record ProfessionalActivity(
        long id,
        String code,
        String name,
        Function function,
        String requiredLicenseType,
        Set<Long> practiceScopes,
        boolean requiresFacilityScope,
        boolean isDefault,
        boolean active,
        long version) {}
