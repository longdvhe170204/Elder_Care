package com.eldercare.backend.admission.api;

import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.resident.api.dto.CareLevel;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Đơn giá cho 010; 004 tự dùng (tên logic {@code DonGiaApi}; contracts mục 1.3). Không có phiên bản hiệu lực tại ngày
 * thì trả rỗng, 010 ghi khoản "thiếu đơn giá" (010 FR-012).
 */
public interface UnitPriceApi {

    /** {@code markReferenced} thì {@code Propagation.MANDATORY} và đặt {@code referenced} (BR-M11-02). */
    Optional<UnitPriceView> unitPriceAt(long serviceId, LocalDate date, boolean markReferenced);

    /** Đơn giá đã ghi trong nội dung hợp đồng của người cao tuổi tại ngày (FR-036). */
    Optional<UnitPriceView> contractUnitPrice(long residentId, long serviceId, LocalDate date);

    /** Q-297: dịch vụ chăm sóc Hiệu lực của mức. */
    Optional<ServiceView> careServiceOf(CareLevel level);

    Optional<ServiceView> residencyFeeServiceOf(long roomTypeId);

    Optional<ServiceView> facilityMedicationServiceOf(long medicationId);

    /** Dịch vụ theo mã, kể cả Ngừng hiệu lực (010 Q2, A duyệt 2026-11-10). */
    Optional<ServiceView> service(long serviceId);

    /** Dịch vụ Hiệu lực của một loại, theo mã tăng dần (010 Q3: nguồn không mang mã dịch vụ). */
    List<ServiceView> activeServicesOf(ServiceType type);

    /**
     * Mục danh mục, kể cả Ngừng hiệu lực, có tên trùng {@code name} sau khi bỏ khoảng trắng thừa, không phân biệt hoa
     * thường, giữ dấu (010 FR-021, Q4).
     */
    List<ServiceView> servicesNamed(String name);
}
