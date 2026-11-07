package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.OverdueReassessmentView;
import com.eldercare.backend.resident.api.dto.ReassessmentState;
import com.eldercare.backend.resident.api.dto.ResidentChangeView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatusAtDateView;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.internal.assessment.service.ReassessmentNoticeService;
import com.eldercare.backend.resident.internal.status.domain.HistoryPoint;
import com.eldercare.backend.resident.internal.status.persistence.ResidentStatisticsStore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Số liệu người cao tuổi cho 016 (module-interfaces 001 mục 1.8a; 016 FR-022, FR-023, FR-044). Phạm vi theo quy ước của
 * 001: toàn viện, hoặc tập hồ sơ 002 đã tính từ tầng, phòng được phân công. Chỉ đọc, không khóa.
 */
@Service
class ResidentStatisticsService implements ResidentStatisticsApi {

    private final ResidentStatisticsStore store;
    private final ReassessmentNoticeService notices;
    private final BusinessTime time;

    ResidentStatisticsService(ResidentStatisticsStore store, ReassessmentNoticeService notices, BusinessTime time) {
        this.store = store;
        this.notices = notices;
        this.time = time;
    }

    /** Trạng thái, mức chăm sóc hiệu lực tính tới hết ngày {@code date} (giờ của viện). */
    @Override
    @Transactional(readOnly = true)
    public Stream<ResidentStatusAtDateView> statusAtDate(ScopeFilter scope, LocalDate date) {
        Set<Long> ids = ids(scope);
        if (ids != null && ids.isEmpty()) {
            return Stream.empty();
        }
        Instant endOfDay = time.startOfDay(date.plusDays(1));
        Map<Long, String> levels = store.latestCareLevelBefore(endOfDay, ids).stream()
                .collect(Collectors.toMap(HistoryPoint::residentId, HistoryPoint::value));
        return store.latestStatusBefore(endOfDay, ids).stream()
                .sorted(Comparator.comparingLong(HistoryPoint::residentId))
                .map(r -> new ResidentStatusAtDateView(
                        r.residentId(),
                        ResidentStatus.valueOf(r.value()),
                        Optional.ofNullable(levels.get(r.residentId())).map(CareLevel::valueOf)))
                .toList()
                .stream();
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<ResidentChangeView> changes(ScopeFilter scope, Instant from, Instant to) {
        Set<Long> ids = ids(scope);
        if ((ids != null && ids.isEmpty()) || !from.isBefore(to)) {
            return Stream.empty();
        }
        List<ResidentChangeView> result = new ArrayList<>();
        for (HistoryPoint r : store.statusChanges(from, to, ids)) {
            result.add(new ResidentChangeView(
                    r.residentId(),
                    r.at(),
                    parse(r.command(), StatusCommand::valueOf),
                    parse(r.valueBefore(), ResidentStatus::valueOf),
                    Optional.of(ResidentStatus.valueOf(r.value())),
                    Optional.empty()));
        }
        for (HistoryPoint r : store.careLevelChanges(from, to, ids)) {
            result.add(new ResidentChangeView(
                    r.residentId(),
                    r.at(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(CareLevel.valueOf(r.value()))));
        }
        result.sort(Comparator.comparing(ResidentChangeView::at).thenComparingLong(ResidentChangeView::residentId));
        return result.stream();
    }

    /** Đúng trạng thái "quá hạn đánh giá lại" mà 001 xác định cho từng người (FR-043, FR-044). */
    @Override
    @Transactional(readOnly = true)
    public Stream<OverdueReassessmentView> overdueReassessments(ScopeFilter scope) {
        Set<Long> ids = ids(scope);
        List<OverdueReassessmentView> result = new ArrayList<>();
        for (long residentId : notices.residentsWithUnmetNotices()) {
            if (ids != null && !ids.contains(residentId)) {
                continue;
            }
            var status = notices.detail(residentId).status();
            if (status.state() == ReassessmentState.OVERDUE
                    && status.overdueSince().isPresent()) {
                result.add(new OverdueReassessmentView(
                        residentId, status.overdueSince().get()));
            }
        }
        return result.stream();
    }

    /** {@code null} là toàn viện. */
    private static Set<Long> ids(ScopeFilter scope) {
        return scope.facilityWide() ? null : Set.copyOf(scope.residentIds());
    }

    /** Giá trị không thuộc enum (dòng lịch sử do hệ thống ghi) thì rỗng thay vì lỗi. */
    private static <T> Optional<T> parse(String value, Function<String, T> parser) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(parser.apply(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
