package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.care.api.events.ActivityCostSourceBilled;
import com.eldercare.backend.care.api.events.BillableCareResultRecorded;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.MedicationBilledEvent;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** P-04: chuẩn hóa sự kiện đã công bố của nguồn về chỉ mục 010 sau khi lệnh nguồn commit. */
@Component
class BillingSourceEvents {
    private final BillingSourceIndex index;

    BillingSourceEvents(BillingSourceIndex index) {
        this.index = index;
    }

    @ApplicationModuleListener
    void onSourceBilled(CostSourceBilled event) {
        index.record(
                new IndexedBillingSource(
                        event.sourceType(),
                        event.sourceId(),
                        event.unit(),
                        event.residentId(),
                        event.incurredOn(),
                        event.occurredAt(),
                        event.serviceId().orElse(null),
                        null,
                        event.quantity()),
                event.eventKey());
    }

    /** Hoạt động của 014 (lượt có mặt ở buổi, lượt rời viện có tính phí): cùng cách ghi như {@link CostSourceBilled}. */
    @ApplicationModuleListener
    void onActivityCostSourceBilled(ActivityCostSourceBilled event) {
        index.record(
                new IndexedBillingSource(
                        event.sourceType(),
                        event.sourceId(),
                        event.unit(),
                        event.residentId(),
                        event.incurredOn(),
                        event.occurredAt(),
                        event.serviceId().orElse(null),
                        null,
                        event.quantity()),
                event.eventKey());
    }

    @ApplicationModuleListener
    void onCareResultRecorded(BillableCareResultRecorded event) {
        index.record(
                new IndexedBillingSource(
                        "CARE_TASK_RESULT",
                        Long.toString(event.resultId()),
                        "UNIT",
                        event.residentId(),
                        event.performedAt().atZone(BusinessTime.ZONE).toLocalDate(),
                        event.performedAt(),
                        event.serviceId(),
                        null,
                        event.quantity()),
                event.eventKey());
    }

    @ApplicationModuleListener
    void onMedicationBilled(MedicationBilledEvent event) {
        index.record(
                new IndexedBillingSource(
                        event.source().name(),
                        Long.toString(event.sourceId()),
                        Long.toString(event.medicationId()),
                        event.residentId(),
                        event.occurredAt().atZone(BusinessTime.ZONE).toLocalDate(),
                        event.occurredAt(),
                        null,
                        event.medicationId(),
                        event.quantity()),
                "M06.BILLED:" + event.source().name() + ":" + event.sourceId());
    }
}
