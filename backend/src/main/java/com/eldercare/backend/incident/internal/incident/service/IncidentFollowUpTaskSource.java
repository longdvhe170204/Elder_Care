package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Nguồn công việc {@code INCIDENT_FOLLOW_UP} cho 005 (FR-045b): căn cứ là mã sự cố đang mở của chính người cao tuổi. Sự cố
 * đã đóng, đã hủy hoặc của người khác thì 005 chặn; công việc đã lập trước khi đóng vẫn giữ để ghi kết quả.
 */
@Component
public class IncidentFollowUpTaskSource implements TaskSourcePort {
    private static final List<String> OPEN = List.of("NEW", "IN_PROGRESS", "MONITORING");

    private final IncidentStore incidents;
    private final IncidentCatalogService catalog;

    public IncidentFollowUpTaskSource(IncidentStore incidents, IncidentCatalogService catalog) {
        this.incidents = incidents;
        this.catalog = catalog;
    }

    @Override
    public Set<SourceType> managedTypes() {
        return Set.of(SourceType.INCIDENT_FOLLOW_UP);
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        var incident = incidents.view(sourceId);
        boolean open = incident.filter(view -> OPEN.contains(view.status())).isPresent();
        boolean same = incident.filter(view -> Long.valueOf(residentId).equals(view.residentId()))
                .isPresent();
        return new SourceValidation(open, same, plannedAt != null, false);
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        return incidents.search(OPEN, residentId, null, true, Set.of(), Set.of()).stream()
                .map(view -> new SourceView(
                        SourceType.INCIDENT_FOLLOW_UP,
                        view.id(),
                        view.discoveredAt(),
                        "Việc tiếp theo của sự cố SC-" + view.id() + ", " + typeName(view)))
                .toList();
    }

    private String typeName(IncidentView view) {
        return catalog.incidentTypes().stream()
                .filter(type -> type.id() == view.currentTypeId())
                .map(IncidentTypeView::name)
                .findFirst()
                .orElse("sự cố");
    }
}
