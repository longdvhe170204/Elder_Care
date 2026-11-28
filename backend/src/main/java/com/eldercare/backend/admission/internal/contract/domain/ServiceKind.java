package com.eldercare.backend.admission.internal.contract.domain;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.resident.api.dto.CareLevel;

/**
 * Loại, mức chăm sóc (chỉ dịch vụ chăm sóc, FR-025, Q-297), loại phòng (chỉ phí lưu trú), kỳ tính (Q-462) và trạng thái
 * Hiệu lực của dịch vụ.
 */
public record ServiceKind(
        String type, CareLevel careLevel, Long roomTypeId, BillingPeriod billingPeriod, boolean active) {}
