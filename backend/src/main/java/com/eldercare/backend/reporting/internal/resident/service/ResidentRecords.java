package com.eldercare.backend.reporting.internal.resident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.admission.api.ResidencyStatisticsApi;
import com.eldercare.backend.admission.api.dto.ResidenceTypeAtDateView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.ResidentChangeView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Người cao tuổi cho {@code RES.IN_RESIDENCE}, {@code RES.AT_DATE}, {@code RES.MOVEMENTS} (FR-020, FR-022, FR-023): trạng
 * thái 5.5, mức chăm sóc hiệu lực tại ngày và lệnh trạng thái theo thời điểm hiệu lực đọc ở 001; loại lưu trú theo hợp
 * đồng tại ngày (kể cả hợp đồng quá hạn, Q-20; người chưa có hợp đồng là "chưa xác định", Q-22) đọc ở 004. Tầng tại ngày
 * qua {@code ResidentLocationApi.floorOn} của 003.
 */
@Component
@Transactional(readOnly = true)
public class ResidentRecords {

    static final String UNDETERMINED = "UNDETERMINED";

    /** Đang lưu trú gồm vắng tạm thời (FR-020). */
    static final Set<ResidentStatus> IN_RESIDENCE = Set.of(
            ResidentStatus.IN_RESIDENCE,
            ResidentStatus.TEMPORARILY_ABSENT,
            ResidentStatus.OFF_SITE_ACTIVITY,
            ResidentStatus.HOSPITALIZED);

    /** Lệnh của FR-023; điểm danh đi, về hoạt động bên ngoài và lập hồ sơ không là biến động. */
    static final Set<StatusCommand> MOVEMENTS = Set.of(
            StatusCommand.COMPLETE_ADMISSION,
            StatusCommand.CANCEL_ADMISSION,
            StatusCommand.GRANT_TEMPORARY_ABSENCE,
            StatusCommand.RECORD_RETURN,
            StatusCommand.TRANSFER_OUT,
            StatusCommand.END_RESIDENCY,
            StatusCommand.RECORD_DEATH);

    static final String CARE_LEVEL_CHANGE = "CARE_LEVEL_CHANGE";

    /** Một người cao tuổi tại một ngày; mốc xếp khoảng là đầu ngày. */
    public record ResidentAtDateRecord(
            long resident,
            LocalDate date,
            ResidentStatus status,
            String careLevel,
            String residenceType,
            Long floorAreaId)
            implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M01.RESIDENT:" + resident + ":" + date;
        }

        @Override
        public Instant referenceTime() {
            return date.atStartOfDay(BusinessTime.ZONE).toInstant();
        }

        @Override
        public Long residentId() {
            return resident;
        }
    }

    /** Một biến động; mốc xếp khoảng là thời điểm hiệu lực của lệnh. */
    public record MovementRecord(ResidentChangeView view, Long floorAreaId) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M01.CHANGE:" + view.residentId() + ":" + view.at() + ":" + kind();
        }

        @Override
        public Instant referenceTime() {
            return view.at();
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }

        /** Lệnh; lệnh ghi nhận quay về tách theo trạng thái trước (kết thúc Tạm vắng, Điều trị tại bệnh viện). */
        String kind() {
            if (view.command().isEmpty()) {
                return view.careLevelAfter().isPresent() ? CARE_LEVEL_CHANGE : null;
            }
            StatusCommand c = view.command().get();
            if (!MOVEMENTS.contains(c)) {
                return null;
            }
            return c == StatusCommand.RECORD_RETURN
                    ? c.name() + ":" + view.fromStatus().map(Enum::name).orElse("UNKNOWN")
                    : c.name();
        }
    }

    private final ObjectProvider<ResidentStatisticsApi> residents;
    private final ObjectProvider<ResidencyStatisticsApi> residency;
    private final ObjectProvider<ResidentLocationApi> locations;

    public ResidentRecords(
            ObjectProvider<ResidentStatisticsApi> residents,
            ObjectProvider<ResidencyStatisticsApi> residency,
            ObjectProvider<ResidentLocationApi> locations) {
        this.residents = residents;
        this.residency = residency;
        this.locations = locations;
    }

    /** Người cao tuổi tại ngày cuối của khoảng (FR-022); bộ lọc loại lưu trú áp ở đây. */
    public Stream<ResidentAtDateRecord> atDate(ScopeFilter rough, Instant to, String residenceTypeFilter) {
        ResidentStatisticsApi api = residents.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        LocalDate date = LocalDate.ofInstant(to.minusNanos(1), BusinessTime.ZONE);
        ResidencyStatisticsApi types = residency.getIfAvailable();
        // 004 chỉ lọc theo mã người cao tuổi; phạm vi đã áp ở 001 nên đọc toàn viện rồi ghép theo mã
        Map<Long, String> typeOf = types == null
                ? Map.of()
                : types.residenceTypeAtDate(ScopeFilter.FACILITY, date)
                        .collect(Collectors.toMap(
                                ResidenceTypeAtDateView::residentId,
                                v -> v.residenceType().name(),
                                (a, b) -> a));
        ResidentLocationApi location = locations.getIfAvailable();
        return api.statusAtDate(rough, date)
                .map(v -> new ResidentAtDateRecord(
                        v.residentId(),
                        date,
                        v.status(),
                        v.careLevel().map(Enum::name).orElse(null),
                        typeOf.getOrDefault(v.residentId(), UNDETERMINED),
                        location == null
                                ? null
                                : location.floorOn(v.residentId(), date).orElse(null)))
                .filter(r -> residenceTypeFilter == null || residenceTypeFilter.equals(r.residenceType()));
    }

    public Stream<MovementRecord> movements(ScopeFilter rough, Instant from, Instant to) {
        ResidentStatisticsApi api = residents.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        ResidentLocationApi location = locations.getIfAvailable();
        Function<ResidentChangeView, Long> floor = v -> location == null
                ? null
                : location.floorOn(v.residentId(), LocalDate.ofInstant(v.at(), BusinessTime.ZONE))
                        .orElse(null);
        return api.changes(rough, from, to)
                .map(v -> new MovementRecord(v, floor.apply(v)))
                .filter(r -> r.kind() != null);
    }

    static DetailRow atDateRow(ResidentAtDateRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", r.date());
        result.put("residenceType", r.residenceType());
        result.put("careLevel", r.careLevel());
        return row(r.sourceKey(), "RESIDENT", r.status().name(), r, result);
    }

    static DetailRow movementRow(MovementRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromStatus", r.view().fromStatus().map(Enum::name).orElse(null));
        result.put("toStatus", r.view().toStatus().map(Enum::name).orElse(null));
        result.put("careLevelAfter", r.view().careLevelAfter().map(Enum::name).orElse(null));
        return row(r.sourceKey(), "RESIDENT_CHANGE", r.kind(), r, result);
    }

    private static DetailRow row(String key, String type, String status, MetricRecord r, Map<String, Object> result) {
        return new DetailRow(
                key,
                "001",
                type,
                status,
                r.referenceTime(),
                r.floorAreaId(),
                null,
                r.residentId(),
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
