package com.eldercare.backend.care.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.dto.*;
import java.time.LocalDate;
import java.util.stream.Stream;

/** 014 contracts mục 1.2 (HoatDongThongKeApi): số liệu cho 016 (HD.*, KT.*); chỉ đọc. */
public interface ActivityStatisticsApi {
    /** FR-070: đăng ký, Có mặt, Vắng theo lý do, Không ghi nhận. */
    Stream<SessionStatisticsView> sessions(ScopeFilter scope, LocalDate from, LocalDate to);

    /** Giờ về dự kiến, gia hạn, về thực tế, thiếu người. */
    Stream<TripStatisticsView> trips(ScopeFilter scope, LocalDate from, LocalDate to);

    /** FR-065: kết quả kiểm tra kèm vai trò người kiểm tra. */
    Stream<QualityCheckStatisticsView> qualityChecks(ScopeFilter scope, LocalDate from, LocalDate to);

    /**
     * 016 FR-037 (Q-168, Q-254): công việc hoàn thành trong khoảng không vào mẫu kiểm tra vì do Trưởng tầng được giao, hoặc
     * Người phụ trách ca của ca không có Trưởng tầng được giao, tự thực hiện ("ngoài phạm vi kiểm tra").
     */
    Stream<OutOfCheckScopeView> outOfCheckScope(ScopeFilter scope, LocalDate from, LocalDate to);
}
