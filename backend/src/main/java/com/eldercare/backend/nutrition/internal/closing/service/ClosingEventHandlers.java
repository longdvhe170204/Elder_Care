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
import com.eldercare.backend.nutrition.internal.conflict.service.M08EventListener;
import com.eldercare.backend.resident.api.events.AllergyExcluded;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.resident.api.events.OtherAllergyRecorded;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn phát sinh sau chốt (FR-039 danh sách sự kiện nguồn; module-interfaces mục 2.1): mỗi sự kiện tính lại suất của
 * người liên quan ở các bữa đã chốt chưa tới giờ, đúng một lần theo khóa sự kiện ({@link M08EventListener#once}). Chạy
 * trong transaction của người gọi; {@code ClosingEventListener} gọi sau commit của lệnh nguồn.
 */
@Component
public class ClosingEventHandlers {

    private final M08EventListener listener;
    private final PortionClosingService closing;

    ClosingEventHandlers(M08EventListener listener, PortionClosingService closing) {
        this.listener = listener;
        this.closing = closing;
    }

    @Transactional
    public void onResidentStatusChanged(ResidentStatusChanged e) {
        one("STATUS:" + e.residentId() + ":" + e.historyId(), e.residentId(), "STATUS_CHANGE");
    }

    @Transactional
    public void onBedTransferred(BedTransferred e) {
        one("BED_TRANSFER:" + e.residentId() + ":" + e.nextAllocationId(), e.residentId(), "BED_TRANSFER");
    }

    @Transactional
    public void onAbsenceOpened(AbsenceOpened e) {
        one("ABSENCE_OPENED:" + e.absenceId(), e.residentId(), "ABSENCE");
    }

    @Transactional
    public void onAbsenceExtended(AbsenceExtended e) {
        one("ABSENCE_EXTENDED:" + e.absenceId() + ":" + e.expectedReturnAt(), e.residentId(), "ABSENCE");
    }

    @Transactional
    public void onAbsenceClosed(AbsenceClosed e) {
        one("ABSENCE_CLOSED:" + e.absenceId(), e.residentId(), "ABSENCE");
    }

    @Transactional
    public void onAttendanceChanged(AttendanceChanged e) {
        one(
                "ATTENDANCE:" + e.residentId() + ":" + e.date() + ":" + e.status() + ":" + e.adHocSession(),
                e.residentId(),
                "DAY_CARE_ATTENDANCE");
    }

    @Transactional
    public void onAttendanceDetermined(AttendanceDetermined e) {
        one(
                "ATTENDANCE:" + e.residentId() + ":" + e.date() + ":" + e.status() + ":" + e.adHocSession(),
                e.residentId(),
                "DAY_CARE_ATTENDANCE");
    }

    @Transactional
    public void onDayCareAbsenceNotice(DayCareAbsenceNoticeRecorded e) {
        one(
                "DAY_CARE_ABSENCE:" + e.residentId() + ":" + e.date() + ":" + e.sessionId(),
                e.residentId(),
                "DAY_CARE_ATTENDANCE");
    }

    @Transactional
    public void onDayCareClosureDay(DayCareClosureDayChanged e) {
        all("DAY_CARE_CLOSURE:" + e.date() + ":" + e.active(), "DAY_CARE_CLOSURE");
    }

    @Transactional
    public void onDietAssignmentTookEffect(DietAssignmentTookEffect e) {
        one("DIET_ASSIGNMENT:" + e.assignmentId(), e.residentId(), "DIET_ASSIGNMENT");
    }

    @Transactional
    public void onAllergyRecorded(AllergyRecorded e) {
        one("ALLERGY_RECORDED:" + e.itemId(), e.residentId(), "ALLERGY");
    }

    @Transactional
    public void onOtherAllergyRecorded(OtherAllergyRecorded e) {
        one("OTHER_ALLERGY_RECORDED:" + e.itemId(), e.residentId(), "ALLERGY");
    }

    @Transactional
    public void onAllergyExcluded(AllergyExcluded e) {
        one("ALLERGY_EXCLUDED:" + e.itemId(), e.residentId(), "ALLERGY");
    }

    @Transactional
    public void onStayMealChanged(StayMealChanged e) {
        one("STAY:" + e.eventKey(), e.residentId(), "FAMILY_STAY");
    }

    @Transactional
    public void onTripMealChanged(TripMealChanged e) {
        many("TRIP:" + e.eventKey(), e.residentIds(), "TRIP");
    }

    private void one(String key, long residentId, String type) {
        many(key, Set.of(residentId), type);
    }

    private void many(String key, Collection<Long> residents, String type) {
        String eventKey = "M08.CLOSING." + key;
        listener.once(eventKey, () -> closing.recompute(Optional.of(residents), eventKey, type, false));
    }

    private void all(String key, String type) {
        String eventKey = "M08.CLOSING." + key;
        listener.once(eventKey, () -> closing.recompute(Optional.empty(), eventKey, type, false));
    }
}
