package com.eldercare.backend.admission.api;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.dto.ResidenceTypeAtDateView;
import com.eldercare.backend.admission.api.dto.WaitingEntryView;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Số liệu cho báo cáo 016 (tên logic {@code LuuTruThongKeApi}; contracts mục 1.5a, research O-10); chỉ đọc. */
public interface ResidencyStatisticsApi {

    /** 016 FR-022. */
    Stream<ResidenceTypeAtDateView> residenceTypeAtDate(ScopeFilter scope, LocalDate date);

    /** 016 FR-025: hồ sơ chờ trong khoảng. */
    Stream<WaitingEntryView> waitingList(LocalDate from, LocalDate to);
}
