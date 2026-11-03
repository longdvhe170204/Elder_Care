package com.eldercare.backend.incident.api;

import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.incident.api.dto.MonitoringFlagView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Nguồn sự cố hiện hành và lịch sử; khoảng nửa mở [from,to), mục 1.7. */
public interface IncidentApi {
    Optional<IncidentView> view(long incidentId);

    List<IncidentView> byResident(long residentId, Instant from, Instant to, boolean excludeVoided);

    boolean hasOpenItem(long residentId);

    MonitoringFlagView monitoringFlag(long residentId, Instant at);
}
