package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.resident.api.dto.LatestVitalSign;
import com.eldercare.backend.resident.api.spi.LatestVitalSignSource;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bean {@link LatestVitalSignSource} của 001 (FR-024): chuyển tiếp {@link VitalSignApi#latestPerType}, giá trị hiện hành sau
 * đính chính kèm thời điểm đo gốc; mỗi loại chỉ số tối đa một bản, sắp theo mã chỉ số.
 */
@Component
public class DefaultLatestVitalSignSource implements LatestVitalSignSource {
    private final VitalSignApi vitalSigns;

    public DefaultLatestVitalSignSource(VitalSignApi vitalSigns) {
        this.vitalSigns = vitalSigns;
    }

    @Override
    public List<LatestVitalSign> latest(long residentId) {
        return vitalSigns.latestPerType(residentId).values().stream()
                .sorted(Comparator.comparingLong(view -> view.indicatorId()))
                .map(view -> new LatestVitalSign(
                        view.indicatorId(),
                        view.indicatorCode(),
                        view.indicatorName(),
                        view.unit(),
                        view.values(),
                        view.measuredAt()))
                .toList();
    }
}
