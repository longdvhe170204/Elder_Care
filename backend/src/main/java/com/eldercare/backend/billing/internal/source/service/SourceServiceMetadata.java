package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Loại chi phí, đơn vị và dịch vụ tính giá của nguồn trong chỉ mục (010 FR-005, FR-008, FR-012, FR-019; A duyệt Q2, Q3
 * ngày 2026-11-10). Đơn vị lấy từ danh mục 004, riêng thuốc lấy đơn vị dùng nguồn gửi; không quy đổi đơn vị.
 */
@Service
public class SourceServiceMetadata {
    private static final Set<String> MEDICATION = Set.of("DOSE", "PRN_ADMINISTRATION", "DISPENSE_RECORD");

    private final UnitPriceApi prices;

    public SourceServiceMetadata(UnitPriceApi prices) {
        this.prices = prices;
    }

    /** {@code serviceId} rỗng khi không xác định được dịch vụ duy nhất (Q3): khoản mang dấu thiếu đơn giá. */
    public record Resolved(Long serviceId, CostType costType, String unit, String description, String blockedReason) {}

    public Resolved resolve(IndexedBillingSource source) {
        if (MEDICATION.contains(source.sourceType())) return medication(source);
        Optional<ServiceView> service = source.serviceId() != null
                ? prices.service(source.serviceId())
                : unique(fallbackType(source.sourceType()));
        if (service.isPresent()) {
            var s = service.get();
            return new Resolved(s.id(), costType(s.type()), s.unit(), s.name(), null);
        }
        if (source.serviceId() != null) return blocked("Mã dịch vụ nguồn không có trong danh mục");
        ServiceType fallback = fallbackType(source.sourceType());
        if (fallback == null) return blocked("Nguồn chưa cung cấp mã dịch vụ tính phí");
        // Q3: không có hoặc có nhiều dịch vụ Hiệu lực cùng loại thì không tự chọn; khoản thiếu đơn giá.
        return new Resolved(
                null, costType(fallback), fallback == ServiceType.MEAL ? "suất" : "đêm", label(fallback), null);
    }

    /** Dịch vụ dùng để tính lại khoản chưa có mã dịch vụ (Q3), theo loại nguồn của khoản. */
    public Optional<Long> serviceForUnresolved(String sourceType) {
        return unique(fallbackType(sourceType)).map(ServiceView::id);
    }

    private Resolved medication(IndexedBillingSource source) {
        if (source.usageUnit() == null || source.usageUnit().isBlank())
            return blocked("Nguồn thuốc chưa gửi đơn vị dùng");
        if (source.medicationId() == null) return blocked("Nguồn thuốc chưa gửi mã vật phẩm");
        var service = prices.facilityMedicationServiceOf(source.medicationId());
        if (service.isEmpty()) return blocked("Thuốc chưa có trong danh mục thuốc nguồn viện");
        if (!source.usageUnit().equalsIgnoreCase(service.get().unit().strip()))
            return blocked("Đơn vị dùng của nguồn khác đơn vị tính giá trong danh mục; không quy đổi");
        // FR-019: mô tả là mã vật phẩm, không chứa tên thuốc.
        return new Resolved(
                service.get().id(), CostType.MEDICATION, source.usageUnit(), "Thuốc mã " + source.medicationId(), null);
    }

    private Optional<ServiceView> unique(ServiceType type) {
        if (type == null) return Optional.empty();
        var active = prices.activeServicesOf(type);
        return active.size() == 1 ? Optional.of(active.getFirst()) : Optional.empty();
    }

    private static ServiceType fallbackType(String sourceType) {
        return switch (sourceType) {
            case "FAMILY_MEAL" -> ServiceType.MEAL;
            case "OVERNIGHT_STAY" -> ServiceType.FAMILY_STAY;
            default -> null;
        };
    }

    private static String label(ServiceType type) {
        return type == ServiceType.MEAL ? "Suất ăn người thân" : "Người thân ở lại";
    }

    private static Resolved blocked(String reason) {
        return new Resolved(null, null, null, null, reason);
    }

    /** Q2 (A duyệt 2026-11-10). */
    static CostType costType(ServiceType type) {
        return switch (type) {
            case CONSUMABLE, MEDICAL_SUPPLY -> CostType.CONSUMABLE;
            case FACILITY_SUPPLIED_MEDICATION -> CostType.MEDICATION;
            case FAMILY_STAY -> CostType.FAMILY_OVERNIGHT_STAY;
            case MEAL -> CostType.FAMILY_MEAL;
            case ACTIVITY -> CostType.ACTIVITY;
            case RESIDENCY_FEE -> CostType.RESIDENCY_FEE;
            case DAY_CARE_SESSION -> CostType.DAY_CARE_SESSION_FEE;
            case CARE, REHABILITATION, MEDICAL_ESCORT, INJECTION, OTHER -> CostType.SERVICE;
        };
    }
}
