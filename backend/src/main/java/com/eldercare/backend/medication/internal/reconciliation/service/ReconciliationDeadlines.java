package com.eldercare.backend.medication.internal.reconciliation.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.api.events.ReconciliationNeeded;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.SuspensionReason;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.MedicationReconciliation;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationReason;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationStatus;
import com.eldercare.backend.medication.internal.reconciliation.persistence.MedicationReconciliationRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ReconciliationDueStore;
import java.time.Instant;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Hạn đối chiếu thuốc (T037, research D-13; FR-034, FR-034a, FR-035, BR-M07-09): lệnh Hoàn tất tiếp nhận, Ghi nhận trở
 * về từ bệnh viện tạm dừng đơn chờ đối chiếu và đặt hạn CFG-M07-05; tới hạn mà chưa có phiếu Đã xác nhận thì yêu cầu
 * cảnh báo mức Trung bình một lần. Hệ thống không tạo phiếu, không tạo dòng (Q-306, Q-307). Liều đã Đến giờ, Trễ giữ
 * nguyên như bảng FR-021; chỉ liều Chưa đến giờ chuyển Tạm dừng.
 */
@Component
public class ReconciliationDeadlines {

    static final String ALERT_TYPE = "CHUA_DOI_CHIEU";

    private static final List<PrescriptionStatus> UNENDED =
            List.of(PrescriptionStatus.ACTIVE, PrescriptionStatus.PENDING_EFFECTIVE);
    private static final List<ReconciliationStatus> OPEN =
            List.of(ReconciliationStatus.OPEN, ReconciliationStatus.PENDING_CONFIRMATION);

    private final PrescriptionRepository prescriptions;
    private final MedicationDoseRepository doses;
    private final MedicationReconciliationRepository tickets;
    private final ReconciliationDueStore due;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public ReconciliationDeadlines(
            PrescriptionRepository prescriptions,
            MedicationDoseRepository doses,
            MedicationReconciliationRepository tickets,
            ReconciliationDueStore due,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.prescriptions = prescriptions;
        this.doses = doses;
        this.tickets = tickets;
        this.due = due;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Hoàn tất tiếp nhận (FR-034a): đã có phiếu "tiếp nhận" Đã xác nhận thì chỉ cho đơn chờ tiếp nhận Hiệu lực. */
    public void completeAdmission(long residentId, Instant at) {
        boolean confirmed = tickets.findByResidentId(residentId).stream()
                .anyMatch(t -> t.getReason() == ReconciliationReason.ADMISSION
                        && t.getStatus() == ReconciliationStatus.CONFIRMED);
        if (!confirmed) {
            begin(residentId, ReconciliationReason.ADMISSION, at);
            return;
        }
        for (Prescription p : prescriptions.findByResidentIdAndStatusIn(residentId, UNENDED)) {
            if (p.isPendingAdmission() && p.getStatus() == PrescriptionStatus.PENDING_EFFECTIVE) {
                p.activateAtAdmission(at);
                prescriptions.save(p);
            }
        }
    }

    /** Tạm dừng đơn chờ đối chiếu, đặt hạn, báo người lập phiếu (FR-034). */
    public void begin(long residentId, ReconciliationReason reason, Instant at) {
        Instant dueAt = at.plus(parameters.read(ParameterCode.CFG_M07_05).asDuration());
        for (Prescription p : prescriptions.findByResidentIdAndStatusIn(residentId, UNENDED)) {
            p.suspend(SuspensionReason.RECONCILIATION_PENDING);
            prescriptions.save(p);
            for (MedicationDose dose : doses.findByPrescriptionIdAndStatusIn(p.getId(), List.of(DoseStatus.UPCOMING))) {
                if (!dose.getScheduledTime().isBefore(at)) {
                    dose.suspend(time.now());
                    doses.save(dose);
                }
            }
        }
        due.start(residentId, dueAt, reason.name());
        for (MedicationReconciliation ticket : tickets.findByResidentIdAndStatusIn(residentId, OPEN)) {
            ticket.assignDeadline(dueAt);
            tickets.save(ticket);
        }
        events.publishEvent(new ReconciliationNeeded(residentId, reason.name(), at));
    }

    /** Phiếu được xác nhận: gỡ hạn, liều Tạm dừng của đơn đã Hiệu lực lại quay về lịch (FR-021, FR-036). */
    public void complete(long residentId) {
        Instant now = time.now();
        due.clear(residentId);
        for (Prescription p :
                prescriptions.findByResidentIdAndStatusIn(residentId, List.of(PrescriptionStatus.ACTIVE))) {
            for (MedicationDose dose :
                    doses.findByPrescriptionIdAndStatusIn(p.getId(), List.of(DoseStatus.SUSPENDED))) {
                if (dose.getEndedAt() == null && dose.getWindowTo().isAfter(now)) {
                    dose.resumeFromSuspension(now);
                    doses.save(dose);
                }
            }
        }
    }

    /** Việc theo phút (BR-M07-09): tới hạn chưa đối chiếu thì cảnh báo một lần và đặt quá hạn của phiếu mở. */
    public void warnOverdue(Instant now) {
        for (long residentId : due.overdueResidentIds(now)) {
            for (MedicationReconciliation ticket : tickets.findByResidentIdAndStatusIn(residentId, OPEN)) {
                ticket.markOverdue(now);
                tickets.save(ticket);
            }
            events.publishEvent(new MedicationAlertRequestedEvent(
                    ALERT_TYPE,
                    "TRUNG_BINH",
                    residentId,
                    "resident:" + residentId,
                    ALERT_TYPE + ":" + residentId + ":" + now.toEpochMilli(),
                    now,
                    List.of()));
            due.markWarned(residentId, now);
        }
    }
}
