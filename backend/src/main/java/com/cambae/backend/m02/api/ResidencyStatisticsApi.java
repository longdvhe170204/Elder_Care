package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.ResidenceTypeAtDateView;
import com.cambae.backend.m02.api.dto.WaitingEntryView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.LocalDate;
import java.util.stream.Stream;

/** Số liệu cho báo cáo 016 (tên logic {@code LuuTruThongKeApi}; contracts mục 1.5a, research O-10); chỉ đọc. */
public interface ResidencyStatisticsApi {

    /** 016 FR-022. */
    Stream<ResidenceTypeAtDateView> residenceTypeAtDate(ScopeFilter scope, LocalDate date);

    /** 016 FR-025: hồ sơ chờ trong khoảng. */
    Stream<WaitingEntryView> waitingList(LocalDate from, LocalDate to);
}
