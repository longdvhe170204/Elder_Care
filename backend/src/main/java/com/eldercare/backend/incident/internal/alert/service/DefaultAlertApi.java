package com.eldercare.backend.incident.internal.alert.service;

import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.incident.api.dto.AlertView;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Cài đặt nguồn đọc cảnh báo của M05 (AlertApi, hợp đồng mục 1.7).
 */
@Service
public class DefaultAlertApi implements AlertApi {

    private final AlertStore alertStore;

    public DefaultAlertApi(AlertStore alertStore) {
        this.alertStore = alertStore;
    }

    @Override
    public Optional<AlertView> view(long alertId) {
        return alertStore.findById(alertId).map(DefaultAlertApi::toView);
    }

    @Override
    public List<AlertView> byResident(long residentId, Instant from, Instant to) {
        return alertStore.findByResident(residentId, from, to).stream()
                .map(DefaultAlertApi::toView)
                .toList();
    }

    @Override
    public boolean hasOpenAlert(long residentId) {
        return alertStore.hasOpenAlert(residentId);
    }

    private static AlertView toView(AlertRecord r) {
        return new AlertView(
                r.id(),
                r.residentId(),
                r.type(),
                r.level() != null ? r.level().name() : null,
                r.status() != null ? r.status().name() : null,
                r.firstAt(),
                r.floorAreaId());
    }
}
