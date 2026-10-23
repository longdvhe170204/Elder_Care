package com.cambae.backend.m07.api.spi;

import java.util.Optional;

/**
 * SPI do M07 khai báo, Module M11/M03 (019) cài đặt để đối soát thuốc cung ứng theo đợt mua sắm.
 * Chỉ đọc (YeuCauThuocCungUngPort).
 */
public interface ProcurementMedicationPort {
    /** Lấy dòng yêu cầu thuốc ở trạng thái ĐÃ MUA để đối chiếu tiếp nhận. */
    Optional<ProcurementMedicationLineView> lineRecordedPurchased(long lineId);
}
