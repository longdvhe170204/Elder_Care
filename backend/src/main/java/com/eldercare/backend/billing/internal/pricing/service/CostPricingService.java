package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.billing.internal.pricing.domain.DongRounding;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T006a: chọn giá theo ngày phát sinh trong lệnh của caller; không tự lập khoản. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class CostPricingService {
    private final ContractReadApi contracts;
    private final UnitPriceApi prices;

    public CostPricingService(ContractReadApi contracts, UnitPriceApi prices) {
        this.contracts = contracts;
        this.prices = prices;
    }

    public PriceQuote event(long residentId, long serviceId, LocalDate incurredOn, BigDecimal quantity) {
        var content = contracts.contentAtDate(residentId, incurredOn);
        if (content.isPresent()) {
            var contract = content.get();
            boolean registered = contract.recordedUnitPrices().containsKey(serviceId);
            boolean inPackage = false;
            for (var item : contract.content().path("services")) {
                if (item.path("serviceId").asLong() == serviceId) {
                    registered = true;
                    inPackage = item.path("inPackage").asBoolean(false);
                    break;
                }
            }
            if (registered) {
                var basis = new PriceBasis(
                        BasisKind.CONTRACT, contract.contractId(), contract.version(), contract.addendumId(), null);
                return quote(contract.recordedUnitPrices().get(serviceId), quantity, inPackage, basis);
            }
        }
        return prices.unitPriceAt(serviceId, incurredOn, true)
                .map(version -> quote(
                        version.unitPrice(),
                        quantity,
                        false,
                        new PriceBasis(BasisKind.PRICE_VERSION, null, null, null, version.priceVersionId())))
                .orElseGet(() -> new PriceQuote(null, null, false, null));
    }

    private PriceQuote quote(BigDecimal unitPrice, BigDecimal quantity, boolean inPackage, PriceBasis basis) {
        var amount = inPackage
                ? BigDecimal.ZERO
                : unitPrice == null ? null : DongRounding.round(unitPrice.multiply(quantity));
        return new PriceQuote(unitPrice, amount, inPackage, basis);
    }

    public enum BasisKind {
        CONTRACT,
        PRICE_VERSION
    }

    public record PriceBasis(
            BasisKind kind, Long contractId, Integer contentVersion, Long addendumId, Long priceVersionId) {}

    public record PriceQuote(BigDecimal unitPrice, BigDecimal amount, boolean inPackage, PriceBasis basis) {
        public boolean missingUnitPrice() {
            return !inPackage && unitPrice == null;
        }
    }
}
