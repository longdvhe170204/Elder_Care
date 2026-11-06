package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.admission.api.DayCareCalendarApi;
import com.eldercare.backend.admission.api.dto.DayCareCalendarView;
import com.eldercare.backend.admission.internal.absence.persistence.AdmissionDayCareStore;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bản cài {@link DayCareCalendarApi}: đọc bảng {@code absence} (dòng {@code CLOSURE_DAY}, {@code DAY_CARE_NOTICE}). */
@Service
class DayCareCalendarService implements DayCareCalendarApi {

    private final AdmissionDayCareStore store;

    DayCareCalendarService(AdmissionDayCareStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public DayCareCalendarView onDate(long residentId, LocalDate date) {
        return new DayCareCalendarView(date, store.activeClosureExists(date), store.noticesOn(residentId, date));
    }
}
