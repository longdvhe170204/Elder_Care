package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.admission.api.events.AbsenceClosed;
import com.eldercare.backend.admission.api.events.AbsenceExtended;
import com.eldercare.backend.admission.api.events.AbsenceOpened;
import com.eldercare.backend.admission.api.events.DayCareAbsenceNoticeRecorded;
import com.eldercare.backend.admission.api.events.DayCareClosureDayChanged;
import com.eldercare.backend.care.api.events.AttendanceChanged;
import com.eldercare.backend.care.api.events.AttendanceDetermined;
import com.eldercare.backend.nutrition.api.events.DietAssignmentTookEffect;
import com.eldercare.backend.nutrition.api.events.StayMealChanged;
import com.eldercare.backend.nutrition.api.events.TripMealChanged;
import com.eldercare.backend.resident.api.events.AllergyExcluded;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.resident.api.events.OtherAllergyRecorded;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nghe sự kiện nguồn sau commit của lệnh nguồn (như {@code NutritionAllergyListener}), mỗi sự kiện một transaction mới, rồi
 * giao {@link ClosingEventHandlers}. Phát sinh tới bếp ngay khi lệnh nguồn hoàn tất (SC-002 ≤ 5 phút). Sự kiện của chính
 * 011 có yêu cầu thông báo (đổi món, xác nhận bản sửa) không nghe ở đây (chỉ M13 nghe, M13ArchTest): lệnh gọi thẳng.
 */
@Component
class ClosingEventListener {

    private final ClosingEventHandlers handlers;

    ClosingEventListener(ClosingEventHandlers handlers) {
        this.handlers = handlers;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ResidentStatusChanged e) {
        handlers.onResidentStatusChanged(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(BedTransferred e) {
        handlers.onBedTransferred(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AbsenceOpened e) {
        handlers.onAbsenceOpened(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AbsenceExtended e) {
        handlers.onAbsenceExtended(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AbsenceClosed e) {
        handlers.onAbsenceClosed(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AttendanceChanged e) {
        handlers.onAttendanceChanged(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AttendanceDetermined e) {
        handlers.onAttendanceDetermined(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DayCareAbsenceNoticeRecorded e) {
        handlers.onDayCareAbsenceNotice(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DayCareClosureDayChanged e) {
        handlers.onDayCareClosureDay(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(DietAssignmentTookEffect e) {
        handlers.onDietAssignmentTookEffect(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AllergyRecorded e) {
        handlers.onAllergyRecorded(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(OtherAllergyRecorded e) {
        handlers.onOtherAllergyRecorded(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AllergyExcluded e) {
        handlers.onAllergyExcluded(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(StayMealChanged e) {
        handlers.onStayMealChanged(e);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(TripMealChanged e) {
        handlers.onTripMealChanged(e);
    }
}
