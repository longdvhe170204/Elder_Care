package com.eldercare.backend.billing.api;

import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementTotalView;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 019 contracts mục 2.3 (MuaSamThongKeApi) cho 016 (CP.MUA_SAM, FR-055a), theo yêu cầu chung của 016
 * module-interfaces mục 2 (chủ dự án duyệt 2026-11-06). Phạm vi theo bộ phận vì ScopeFilter không có bộ phận;
 * departmentIds rỗng là toàn viện. Khoảng [from, to] theo ngày lập yêu cầu (ngày nghiệp vụ, hai đầu tính
 * vào), A xác nhận 2026-11-07.
 */
public interface ProcurementStatisticsApi {
    Stream<ProcurementRequestView> byDepartment(Set<Long> departmentIds, LocalDate from, LocalDate to);

    /** Tổng theo bộ phận và trạng thái, không tải bản ghi. */
    List<ProcurementTotalView> totals(Set<Long> departmentIds, LocalDate from, LocalDate to);
}
