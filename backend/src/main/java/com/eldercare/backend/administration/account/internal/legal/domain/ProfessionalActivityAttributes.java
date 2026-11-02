package com.eldercare.backend.administration.account.internal.legal.domain;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.List;

/**
 * Thuộc tính của nghiệp vụ chuyên môn ở {@code catalog_item} loại {@code M15.PROFESSIONAL_ACTIVITY} (002 T098; FR-036 →
 * FR-038, BR-M15-19): chức năng, loại giấy phép yêu cầu, cần phạm vi hoạt động của cơ sở, mục mặc định, mục phạm vi
 * hành nghề yêu cầu (danh mục của 008). Mục mặc định chỉ khi Hiệu lực: backend kiểm.
 */
public record ProfessionalActivityAttributes(
        Function functionCode,
        String requiredLicenseType,
        boolean requiresFacilityScope,
        boolean isDefault,
        List<Long> practiceScopeIds) {

    public static final SharedKind<ProfessionalActivityAttributes> KIND = SharedKind.of(
                    "M15.PROFESSIONAL_ACTIVITY", ProfessionalActivityAttributes.class)
            .notDeletable();
}
