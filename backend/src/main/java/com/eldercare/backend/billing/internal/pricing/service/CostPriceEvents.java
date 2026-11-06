package com.eldercare.backend.billing.internal.pricing.service;

import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.admission.api.events.PriceVersionCreated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Chạy sau commit của 004, không sửa producer hoặc tự tạo chi phí. */
@Component
class CostPriceEvents {
    private final CostPriceRecalculator recalculator;

    CostPriceEvents(CostPriceRecalculator recalculator) {
        this.recalculator = recalculator;
    }

    @ApplicationModuleListener
    void priceCreated(PriceVersionCreated event) {
        recalculator.priceCreated(event);
    }

    @ApplicationModuleListener
    void addendumApplied(AddendumApplied event) {
        recalculator.addendumApplied(event);
    }
}
