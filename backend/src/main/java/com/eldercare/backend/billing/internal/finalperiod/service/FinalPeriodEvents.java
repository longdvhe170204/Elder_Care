package com.eldercare.backend.billing.internal.finalperiod.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.events.*;
import com.eldercare.backend.billing.api.events.CostItemChanged;
import com.eldercare.backend.billing.internal.period.domain.BillingResidentState;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** SPI đã ghi nguyên tử trong lệnh 004; listener đối chiếu trạng thái hiện hành thay vì phát lại ngày cũ. */
@Component
class FinalPeriodEvents {
    private final FinalPeriodService costs;
    private final PostDeathTaskApi tasks;
    private final ResidentModuleStateApi states;

    FinalPeriodEvents(FinalPeriodService costs, PostDeathTaskApi tasks, ResidentModuleStateApi states) {
        this.costs = costs;
        this.tasks = tasks;
        this.states = states;
    }

    @ApplicationModuleListener
    void created(ResidencyEndingCreated event) {
        current(event.residentId());
    }

    @ApplicationModuleListener
    void changed(ResidencyEndingDateChanged event) {
        current(event.residentId());
    }

    @ApplicationModuleListener
    void cancelled(ResidencyEndingCancelled event) {
        current(event.residentId());
    }

    private void current(long resident) {
        costs.recheck(resident);
    }

    @ApplicationModuleListener
    void costsChanged(CostItemChanged event) {
        refresh(event.residentId());
        costs.recheck(event.residentId());
    }

    @ApplicationModuleListener
    void died(DeathRecorded event) {
        refresh(event.residentId());
    }

    private void refresh(long resident) {
        if (states.get(BillingResidentState.KIND, resident)
                .map(s -> s.deathAt() != null)
                .orElse(false)) tasks.refresh(resident);
    }
}
