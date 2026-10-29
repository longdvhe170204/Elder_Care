package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.resident.api.events.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** T035: tiếp nhận sự kiện qua outbox sau commit, không sửa transaction M01. */
@Component
public class AllocationSuitabilityListener {
    private final AllocationSuitabilityService service;

    public AllocationSuitabilityListener(AllocationSuitabilityService service) {
        this.service = service;
    }

    @ApplicationModuleListener
    public void careLevelChanged(CareLevelChanged event) {
        service.reassess(event.residentId());
    }

    @ApplicationModuleListener
    public void initialAssessmentConfirmed(InitialAssessmentConfirmed event) {
        service.reassess(event.residentId());
    }

    @ApplicationModuleListener
    public void personalInfoUpdated(PersonalInfoUpdated event) {
        if (event.changedFields().contains("gender")) service.reassess(event.residentId());
    }
}
