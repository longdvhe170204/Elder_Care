package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.billing.internal.pricing.domain.*;
import com.eldercare.backend.billing.internal.source.domain.CostSourceCandidate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-008/009/012: dùng chung bộ chọn giá, riêng công thức phân bổ phí theo ngày. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class SourceCostPricing {
    private final CostPricingService pricing;

    public SourceCostPricing(CostPricingService pricing) {
        this.pricing = pricing;
    }

    public CalculatedCost calculate(CostSourceCandidate source) {
        var quote = pricing.event(source.residentId(), source.serviceId(), source.date(), source.quantity());
        var flags = new LinkedHashSet<>(source.flags());
        if (quote.missingUnitPrice()) flags.add("MISSING_UNIT_PRICE");
        if (quote.inPackage()) flags.add("INCLUDED_IN_PACKAGE");
        var amount = quote.amount();
        if (amount != null && !quote.inPackage() && source.sourceType().equals("RESIDENCY_DAY")) {
            String period = (String) source.facts().get("billingPeriod");
            if (!"MONTH".equals(period) && !"DAY".equals(period))
                throw new IllegalStateException("Chưa có kỳ tính phí lưu trú hợp lệ từ hợp đồng");
            amount = period.equals("MONTH")
                    ? ResidencyDayFee.monthly(quote.unitPrice(), source.date())
                    : ResidencyDayFee.direct(quote.unitPrice());
        }
        return new CalculatedCost(
                source.quantity(), quote.unitPrice(), source.factor(), amount, quote.basis(), source.facts(), flags);
    }
}
