package com.eldercare.backend.belongings.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.belongings.api.dto.BelongingReportView;
import com.eldercare.backend.belongings.api.dto.InventoryCheckReportView;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Số liệu đồ gửi cho 016 ({@code DoGuiThongKeApi}; 013 contracts mục 1.3; Q-194 giai đoạn sau). */
public interface BelongingStatisticsApi {

    /** Thất lạc, hư hỏng, chưa trả sau kết thúc lưu trú trong khoảng ngày theo phạm vi. */
    Stream<BelongingReportView> byPeriod(ScopeFilter scope, LocalDate from, LocalDate to);

    /** Phiếu kiểm kê của một kỳ ({@code periodCode}, ví dụ {@code 2026-10}). */
    Stream<InventoryCheckReportView> inventoryChecks(String periodCode);
}
