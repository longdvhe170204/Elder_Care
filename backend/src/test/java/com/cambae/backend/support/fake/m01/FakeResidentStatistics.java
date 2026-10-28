package com.cambae.backend.support.fake.m01;

import com.cambae.backend.m01.api.ResidentStatisticsApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.OverdueReassessmentView;
import com.cambae.backend.m01.api.dto.ResidentChangeView;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.ResidentStatusAtDateView;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Bản giả {@link ResidentStatisticsApi} (module-interfaces mục 1.8a) cho 016: test đặt sẵn vị trí, trạng thái theo ngày,
 * biến động, quá hạn đánh giá lại; lọc theo {@link ScopeFilter} (toàn viện, tầng, phòng, hồ sơ). Nạp qua
 * {@link FakeM01Config}.
 */
public class FakeResidentStatistics implements ResidentStatisticsApi {

    private record Location(Long floorAreaId, Long roomId) {}

    private final Map<Long, Location> locations = new HashMap<>();
    private final Map<LocalDate, List<ResidentStatusAtDateView>> byDate = new HashMap<>();
    private final List<ResidentChangeView> changes = new ArrayList<>();
    private final List<OverdueReassessmentView> overdue = new ArrayList<>();

    /** Tầng, phòng của người cao tuổi để lọc theo phạm vi; không đặt thì chỉ lọc được theo toàn viện, mã hồ sơ. */
    public FakeResidentStatistics resident(long residentId, Long floorAreaId, Long roomId) {
        locations.put(residentId, new Location(floorAreaId, roomId));
        return this;
    }

    public FakeResidentStatistics statusOn(LocalDate date, long residentId, ResidentStatus status, CareLevel level) {
        byDate.computeIfAbsent(date, d -> new ArrayList<>())
                .add(new ResidentStatusAtDateView(residentId, status, Optional.ofNullable(level)));
        return this;
    }

    public FakeResidentStatistics change(ResidentChangeView change) {
        changes.add(change);
        return this;
    }

    public FakeResidentStatistics overdue(long residentId, Instant since) {
        overdue.add(new OverdueReassessmentView(residentId, since));
        return this;
    }

    @Override
    public Stream<ResidentStatusAtDateView> statusAtDate(ScopeFilter scope, LocalDate date) {
        return byDate.getOrDefault(date, List.of()).stream().filter(v -> inScope(scope, v.residentId()));
    }

    @Override
    public Stream<ResidentChangeView> changes(ScopeFilter scope, Instant from, Instant to) {
        return changes.stream()
                .filter(c -> !c.at().isBefore(from) && c.at().isBefore(to))
                .filter(c -> inScope(scope, c.residentId()));
    }

    @Override
    public Stream<OverdueReassessmentView> overdueReassessments(ScopeFilter scope) {
        return overdue.stream().filter(o -> inScope(scope, o.residentId()));
    }

    private boolean inScope(ScopeFilter scope, long residentId) {
        if (scope.facilityWide() || scope.residentIds().contains(residentId)) {
            return true;
        }
        Location at = locations.get(residentId);
        return at != null
                && ((at.floorAreaId() != null && scope.floorAreaIds().contains(at.floorAreaId()))
                        || (at.roomId() != null && scope.roomIds().contains(at.roomId())));
    }
}
