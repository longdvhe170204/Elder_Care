package com.eldercare.backend.billing.api.spi;

import java.util.Set;

/**
 * 019 FR-008d "Đã nhận" (Q-337, 11.7; chủ dự án duyệt 2026-11-08 thay {@code medication.api.ProcurementIntakeApi}): M11
 * khai, M07 (006) cài. Thiếu bean thì "Đã nhận" của yêu cầu loại thuốc là chưa biết ({@code null}).
 */
public interface ProcurementIntakePort {
    /** Id dòng của yêu cầu {@code procurementRequestId} có ít nhất một lần tiếp nhận thuốc cung ứng Đạt. */
    Set<Long> passedLineIds(long procurementRequestId);
}
