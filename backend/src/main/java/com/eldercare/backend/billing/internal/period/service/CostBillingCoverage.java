package com.eldercare.backend.billing.internal.period.service;

import com.eldercare.backend.admission.api.*;
import com.eldercare.backend.admission.api.dto.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** FR-011/031/032: đọc khoảng tính phí qua API nguồn; ngày vắng vẫn được tính trong khoảng. */
@Service
public class CostBillingCoverage {
    private final ResidentApi residents;
    private final ContractReadApi contracts;
    private final ResidencyDayApi days;

    public CostBillingCoverage(ResidentApi residents, ContractReadApi contracts, ResidencyDayApi days) {
        this.residents = residents;
        this.contracts = contracts;
        this.days = days;
    }

    public Set<Long> expected(YearMonth month) {
        var expected = new TreeSet<Long>();
        for (long id : residents.byStatus(EnumSet.allOf(ResidentStatus.class)))
            if (hasBillingTime(id, month)) expected.add(id);
        return expected;
    }

    public boolean fullMonth(long resident, YearMonth month) {
        LocalDate stop = actualStop(resident);
        if (stop != null && stop.isBefore(month.atEndOfMonth())) return false;
        var residential = days.daysBetween(resident, month.atDay(1), month.atEndOfMonth());
        if (residential.stream().map(ResidencyDayView::date).distinct().count() == month.lengthOfMonth()) return true;
        return firstDayCare(resident, month.atEndOfMonth())
                .map(d -> !d.isAfter(month.atDay(1)))
                .orElse(false);
    }

    private boolean hasBillingTime(long resident, YearMonth month) {
        LocalDate stop = actualStop(resident);
        if (stop != null && stop.isBefore(month.atDay(1))) return false;
        if (!days.daysBetween(resident, month.atDay(1), month.atEndOfMonth()).isEmpty()) return true;
        return firstDayCare(resident, month.atEndOfMonth()).isPresent();
    }

    private LocalDate actualStop(long resident) {
        return residents.isFinalStatus(resident)
                ? residents
                        .finalStatusAt(resident)
                        .map(at -> LocalDate.ofInstant(at, BusinessTime.ZONE))
                        .orElse(null)
                : null;
    }

    private Optional<LocalDate> firstDayCare(long resident, LocalDate through) {
        var start = contracts.ofResident(resident).stream()
                .filter(c -> Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED)
                        .contains(c.status()))
                .map(ContractView::startDate)
                .filter(d -> !d.isAfter(through))
                .min(Comparator.naturalOrder());
        if (start.isEmpty()) return Optional.empty();
        return days.dayCareSessions(resident, start.get(), through).stream()
                .filter(d -> d.attendance() == Attendance.PRESENT && !d.closureDay())
                .map(DayCareSessionView::date)
                .min(Comparator.naturalOrder());
    }
}
