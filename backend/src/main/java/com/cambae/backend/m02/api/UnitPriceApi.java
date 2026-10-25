package com.cambae.backend.m02.api;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.ServiceView;
import com.cambae.backend.m02.api.dto.UnitPriceView;
import java.time.LocalDate;
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
}
