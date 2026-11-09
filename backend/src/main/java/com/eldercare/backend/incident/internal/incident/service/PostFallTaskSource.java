package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Nguồn công việc {@code POST_FALL_MONITORING} cho 005 (research S-14): căn cứ là mã sự cố ngã; hợp lệ khi khoảng của
 * sự cố đó đang theo dõi và thời điểm dự kiến nằm trong {@code (mốc, hết]}. Ngoài khoảng thì 005 chặn.
 */
@Component
public class PostFallTaskSource implements TaskSourcePort {
    private final FallMonitoringStore windows;

    public PostFallTaskSource(FallMonitoringStore windows) {
        this.windows = windows;
    }

    @Override
    public Set<SourceType> managedTypes() {
        return Set.of(SourceType.POST_FALL_MONITORING);
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        var window = windows.active(residentId).filter(w -> w.content().incidentId() == sourceId);
        if (window.isEmpty()) return new SourceValidation(false, false, false, false);
        var content = window.get().content();
        boolean inside =
                plannedAt != null && plannedAt.isAfter(content.anchor()) && !plannedAt.isAfter(content.endsAt());
        return new SourceValidation(inside, true, inside, false);
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        return windows
                .active(residentId)
                .filter(w -> w.content().anchor().isBefore(to)
                        && w.content().endsAt().isAfter(from))
                .map(w -> new SourceView(
                        SourceType.POST_FALL_MONITORING,
                        w.content().incidentId(),
                        w.content().anchor(),
                        "Theo dõi sau ngã đến " + w.content().endsAt()))
                .stream()
                .toList();
    }
}
