package com.eldercare.backend.billing.internal.item.domain;

import com.eldercare.backend.billing.internal.item.domain.CostRecalculation.Calculation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Nội dung ghi vào cột khoản hiện có; không tạo bảng dữ liệu hoặc thêm trường HTTP. */
public record CostPricingSnapshot(
        Calculation calculation,
        Set<String> pricingFlags,
        Map<String, Object> unitPriceBasis,
        Map<String, Object> sourceValues) {
    public CostPricingSnapshot {
        pricingFlags = Set.copyOf(pricingFlags);
        unitPriceBasis = Collections.unmodifiableMap(new LinkedHashMap<>(unitPriceBasis));
        sourceValues = Collections.unmodifiableMap(new LinkedHashMap<>(sourceValues));
    }

    public Set<String> mergeFlags(Set<String> existing) {
        var flags = new LinkedHashSet<>(existing);
        flags.remove("MISSING_UNIT_PRICE");
        flags.remove("INCLUDED_IN_PACKAGE");
        flags.addAll(pricingFlags);
        return Set.copyOf(flags);
    }
}
