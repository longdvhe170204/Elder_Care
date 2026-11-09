package com.eldercare.backend.belongings.internal.ending.service;

import com.eldercare.backend.admission.api.events.ResidencyEndingCreated;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Nghe {@code HoSoKetThucDaLap} (004) sau commit, transaction riêng (research K-08, FR-025 phần thông báo). */
@Component
class EndingListeners {

    private final FinalStateReminders reminders;

    EndingListeners(FinalStateReminders reminders) {
        this.reminders = reminders;
    }

    @ApplicationModuleListener
    void on(ResidencyEndingCreated event) {
        reminders.endingCreated(event.residencyEndingId(), event.residentId());
    }
}
