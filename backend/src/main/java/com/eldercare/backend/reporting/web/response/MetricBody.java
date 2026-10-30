package com.eldercare.backend.reporting.web.response;

import com.eldercare.backend.reporting.internal.metric.domain.Metric;

/** {@code Metric} của contracts/api-016.yaml: định nghĩa hiển thị tại chỗ (FR-003). */
public record MetricBody(String code, String group, String name, String definition, String source) {

    public static MetricBody of(Metric<?> m) {
        return new MetricBody(m.code(), m.group().name(), m.name(), m.definition(), m.source());
    }
}
