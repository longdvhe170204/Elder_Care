package com.cambae.backend.m11.api;

import com.cambae.backend.m11.api.dto.*;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

/** Dữ liệu 016 theo contracts 010 mục 1.4; bản cài lọc phạm vi, thuốc chỉ trả mã. */
public interface CostStatisticsApi {
    Stream<CostItemReportView> items(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<CostStatementReportView> statements(Optional<Long> periodId);

    Stream<CostPeriodView> periods();
}
