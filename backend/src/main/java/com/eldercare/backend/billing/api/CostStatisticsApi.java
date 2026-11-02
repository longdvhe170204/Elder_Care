package com.eldercare.backend.billing.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.dto.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

/** Dữ liệu 016 theo contracts 010 mục 1.4; bản cài lọc phạm vi, thuốc chỉ trả mã. */
public interface CostStatisticsApi {
    Stream<CostItemReportView> items(ScopeFilter scope, LocalDate from, LocalDate to);

    Stream<CostStatementReportView> statements(Optional<Long> periodId);

    Stream<CostPeriodView> periods();
}
