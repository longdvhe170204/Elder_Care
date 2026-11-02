package com.eldercare.backend.incident.api;

import com.eldercare.backend.incident.api.dto.AlertView;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Nguồn đọc cảnh báo của M05 (mục 1.7). */
public interface AlertApi {
    Optional<AlertView> view(long alertId);

    List<AlertView> byResident(long residentId, Instant from, Instant to);

    boolean hasOpenAlert(long residentId);
}
