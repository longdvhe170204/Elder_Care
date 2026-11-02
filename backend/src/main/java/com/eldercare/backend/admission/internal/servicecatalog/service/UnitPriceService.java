package com.eldercare.backend.admission.internal.servicecatalog.service;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.admission.internal.domain.ServiceAttributes;
import com.eldercare.backend.admission.internal.servicecatalog.persistence.PriceVersionStore;
import com.eldercare.backend.resident.api.dto.CareLevel;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đơn giá và tra dịch vụ (research T-05; FR-034, FR-035, BR-M11-02, Q-297). Đơn giá hiệu lực tại ngày là phiên bản có {@code
 * effective_from <= ngày <= effective_to} (hoặc còn mở); không có thì trả rỗng để 010 ghi khoản "thiếu đơn giá" (010 FR-012). {@code
 * markReferenced} chạy trong transaction của người gọi và đặt cờ {@code referenced}, từ đó giá và ngày bắt đầu của phiên bản không
 * sửa được (trigger {@code trg_price_version_immutable}). Phí lưu trú theo loại phòng và thuốc nguồn viện chưa có ràng buộc duy nhất;
 * hàm tra trả dịch vụ Hiệu lực có mã nhỏ nhất.
 */
@Service
public class UnitPriceService implements UnitPriceApi {

    private final PriceVersionStore versions;
    private final CatalogApi catalog;

    UnitPriceService(PriceVersionStore versions, CatalogApi catalog) {
        this.versions = versions;
        this.catalog = catalog;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<UnitPriceView> unitPriceAt(long serviceId, LocalDate date, boolean markReferenced) {
        if (markReferenced && !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalTransactionStateException(
                    "Đánh dấu đã tham chiếu phải chạy trong transaction của người gọi (BR-M11-02)");
        }
        Optional<UnitPriceView> found = versions.effectiveAt(serviceId, date, markReferenced);
        if (markReferenced) {
            found.ifPresent(view -> versions.markReferenced(view.priceVersionId()));
        }
        return found;
    }

    @Override
    public Optional<UnitPriceView> contractUnitPrice(long residentId, long serviceId, LocalDate date) {
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceView> careServiceOf(CareLevel level) {
        return activeService(a -> a.type() == ServiceType.CARE && a.careLevelCode() == level);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceView> residencyFeeServiceOf(long roomTypeId) {
        return activeService(a -> a.type() == ServiceType.RESIDENCY_FEE
                && Long.valueOf(roomTypeId).equals(a.roomTypeId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceView> facilityMedicationServiceOf(long medicationId) {
        return activeService(a -> a.type() == ServiceType.FACILITY_SUPPLIED_MEDICATION
                && Long.valueOf(medicationId).equals(a.medicationId()));
    }

    private Optional<ServiceView> activeService(Predicate<ServiceAttributes> filter) {
        return catalog.list(ServiceAttributes.KIND, null, true).stream()
                .filter(item -> filter.test(item.attributes()))
                .min(Comparator.comparingLong(CatalogItem::id))
                .map(item -> new ServiceView(
                        item.id(),
                        item.attributes().type(),
                        item.name(),
                        item.attributes().unit(),
                        item.attributes().billingPeriod()));
    }
}
