package com.eldercare.backend.admission.internal.waitinglist.service;

import com.eldercare.backend.roombedallocation.api.events.BedBecameEmpty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Giường về Trống (003): báo Hành chính chọn hồ sơ phù hợp, kể cả khi không có ai (FR-013, FR-015; research T-07). */
@Component
class BedBecameEmptyListener {

    private final MatchingEntryService matching;
    private final ApplicationEventPublisher events;

    BedBecameEmptyListener(MatchingEntryService matching, ApplicationEventPublisher events) {
        this.matching = matching;
        this.events = events;
    }

    @ApplicationModuleListener
    void on(BedBecameEmpty event) {
        matching.notifyBedAvailable(event.bedId(), events);
    }
}
