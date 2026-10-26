package com.cambae.backend.support.fake.m09;

import com.cambae.backend.m09.api.StaffStatisticsApi;
import com.cambae.backend.m09.api.dto.*;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

/** Bản giả T003 trả các dòng test đặt sẵn; không thay bộ lọc quyền hoặc truy vấn thật của 008/016. */
public class FakeStaffStatistics implements StaffStatisticsApi {
    private List<StaffingRatioView> ratios = List.of();
    private List<HandoverReportView> handovers = List.of();
    private List<ExpiringCredentialView> expiring = List.of();
    private List<DutyBasisView> bases = List.of();
    private List<AssignmentHistoryView> history = List.of();

    public FakeStaffStatistics ratios(List<StaffingRatioView> values) {
        ratios = List.copyOf(values);
        return this;
    }

    public FakeStaffStatistics handovers(List<HandoverReportView> values) {
        handovers = List.copyOf(values);
        return this;
    }

    public FakeStaffStatistics expiring(List<ExpiringCredentialView> values) {
        expiring = List.copyOf(values);
        return this;
    }

    public FakeStaffStatistics bases(List<DutyBasisView> values) {
        bases = List.copyOf(values);
        return this;
    }

    public FakeStaffStatistics history(List<AssignmentHistoryView> values) {
        // Giữ nguyên shiftId rỗng của giao Trưởng tầng; không gán một số ca giả.
        history = List.copyOf(values);
        return this;
    }

    public Stream<StaffingRatioView> staffingRatios(ScopeFilter scope, LocalDate from, LocalDate to) {
        return ratios.stream();
    }

    public Stream<HandoverReportView> handovers(ScopeFilter scope, LocalDate from, LocalDate to) {
        return handovers.stream();
    }

    public Stream<ExpiringCredentialView> expiringCredentials(int withinDays) {
        return expiring.stream();
    }

    public Stream<DutyBasisView> nonRoleBases(ScopeFilter scope, Instant from, Instant to) {
        return bases.stream();
    }

    public Stream<AssignmentHistoryView> assignmentHistory(ScopeFilter scope, Instant from, Instant to) {
        return history.stream();
    }
}
