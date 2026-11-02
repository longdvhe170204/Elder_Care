package com.eldercare.backend.admission.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.resident.api.dto.CareLevel;

/**
 * Thuộc tính của dịch vụ ở {@code catalog_item} loại {@code M02.SERVICE} (data-model mục 0, 6; R-15). {@code careLevelCode} chỉ với
 * loại {@code CARE} (Q-297; kiểm ở domain, cột sinh {@code m02_active_care_level} giữ mỗi mức một dịch vụ Hiệu lực). Không xóa được vì
 * hợp đồng tham chiếu qua JSON.
 */
public record ServiceAttributes(
        ServiceType type,
        String unit,
        BillingPeriod billingPeriod,
        CareLevel careLevelCode,
        Long roomTypeId,
        Long medicationId) {

    public static final SharedKind<ServiceAttributes> KIND =
            SharedKind.of("M02.SERVICE", ServiceAttributes.class).notDeletable();
}
