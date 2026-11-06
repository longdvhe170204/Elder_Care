package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T010: dấu xử lý và chỉ mục cùng transaction của listener, không lập bảng hay khoản chi phí. */
@Service
public class BillingSourceIndex {
    private final BillingSourceIndexStore store;
    private final ReminderMarkers markers;
    private final BusinessTime time;

    public BillingSourceIndex(BillingSourceIndexStore store, ReminderMarkers markers, BusinessTime time) {
        this.store = store;
        this.markers = markers;
        this.time = time;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(IndexedBillingSource source, String eventKey) {
        if (!markers.mark(
                "M11.SOURCE_PROCESSED",
                "M11.BILLING_SOURCE",
                source.residentId(),
                source.processingMarker(eventKey),
                Map.of("sourceType", source.sourceType(), "sourceId", source.sourceId(), "unit", source.unit()))) {
            return;
        }
        store.insertIfAbsent(source, time.now());
    }
}
