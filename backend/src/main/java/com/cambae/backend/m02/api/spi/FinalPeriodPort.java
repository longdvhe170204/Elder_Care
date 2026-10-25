package com.cambae.backend.m02.api.spi;

import java.time.LocalDate;

/** SPI 010 cài (tên logic {@code KyCuoiPort}); mọi hàm {@code Propagation.MANDATORY}. Provisional: tên chờ E duyệt. */
public interface FinalPeriodPort {

    /** FR-066 (1): tác động kèm lệnh lập hồ sơ kết thúc; trả mã bảng kỳ cuối nháp. */
    long createDraft(long residentId, LocalDate expectedEndDate);

    /** Đổi ngày kết thúc dự kiến hoặc quá ngày dự kiến. */
    void recalculate(long residentId, LocalDate newDate);

    /** Hồ sơ kết thúc bị hủy: bỏ mốc dừng tính phí. */
    void cancelStop(long residentId);
}
