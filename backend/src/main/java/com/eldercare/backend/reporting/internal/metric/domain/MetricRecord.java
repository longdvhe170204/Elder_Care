package com.eldercare.backend.reporting.internal.metric.domain;

import java.time.Instant;

/**
 * Bản ghi nguồn đưa vào chỉ tiêu, đã mang trạng thái do module nguồn xác định (FR-002; module-interfaces mục 2). Mỗi
 * chỉ tiêu chuyển view của {@code *ThongKeApi} sang kiểu cài interface này.
 */
public interface MetricRecord {

    /** Khóa bản ghi ở module nguồn, duy nhất trong một chỉ tiêu; dùng khử trùng (FR-015). */
    String sourceKey();

    /** Mốc xếp khoảng (FR-006); bản ghi ngoại tuyến là thời điểm trên thiết bị. Rỗng với số liệu "lúc xem". */
    Instant referenceTime();

    /** Tầng/khu lúc phát sinh (FR-011); rỗng khi không gắn tầng. */
    Long floorAreaId();

    default Long roomId() {
        return null;
    }

    Long residentId();

    /** Hủy ghi nhận: không vào chỉ tiêu nào (FR-007). */
    default boolean voided() {
        return false;
    }

    /** Bản ghi ngoại tuyến chờ xem lại (FR-007). */
    default boolean pendingReview() {
        return false;
    }
}
