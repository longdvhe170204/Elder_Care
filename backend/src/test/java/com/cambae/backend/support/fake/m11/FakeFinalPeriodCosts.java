package com.cambae.backend.support.fake.m11;

import com.cambae.backend.m11.api.FinalPeriodCostApi;
import com.cambae.backend.m11.api.dto.FinalPeriodConditionView;
import java.util.*;

/** Test phải đặt kết quả điều kiện; bản giả không tự cho phép kết thúc lưu trú. */
public class FakeFinalPeriodCosts implements FinalPeriodCostApi {
    private final Map<Long, FinalPeriodConditionView> results = new HashMap<>();

    public FakeFinalPeriodCosts condition(long residentId, FinalPeriodConditionView result) {
        results.put(residentId, result);
        return this;
    }

    @Override
    public FinalPeriodConditionView finalPeriodCondition(long residentId) {
        var result = results.get(residentId);
        if (result == null) throw new IllegalStateException("Chưa đặt kết quả kỳ cuối trong bản giả 010");
        return result;
    }

    public void reset() {
        results.clear();
    }
}
