package com.cambae.backend.m01.api;

import com.cambae.backend.m01.api.dto.OverdueReassessmentView;
import com.cambae.backend.m01.api.dto.ResidentChangeView;
import com.cambae.backend.m01.api.dto.ResidentStatusAtDateView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Số liệu cho báo cáo 016 (research O-10); chỉ đọc, không khóa, không trả bản ghi Hủy ghi nhận (016 FR-007). */
public interface ResidentStatisticsApi {

    /** Trạng thái 5.5 và mức chăm sóc hiệu lực tại ngày (016 FR-022). */
    Stream<ResidentStatusAtDateView> statusAtDate(ScopeFilter scope, LocalDate date);

    /** Lệnh trạng thái, đổi mức theo thời điểm hiệu lực (016 FR-023). */
    Stream<ResidentChangeView> changes(ScopeFilter scope, Instant from, Instant to);

    /** Người cao tuổi quá hạn đánh giá lại do 001 xác định (016 FR-044). */
    Stream<OverdueReassessmentView> overdueReassessments(ScopeFilter scope);
}
