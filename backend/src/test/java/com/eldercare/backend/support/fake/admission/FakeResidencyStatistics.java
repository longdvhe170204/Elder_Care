package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ResidenceTypeAtDateView;
import com.eldercare.backend.admission.api.dto.ResidenceTypeChangeView;
import com.eldercare.backend.admission.api.dto.WaitingEntryView;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Bản giả {@link ResidencyStatisticsApi} (004 khai báo; 016 T014): fixture đặt sẵn loại lưu trú tại ngày, hồ sơ chờ và lần
 * đổi loại lưu trú; lọc phạm vi theo {@code residentIds} như bản thật, khoảng ngày gồm hai đầu.
 */
public class FakeResidencyStatistics implements ResidencyStatisticsApi {

    private final Map<LocalDate, List<ResidenceTypeAtDateView>> atDate = new HashMap<>();
    private final List<WaitingEntryView> waiting = new ArrayList<>();
    private final List<ResidenceTypeChangeView> changes = new ArrayList<>();

    public FakeResidencyStatistics atDate(LocalDate date, ResidenceTypeAtDateView... views) {
        atDate.computeIfAbsent(date, d -> new ArrayList<>()).addAll(List.of(views));
        return this;
    }

    public FakeResidencyStatistics waiting(WaitingEntryView entry) {
        waiting.add(entry);
        return this;
    }

    public FakeResidencyStatistics change(ResidenceTypeChangeView change) {
        changes.add(change);
        return this;
    }

    @Override
    public Stream<ResidenceTypeAtDateView> residenceTypeAtDate(ScopeFilter scope, LocalDate date) {
        return atDate.getOrDefault(date, List.of()).stream()
                .filter(v -> scope.facilityWide() || scope.residentIds().contains(v.residentId()));
    }

    @Override
    public Stream<WaitingEntryView> waitingList(LocalDate from, LocalDate to) {
        return waiting.stream()
                .filter(e -> !e.applicationDate().isBefore(from)
                        && !e.applicationDate().isAfter(to));
    }

    @Override
    public Stream<ResidenceTypeChangeView> residenceTypeChanges(ScopeFilter scope, LocalDate from, LocalDate to) {
        return changes.stream()
                .filter(c -> scope.facilityWide() || scope.residentIds().contains(c.residentId()))
                .filter(c -> !c.effectiveOn().isBefore(from) && !c.effectiveOn().isAfter(to));
    }
}
