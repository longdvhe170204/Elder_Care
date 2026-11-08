package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.health.internal.catalog.domain.HealthCatalogs;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdIndicator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Đọc chỉ số, thành phần và khoảng vật lý CFG-M06-03 để kiểm ngưỡng (FR-021). */
@Service
public class ThresholdIndicators {
    private final CatalogApi catalogs;
    private final ParameterApi parameters;

    public ThresholdIndicators(CatalogApi catalogs, ParameterApi parameters) {
        this.catalogs = catalogs;
        this.parameters = parameters;
    }

    /** Chỉ số đang dùng; 409 khi Ngừng hiệu lực (CatalogApi.requireActive). */
    public ThresholdIndicator require(long indicatorId) {
        var item = catalogs.requireActive(HealthCatalogs.VITAL_SIGN, indicatorId);
        var components = catalogs.list(HealthCatalogs.VITAL_SIGN_COMPONENT, indicatorId, false).stream()
                .map(CatalogItem::code)
                .collect(Collectors.toSet());
        var ranges = parameters.read(ParameterCode.CFG_M06_03);
        var range = ranges.isSet() ? ranges.raw().get(item.code()) : null;
        return new ThresholdIndicator(item.id(), item.code(), components, range);
    }
}
