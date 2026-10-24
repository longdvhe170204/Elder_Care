package com.cambae.backend.m14.internal.metric.service;

import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục chỉ tiêu cố định trong mã (FR-003; contracts/chi-tieu.md): mỗi nhóm nguồn khai các {@link Metric} dạng bean,
 * danh mục gom lại theo mã. Mã trùng là lỗi cấu hình, báo khi khởi động.
 */
@Service
@Transactional(readOnly = true)
public class MetricCatalog {

    private final Map<String, Metric<?>> byCode = new LinkedHashMap<>();

    MetricCatalog(ObjectProvider<Metric<?>> metrics) {
        for (Metric<?> m : metrics.orderedStream().toList()) {
            if (byCode.putIfAbsent(m.code(), m) != null) {
                throw new IllegalStateException("Trùng mã chỉ tiêu " + m.code());
            }
        }
    }

    public Optional<Metric<?>> byCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    /** Chỉ tiêu thuộc các nhóm người xem được thấy (FR-010; {@code GET /metrics}). */
    public List<Metric<?>> inGroups(Set<MetricGroup> groups) {
        return byCode.values().stream().filter(m -> groups.contains(m.group())).toList();
    }
}
