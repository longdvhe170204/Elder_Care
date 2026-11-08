package com.eldercare.backend.billing.api.dto;

import com.eldercare.backend.admission.api.dto.ConditionStatus;
import java.util.List;

/** Một mã điều kiện quyết toán (tên logic {@code DieuKienMa}); {@code status} chỉ {@code MET} hoặc {@code NOT_MET}. */
public record SettlementConditionView(
        SettlementConditionCode code, ConditionStatus status, boolean waivable, List<String> reasons) {}
