package com.eldercare.backend.resident.api.spi;

import java.time.LocalDate;

/**
 * SPI: thỏa thuận lựa chọn có rủi ro của kế hoạch chăm sóc (005, 8.1, Q-349) cho quyết định giữ mức chăm sóc cũ (001
 * FR-036b, Q-424). Khai ở 001 vì 005 đã phụ thuộc 001 (tránh vòng); 005 cài. Chưa có bản cài thì coi như không có thỏa
 * thuận nào.
 */
public interface CareRiskAgreementPort {

    /**
     * Thỏa thuận {@code agreementId} (mã dòng, hoặc mã gốc khi thỏa thuận được chép sang phiên bản kế hoạch mới) thuộc kế
     * hoạch chăm sóc Hiệu lực của người cao tuổi tại ngày {@code at}.
     */
    boolean isActive(long residentId, long agreementId, LocalDate at);
}
