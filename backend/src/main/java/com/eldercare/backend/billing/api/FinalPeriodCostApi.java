package com.eldercare.backend.billing.api;

import com.eldercare.backend.billing.api.dto.FinalPeriodConditionView;

/** FR-034a: 004 đọc trạng thái và lý do của điều kiện chi phí kỳ cuối. */
public interface FinalPeriodCostApi {
    FinalPeriodConditionView finalPeriodCondition(long residentId);
}
