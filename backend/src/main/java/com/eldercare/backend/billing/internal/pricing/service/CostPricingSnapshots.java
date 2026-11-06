package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.billing.internal.item.domain.CostPricingSnapshot;
import com.eldercare.backend.billing.internal.item.domain.CostRecalculation.Calculation;
import com.eldercare.backend.billing.internal.pricing.service.CalendarCostPricing.CalendarQuote;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.PriceQuote;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** T014: một cách dựng căn cứ/giá trị đã dùng cho lệnh lập khoản và listener tính lại. */
public final class CostPricingSnapshots {
    private CostPricingSnapshots() {}

    public static CostPricingSnapshot event(
            long serviceId, BigDecimal actualQuantity, PriceQuote price, Map<String, Object> sourceValues) {
        Objects.requireNonNull(actualQuantity);
        return new CostPricingSnapshot(
                new Calculation(actualQuantity, price.unitPrice(), null, price.amount()),
                flags(price),
                basis(serviceId, price),
                sourceValues);
    }

    public static CostPricingSnapshot calendar(CalendarQuote quote) {
        var basis = basis(quote.serviceId(), quote.price());
        basis.put("billingPeriod", quote.billingPeriod());
        if (quote.sourceUnitPrice() != null) basis.put("sourceUnitPrice", quote.sourceUnitPrice());
        if (quote.scheduledSessionCount() != null) {
            basis.put("scheduledSessionCount", quote.scheduledSessionCount());
            basis.put("lastScheduledSession", quote.lastScheduledSession());
        }
        return new CostPricingSnapshot(
                new Calculation(
                        BigDecimal.ONE,
                        quote.price().unitPrice(),
                        quote.factorPercent(),
                        quote.price().amount()),
                flags(quote.price()),
                basis,
                quote.sourceValues());
    }

    private static Set<String> flags(PriceQuote price) {
        if (price.includedInPackage()) return Set.of("INCLUDED_IN_PACKAGE");
        return price.missingUnitPrice() ? Set.of("MISSING_UNIT_PRICE") : Set.of();
    }

    private static LinkedHashMap<String, Object> basis(long serviceId, PriceQuote price) {
        var result = new LinkedHashMap<String, Object>();
        result.put("serviceId", serviceId);
        if (price.basis() == null) return result;
        var basis = price.basis();
        result.put("kind", basis.kind().name());
        if (basis.contractId() != null) result.put("contractId", basis.contractId());
        if (basis.contentVersion() != null) result.put("contentVersion", basis.contentVersion());
        if (basis.addendumId() != null) result.put("addendumId", basis.addendumId());
        if (basis.priceVersionId() != null) result.put("priceVersionId", basis.priceVersionId());
        return result;
    }
}
