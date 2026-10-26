package com.cambae.backend.m09.ratio.internal.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.*;
import com.cambae.backend.m03.api.dto.LocationRef;
import com.cambae.backend.m03.api.events.*;
import com.cambae.backend.m09.ratio.internal.persistence.StaffingRatioStore;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import java.time.Instant;
import java.util.*;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/** N-09: lệnh M09 tính cùng transaction; sự kiện nguồn ngoài qua outbox sau commit, lỗi nguồn giữ để thử lại. */
@Service
public class StaffingRatioEvents {
    private final StaffingRatioService ratios;
    private final StaffingRatioSources sources;
    private final StaffingRatioStore store;
    private final ShiftStore shifts;
    private final BusinessTime time;

    public StaffingRatioEvents(
            StaffingRatioService ratios,
            StaffingRatioSources sources,
            StaffingRatioStore store,
            ShiftStore shifts,
            BusinessTime time) {
        this.ratios = ratios;
        this.sources = sources;
        this.store = store;
        this.shifts = shifts;
        this.time = time;
    }

    @ApplicationModuleListener
    public void care(CareLevelChanged event) {
        resident(event.residentId(), event.effectiveFrom(), "CARE_LEVEL_CHANGED");
    }

    @ApplicationModuleListener
    public void status(ResidentStatusChanged event) {
        boolean terminal = Set.of(
                        ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED)
                .contains(event.to());
        resident(event.residentId(), event.at(), terminal ? "FINAL_STATUS" : "RESIDENT_ABSENCE");
    }

    @ApplicationModuleListener
    public void started(AllocationStarted event) {
        resident(event.residentId(), event.at(), "ALLOCATION_CHANGED");
    }

    @ApplicationModuleListener
    public void started(BedAllocationStarted event) {
        resident(event.residentId(), event.at(), "ALLOCATION_CHANGED");
    }

    @ApplicationModuleListener
    public void closed(AllocationClosed event) {
        resident(event.residentId(), event.at(), "ALLOCATION_CHANGED");
    }

    @ApplicationModuleListener
    public void closed(BedAllocationClosed event) {
        resident(event.residentId(), event.at(), "ALLOCATION_CHANGED");
    }

    @ApplicationModuleListener
    public void moved(BedTransferred event) {
        moved(new BedChanged(
                event.residentId(),
                event.previousAllocationId(),
                event.nextAllocationId(),
                event.previousFloorId(),
                event.nextFloorId(),
                event.at()));
    }

    @ApplicationModuleListener
    public void moved(BedChanged event) {
        var refs = new HashSet<LocationRef>();
        if (event.previousFloorId() != null) refs.add(new LocationRef("FLOOR", event.previousFloorId()));
        if (event.newFloorId() != null) refs.add(new LocationRef("FLOOR", event.newFloorId()));
        for (long id : store.openShifts(later(event.at(), time.now()))) {
            var shift = shifts.shift(id, false);
            if (refs.stream().anyMatch(ref -> sources.matches(shift, ref))) calculate(id, "ALLOCATION_CHANGED");
        }
    }

    private void resident(long resident, Instant at, String event) {
        for (long id : store.openShifts(later(at, time.now()))) {
            var shift = shifts.shift(id, false);
            if (sources.affects(shift, resident, at)) calculate(id, event);
        }
    }

    private void calculate(long id, String event) {
        if (ratios.recalculate(id, event).stream().anyMatch(w -> w.code().equals("STAFFING_RATIO_UNAVAILABLE")))
            throw new IllegalStateException("Chưa đủ nguồn/cấu hình tính lại tỷ lệ; giữ sự kiện để thử lại");
    }

    private Instant later(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }
}
