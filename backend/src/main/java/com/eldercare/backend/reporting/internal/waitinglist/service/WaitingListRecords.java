package com.eldercare.backend.reporting.internal.waitinglist.service;

import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.WaitingEntryView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hồ sơ chờ cho {@code RES.WAITING_*} (FR-025) đọc {@code ResidencyStatisticsApi.waitingList} của 004. Hồ sơ chờ chưa gắn
 * tầng, người cao tuổi; trạng thái là trạng thái hiện tại do 004 giữ.
 */
@Component
@Transactional(readOnly = true)
public class WaitingListRecords {

    /** Trạng thái còn trong danh sách chờ (004 {@code WaitingListStatus.isOpen}). */
    private static final Set<String> OPEN = Set.of("WAITING", "CONTACTED");

    /** Một hồ sơ chờ; mốc xếp khoảng là đầu ngày đăng ký. {@code waitingDays} là số ngày từ ngày đăng ký tới hôm nay. */
    public record WaitingRecord(WaitingEntryView view, Instant referenceTime, BigDecimal waitingDays)
            implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M02.WAITING_LIST:" + view.id();
        }

        @Override
        public Long floorAreaId() {
            return null;
        }

        @Override
        public Long residentId() {
            return null;
        }

        boolean open() {
            return OPEN.contains(view.status());
        }
    }

    private final ObjectProvider<ResidencyStatisticsApi> statistics;
    private final BusinessTime time;

    public WaitingListRecords(ObjectProvider<ResidencyStatisticsApi> statistics, BusinessTime time) {
        this.statistics = statistics;
        this.time = time;
    }

    /** Hồ sơ có ngày đăng ký trong {@code [from, to)} (đổi sang ngày theo giờ Việt Nam), lọc loại lưu trú. */
    public Stream<WaitingRecord> registeredIn(Instant from, Instant to, ReportFilters filters) {
        ResidencyStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        LocalDate first = LocalDate.ofInstant(from, BusinessTime.ZONE);
        LocalDate last = LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
        return records(api.waitingList(first, last))
                .filter(r -> filters.residenceType() == null
                        || filters.residenceType()
                                .equals(r.view().residenceType().name()));
    }

    /** Hồ sơ đang chờ lúc xem, mọi ngày đăng ký (số liệu "lúc xem"). */
    public Stream<WaitingRecord> openNow(ReportFilters filters) {
        ResidencyStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return records(api.waitingList(LocalDate.MIN, time.today()))
                .filter(WaitingRecord::open)
                .filter(r -> filters.residenceType() == null
                        || filters.residenceType()
                                .equals(r.view().residenceType().name()));
    }

    private Stream<WaitingRecord> records(Stream<WaitingEntryView> views) {
        LocalDate today = time.today();
        return views.map(v -> new WaitingRecord(
                v,
                v.applicationDate().atStartOfDay(BusinessTime.ZONE).toInstant(),
                BigDecimal.valueOf(Math.max(0, ChronoUnit.DAYS.between(v.applicationDate(), today)))));
    }

    static DetailRow row(WaitingRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("residenceType", r.view().residenceType().name());
        result.put(
                "careLevel",
                r.view().careLevel() == null ? null : r.view().careLevel().name());
        result.put("applicationDate", r.view().applicationDate());
        result.put("waitingDays", r.waitingDays());
        return new DetailRow(
                r.sourceKey(),
                "004",
                "WAITING_LIST_ENTRY",
                r.view().status(),
                r.referenceTime(),
                null,
                null,
                null,
                null,
                null,
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
