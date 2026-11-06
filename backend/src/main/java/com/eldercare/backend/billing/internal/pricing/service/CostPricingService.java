package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.billing.internal.pricing.domain.DongRounding;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
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
        return catalogue(serviceId, incurredOn, quantity);
    }

    /** Giá ngoài hợp đồng; caller đã xác định đúng dịch vụ, không dùng để lấp giá hợp đồng còn thiếu. */
    public PriceQuote catalogue(long serviceId, LocalDate incurredOn, BigDecimal quantity) {
        return prices.unitPriceAt(serviceId, incurredOn, true)
                .map(version -> quote(
                        version.unitPrice(),
                        quantity,
                        false,
                        new PriceBasis(BasisKind.PRICE_VERSION, null, null, null, version.priceVersionId())))
                .orElseGet(() -> new PriceQuote(null, null, false, null));
    }

    public record ServiceQuote(ServiceView service, PriceQuote price) {}

    /** Giữ nguyên lượng theo nguồn; không quy đổi viên/ml hoặc dùng id thuốc làm id dịch vụ. */
    public ServiceQuote medication(long residentId, long medicationId, LocalDate incurredOn, BigDecimal quantity) {
        var service = prices.facilityMedicationServiceOf(medicationId)
                .orElseThrow(() -> unavailable("Chưa có dịch vụ giá cho thuốc nguồn viện."));
        return new ServiceQuote(service, event(residentId, service.id(), incurredOn, quantity));
    }

    public ServiceView residencyFeeService(long residentId, LocalDate incurredOn) {
        var content = contracts
                .contentAtDate(residentId, incurredOn)
                .orElseThrow(() -> unavailable("Thiếu nội dung hợp đồng tại ngày phát sinh."));
        if (!content.content().hasNonNull("roomTypeId"))
            throw unavailable("Hợp đồng chưa có loại phòng để xác định dịch vụ phí lưu trú.");
        return prices.residencyFeeServiceOf(content.content().path("roomTypeId").asLong())
                .orElseThrow(() -> unavailable("Chưa xác định được dịch vụ phí lưu trú của loại phòng."));
    }

    /** Chỉ phân giải dịch vụ theo mức trong hợp đồng tại ngày; không tự cộng hoặc lập khoản chăm sóc. */
    public ServiceView careService(long residentId, LocalDate incurredOn) {
        var content = contracts
                .contentAtDate(residentId, incurredOn)
                .orElseThrow(() -> unavailable("Thiếu nội dung hợp đồng tại ngày phát sinh."));
        var code = content.content().path("careLevel").asString();
        if (code == null) throw unavailable("Hợp đồng chưa có mức chăm sóc.");
        final CareLevel level;
        try {
            level = CareLevel.valueOf(code);
        } catch (IllegalArgumentException invalid) {
            throw unavailable("Mức chăm sóc trong hợp đồng chưa hợp lệ.");
        }
        var service = prices.careServiceOf(level)
                .orElseThrow(() -> unavailable("Chưa xác định được dịch vụ chăm sóc của mức trong hợp đồng."));
        boolean registered = content.recordedUnitPrices().containsKey(service.id());
        for (var line : content.content().path("services")) {
            if (line.path("serviceId").asLong() == service.id()) registered = true;
        }
        if (!registered)
            throw unavailable("Dịch vụ chăm sóc hiện tại không khớp dịch vụ đã đăng ký trong phiên bản hợp đồng.");
        return service;
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", message);
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
