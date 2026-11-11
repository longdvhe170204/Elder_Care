package com.eldercare.backend.staffing.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.staffing.api.dto.StaffRequestView;
import java.time.LocalDate;
import java.util.stream.Stream;

/** 015 cung cấp cho 016 (contracts/module-interfaces.md mục 2.1, {@code DoiCaThongKeApi}; Q-397 bỏ phủ, lời mời). */
public interface SwapStatisticsApi {

    /** Yêu cầu đổi ca, nghỉ đột xuất lập trong khoảng ngày, trong phạm vi {@code scope}. */
    Stream<StaffRequestView> requests(ScopeFilter scope, LocalDate from, LocalDate to);
}
