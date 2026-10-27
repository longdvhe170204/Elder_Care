package com.cambae.backend.m14.internal.metric.domain;

import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.stream.Stream;

/**
 * Một chỉ tiêu kèm nguồn bản ghi (module-interfaces mục 2): mỗi chỉ tiêu cài dạng bean, đọc {@code *ThongKeApi} của module
 * nguồn rồi chuyển view sang {@link MetricRecord}. {@code MetricCatalog} gom các bean này.
 */
public interface MetricSource<R extends MetricRecord> {

    Metric<R> metric();

    /**
     * Bản ghi theo bộ lọc thô của phạm vi (bước 1, {@code ReportScope.rough}) và khoảng {@code [from, to)}; khoảng rỗng
     * là số liệu "lúc xem" (dashboard, giấy phép). Bộ lọc chung tầng, người cao tuổi do người gọi lọc lại; bộ lọc riêng
     * (loại lưu trú, mức chăm sóc, nhân viên, ca, chế độ chi phí) do nguồn áp.
     */
    Stream<? extends R> records(ScopeFilter rough, Instant from, Instant to, ReportFilters filters);

    /**
     * Bản ghi của dashboard và danh sách chi tiết không khoảng ({@code [from, to)} là hôm nay). Mặc định như
     * {@link #records}; chỉ tiêu có mặt ở cả dashboard và báo cáo với nghĩa khác nhau (tỷ lệ phục vụ ca hiện tại, FR-020
     * và FR-070) cài lại hàm này.
     */
    default Stream<? extends R> current(ScopeFilter rough, Instant from, Instant to) {
        return records(rough, from, to, ReportFilters.NONE);
    }
}
