package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.staffing.api.events.ShiftEnded;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Nghe {@code CaDaKetThuc} (008), {@code DaChuyenGiuong} (003) sau commit, transaction riêng (research K-11). */
@Component
class HeldBelongingListeners {

    private final HeldBelongingReminders reminders;

    HeldBelongingListeners(HeldBelongingReminders reminders) {
        this.reminders = reminders;
    }

    @ApplicationModuleListener
    void on(ShiftEnded event) {
        reminders.shiftEnded(event);
    }

    @ApplicationModuleListener
    void on(BedTransferred event) {
        reminders.bedTransferred(event);
    }
}
