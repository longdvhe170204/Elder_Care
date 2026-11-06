package com.eldercare.backend.reporting.internal.medication.service;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.medication.api.MedicationStatisticsApi;
import com.eldercare.backend.medication.api.dto.DoseReportView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Liều thuốc cho {@code MED.*_RATE} (FR-043) đọc {@code MedicationStatisticsApi.dosesInPeriod} của 006, trạng thái cuối
 * đã tính đính chính. Mẫu số theo 006 FR-051: liều có trạng thái cuối Đã dùng, Từ chối, Không thực hiện, Bỏ lỡ (liều Mang
 * theo đã ghi kết quả có trạng thái cuối của kết quả); Không thực hiện do hệ thống đóng (không có người đóng, FR-047)
 * không vào mẫu số.
 */
@Component
@Transactional(readOnly = true)
public class DoseRecords {

    private static final Set<DoseStatus> FINAL =
            Set.of(DoseStatus.ADMINISTERED, DoseStatus.REFUSED, DoseStatus.NOT_ADMINISTERED, DoseStatus.MISSED);

    /** Ô chia nhóm của liều không xác định được ca. */
    static final String NO_SHIFT = "NO_SHIFT";

    /** Một liều; mốc xếp khoảng là thời điểm dự kiến. */
    public record DoseRecord(DoseReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M07.DOSE:" + view.doseKey();
        }

        @Override
        public Instant referenceTime() {
            return view.scheduledTime();
        }

        /** 006 trả 0 khi liều không gắn tầng. */
        @Override
        public Long floorAreaId() {
            return view.floorAreaId() == 0 ? null : view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return view.residentId();
        }

        @Override
        public boolean pendingReview() {
            return view.recordedOffline() && "PENDING_REVIEW".equals(view.reviewResult());
        }

        boolean inDenominator() {
            return FINAL.contains(view.finalStatus())
                    && !(view.finalStatus() == DoseStatus.NOT_ADMINISTERED && view.closedByStaffId() == null);
        }

        String shiftKey() {
            return view.shiftId() == null ? NO_SHIFT : Long.toString(view.shiftId());
        }
    }

    private final ObjectProvider<MedicationStatisticsApi> statistics;

    public DoseRecords(ObjectProvider<MedicationStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    public Stream<DoseRecord> in(ScopeFilter rough, Instant from, Instant to) {
        MedicationStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.dosesInPeriod(rough, from, to).map(DoseRecord::new);
    }

    static DetailRow row(DoseRecord r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shiftId", r.view().shiftId());
        result.put("administeredLate", r.view().administeredLate());
        result.put("dispensedForTrip", r.view().dispensedForTrip());
        return new DetailRow(
                r.sourceKey(),
                "006",
                "DOSE",
                r.view().finalStatus().name(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                r.residentId(),
                null,
                null,
                result,
                InfoGroup.MEDICATION,
                Map.of(),
                r.pendingReview(),
                false,
                false);
    }
}
