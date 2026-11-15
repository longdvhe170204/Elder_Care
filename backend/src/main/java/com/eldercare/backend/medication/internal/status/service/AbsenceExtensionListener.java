package com.eldercare.backend.medication.internal.status.service;

import com.eldercare.backend.admission.api.events.AbsenceExtended;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gia hạn lượt vắng của 004 ({@code LuotVangDaGiaHan}; 006 T041, research D-15): dời giờ về dự kiến của khoảng vắng đang
 * mở, để lịch thuốc tính liều trong khoảng gia hạn là Tạm dừng hoặc Mang theo (BR-M07-03, FR-021). Khoảng vắng của chuyến
 * đi (014) không phải lượt vắng của 004 nên bỏ qua.
 */
@Component
public class AbsenceExtensionListener {

    private final MedicationAbsenceIntervalRepository absences;

    public AbsenceExtensionListener(MedicationAbsenceIntervalRepository absences) {
        this.absences = absences;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(AbsenceExtended event) {
        absences.findOpenByResidentId(event.residentId())
                .filter(interval -> interval.getTripId() == null)
                .filter(interval ->
                        interval.getAbsenceSessionId() == null || interval.getAbsenceSessionId() == event.absenceId())
                .ifPresent(interval -> {
                    interval.extend(event.expectedReturnAt());
                    absences.save(interval);
                });
    }
}
