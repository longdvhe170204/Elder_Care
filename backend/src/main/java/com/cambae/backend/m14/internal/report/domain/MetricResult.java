package com.cambae.backend.m14.internal.report.domain;

import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kết quả một chỉ tiêu trong báo cáo: giá trị, người cao tuổi của con số và của từng ô chia nhóm (FR-061 "nhóm chỉ có
 * đúng một người"), dòng chi tiết đã giới hạn trường (FR-014). Cùng tập bản ghi cho cả ba (FR-009).
 */
public record MetricResult(
        String metricCode,
        String metricName,
        MetricValue value,
        Set<Long> residents,
        Map<String, Set<Long>> residentsByKey,
        List<DetailRow> rows) {}
