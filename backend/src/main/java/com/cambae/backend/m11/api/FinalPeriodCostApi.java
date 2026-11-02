package com.cambae.backend.m11.api;

import com.cambae.backend.m11.api.dto.FinalPeriodConditionView;

/** FR-034a: 004 đọc trạng thái và lý do của điều kiện chi phí kỳ cuối. */
public interface FinalPeriodCostApi {
    FinalPeriodConditionView finalPeriodCondition(long residentId);
}
