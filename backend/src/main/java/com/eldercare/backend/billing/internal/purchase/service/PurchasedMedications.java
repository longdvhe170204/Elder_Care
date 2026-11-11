package com.eldercare.backend.billing.internal.purchase.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.medication.api.spi.PurchaseAssistancePort;
import com.eldercare.backend.medication.api.spi.PurchaseAssistanceView;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Q-139 (A duyệt Q5, 2026-11-10): thuốc mua hộ là đề nghị Đã mua có vật phẩm danh mục loại thuốc nguồn viện; tên thuốc lấy
 * từ đơn thuốc của 006 theo mã thuốc của vật phẩm, không suy từ mô tả tự do.
 */
@Component
class PurchasedMedications implements PurchaseAssistancePort {
    private final SimpleRecordApi records;
    private final UnitPriceApi catalog;
    private final PrescriptionApi prescriptions;

    PurchasedMedications(SimpleRecordApi records, UnitPriceApi catalog, PrescriptionApi prescriptions) {
        this.records = records;
        this.catalog = catalog;
        this.prescriptions = prescriptions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PurchaseAssistanceView> purchasedDetails(long requestId, long residentId) {
        var row = records.require(PurchaseDetails.KIND, requestId);
        var content = row.content();
        if (!"PURCHASED".equals(row.status())
                || row.residentId() == null
                || row.residentId() != residentId
                || content.catalogItemId() == null
                || content.purchaserStaffId() == null) return Optional.empty();
        var medication = catalog.service(content.catalogItemId())
                .filter(s -> s.type() == ServiceType.FACILITY_SUPPLIED_MEDICATION && s.medicationId() != null)
                .map(s -> s.medicationId());
        if (medication.isEmpty()) return Optional.empty();
        return prescriptions.history(residentId).stream()
                .map(p -> p.medication())
                .filter(m -> m.medicationId() == medication.get())
                .findFirst()
                .map(m -> new PurchaseAssistanceView(
                        requestId, residentId, m.name(), content.quantity(), content.purchaserStaffId()));
    }
}
