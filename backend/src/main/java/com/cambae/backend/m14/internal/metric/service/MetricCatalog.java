package com.cambae.backend.m14.internal.metric.service;

import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục chỉ tiêu cố định trong mã (FR-003; contracts/chi-tieu.md): mỗi chỉ tiêu khai một {@link MetricSource} dạng
 * bean, danh mục gom lại theo mã. Mã trùng là lỗi cấu hình, báo khi khởi động.
 */
@Service
@Transactional(readOnly = true)
public class MetricCatalog {

    private final Map<String, MetricSource<?>> byCode = new LinkedHashMap<>();

    MetricCatalog(ObjectProvider<MetricSource<?>> sources) {
        for (MetricSource<?> s : sources.orderedStream().toList()) {
            if (byCode.putIfAbsent(s.metric().code(), s) != null) {
                throw new IllegalStateException(
                        "Trùng mã chỉ tiêu " + s.metric().code());
            }
        }
    }

    public Optional<Metric<?>> byCode(String code) {
        return Optional.ofNullable(byCode.get(code)).map(MetricSource::metric);
    }

    /** Chỉ tiêu thuộc các nhóm người xem được thấy (FR-010; {@code GET /metrics}). */
    public List<Metric<?>> inGroups(Set<MetricGroup> groups) {
        return byCode.values().stream()
                .<Metric<?>>map(MetricSource::metric)
                .filter(m -> groups.contains(m.group()))
                .toList();
    }

    /** Nguồn của các chỉ tiêu trong một nhóm, theo thứ tự khai báo. */
    public List<MetricSource<?>> sourcesIn(MetricGroup group) {
        return byCode.values().stream().filter(s -> s.metric().group() == group).toList();
    }
}
