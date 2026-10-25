package com.cambae.backend.m14.web.response;

import com.cambae.backend.m14.internal.dashboard.service.DashboardService.BlockStatus;
import com.cambae.backend.m14.internal.dashboard.service.DashboardService.Dashboard;
import java.time.Instant;
import java.util.List;

/** {@code Dashboard} của contracts/api-016.yaml (FR-020, FR-021). */
public record DashboardBody(Instant asOf, List<BlockBody> blocks, String message) {

    public record BlockBody(String group, BlockStatus status, List<MetricValueBody> values) {}

    public static DashboardBody of(Dashboard d) {
        return new DashboardBody(
                d.asOf(),
                d.blocks().stream()
                        .map(b -> new BlockBody(
                                b.group().name(),
                                b.status(),
                                b.values().stream().map(MetricValueBody::of).toList()))
                        .toList(),
                d.message());
    }
}
