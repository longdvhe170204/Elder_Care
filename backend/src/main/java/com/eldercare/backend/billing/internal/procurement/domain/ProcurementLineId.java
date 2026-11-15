package com.eldercare.backend.billing.internal.procurement.domain;

/**
 * Id dòng của yêu cầu mua sắm (api-019 {@code ProcurementRequestLine.id}, chủ dự án duyệt 2026-11-08): dòng nằm trong cột
 * {@code lines} (R-15) nên id tính từ id yêu cầu và thứ tự dòng, {@code requestId × 1.000.000 + thứ tự (từ 1)}. Nội dung
 * khóa ngoài Nháp nên id ổn định từ khi gửi duyệt; trong Nháp sửa dòng thì id đổi theo thứ tự mới.
 */
public final class ProcurementLineId {
    public static final int MAX_LINES = 999_999;
    private static final long FACTOR = 1_000_000L;

    private ProcurementLineId() {}

    public static long of(long requestId, int index) {
        return requestId * FACTOR + index + 1;
    }

    public static long requestId(long lineId) {
        return lineId / FACTOR;
    }

    /** Thứ tự dòng (từ 0); âm khi {@code lineId} không phải id dòng. */
    public static int index(long lineId) {
        return (int) (lineId % FACTOR) - 1;
    }
}
