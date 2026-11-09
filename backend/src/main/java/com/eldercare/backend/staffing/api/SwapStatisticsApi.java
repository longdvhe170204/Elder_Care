package com.eldercare.backend.staffing.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.staffing.api.dto.StaffRequestView;
import com.eldercare.backend.staffing.api.dto.WorkingHoursView;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.Stream;

/** 015 cung cấp cho 016 (contracts/module-interfaces.md mục 2.1, {@code DoiCaThongKeApi}; Q-397 bỏ phủ, lời mời). */
public interface SwapStatisticsApi {

    /** Yêu cầu đổi ca, nghỉ đột xuất lập trong khoảng ngày, trong phạm vi {@code scope}. */
    Stream<StaffRequestView> requests(ScopeFilter scope, LocalDate from, LocalDate to);

    /** Giờ đã làm và đã xếp của nhân viên có ca trong phạm vi, theo tháng (FR-034). */
    Stream<WorkingHoursView> workingHoursByMonth(ScopeFilter scope, YearMonth month);
}
