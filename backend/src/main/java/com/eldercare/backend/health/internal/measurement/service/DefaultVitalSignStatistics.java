package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.health.api.VitalSignStatisticsApi;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-081: bản xác nhận thay bản đo lại; mức lúc đo không bị thay bởi đính chính nội dung. */
@Service
@Transactional(readOnly = true)
public class DefaultVitalSignStatistics implements VitalSignStatisticsApi {
    private final VitalSignRecordStore records;
    private final CorrectionApi corrections;
    private final ResidentLocationApi locations;

    public DefaultVitalSignStatistics(
            VitalSignRecordStore records, CorrectionApi corrections, ResidentLocationApi locations) {
        this.records = records;
        this.corrections = corrections;
        this.locations = locations;
    }

    @Override
    public Stream<VitalSignReportView> readings(ScopeFilter scope, Instant from, Instant to) {
        var rows = records.confirmedInPeriod(from, to);
        var current = corrections.current(
                "M06.VITAL_SIGN_RECORD", rows.stream().map(row -> row.id()).toList());
        var result = new ArrayList<VitalSignReportView>();
        for (var row : rows) {
            var value = current.get(row.id());
            if (value != null && value.voided()) continue;
            var location = locations.at(row.residentId(), row.measuredAt());
            Long floorId = location.map(v -> v.floorId()).orElse(null);
            Long roomId = location.map(v -> v.roomId()).orElse(null);
            if (!scope.facilityWide()
                    && !scope.residentIds().contains(row.residentId())
                    && (floorId == null || !scope.floorAreaIds().contains(floorId))
                    && (roomId == null || !scope.roomIds().contains(roomId))) continue;
            result.add(new VitalSignReportView(
                    row.id(),
                    row.residentId(),
                    row.indicatorId(),
                    floorId,
                    row.measuredAt(),
                    row.level(),
                    currentValues(row, value)));
        }
        return result.stream();
    }

    /** Giá trị theo thành phần; thành phần có đính chính lấy giá trị đã đính chính (bản gốc không đổi). */
    private static Map<String, BigDecimal> currentValues(VitalSignRecord row, CurrentValue current) {
        var values = new LinkedHashMap<String, BigDecimal>();
        row.components().forEach((code, component) -> values.put(code, component.value()));
        if (current != null && current.corrected()) {
            values.putAll(VitalSignCorrectionProvider.values(current.content()));
        }
        return values;
    }
}
