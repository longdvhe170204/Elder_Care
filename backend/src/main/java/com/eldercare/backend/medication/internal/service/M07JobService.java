package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.internal.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.domain.DoseResult;
import com.eldercare.backend.medication.internal.domain.DoseResultType;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.MedicationDose;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.persistence.DoseResultRepository;
import com.eldercare.backend.medication.internal.persistence.M07JobRunStore;
import com.eldercare.backend.medication.internal.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates minute and daily scheduled tasks for Module M07 (research D-19, D-06, D-07, D-08; FR-018 -> FR-023).
 */
@Service
public class M07JobService {

    private static final Logger log = LoggerFactory.getLogger(M07JobService.class);

    private final M07JobRunStore jobRunStore;
    private final PrescriptionRepository prescriptionRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationDoseRepository doseRepository;
    private final DoseResultRepository doseResultRepository;
    private final MedicationScheduleCalculator scheduleCalculator;
    private final DoseAdministrationService doseAdministrationService;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessTime businessTime;
    private final ParameterApi parameters;

    public M07JobService(
            M07JobRunStore jobRunStore,
            PrescriptionRepository prescriptionRepository,
            MedicationRepository medicationRepository,
            MedicationDoseRepository doseRepository,
            DoseResultRepository doseResultRepository,
            MedicationScheduleCalculator scheduleCalculator,
            DoseAdministrationService doseAdministrationService,
            ApplicationEventPublisher eventPublisher,
            BusinessTime businessTime,
            ParameterApi parameters) {
        this.jobRunStore = jobRunStore;
        this.prescriptionRepository = prescriptionRepository;
        this.medicationRepository = medicationRepository;
        this.doseRepository = doseRepository;
        this.doseResultRepository = doseResultRepository;
        this.scheduleCalculator = scheduleCalculator;
        this.doseAdministrationService = doseAdministrationService;
        this.eventPublisher = eventPublisher;
        this.businessTime = businessTime;
        this.parameters = parameters;
    }

    /**
     * Runs minute-level tasks for M07:
     * (1) Transition PENDING_EFFECTIVE -> ACTIVE when startedAt is reached;
     * (2) Transition ACTIVE -> DISCONTINUED when discontinueAt is reached;
     * (3) Materialize doses at window start (DUE / SUSPENDED / LEAVE_MEDICATION);
     * (4) Transition DUE -> LATE (when windowTo expires) and trigger reminder;
     * (5) Transition LATE -> MISSED (after CFG-M07-02 grace period) with SYSTEM recording;
     * (6) Close SUSPENDED doses after window end.
     */
    @Transactional
    public void runMinuteTasks(Instant now) {
        // (1) PENDING_EFFECTIVE -> ACTIVE
        transitionPendingPrescriptions(now);

        // (2) ACTIVE -> DISCONTINUED when discontinueAt is reached
        transitionScheduledSuspensions(now);

        // (3) Materialize doses at window start
        materializeWindowDoses(now);

        // (4) DUE -> LATE
        transitionDueToLate(now);

        // (5) LATE -> MISSED
        transitionLateToMissed(now);

        // (6) Close SUSPENDED doses past window end
        expireSuspendedDoses(now);
    }

    private void transitionPendingPrescriptions(Instant now) {
        List<Prescription> pending =
                prescriptionRepository.findByStatusAndStartedAtLessThanEqual(PrescriptionStatus.PENDING_EFFECTIVE, now);
        for (Prescription p : pending) {
            if (!p.isPendingAdmission()) {
                p.activate();
                prescriptionRepository.save(p);
                log.info("Activated prescription {} at startedAt {}", p.getId(), p.getStartedAt());
            }
        }
    }

    private void transitionScheduledSuspensions(Instant now) {
        List<Prescription> toSuspend =
                prescriptionRepository.findByStatusAndSuspendedFromLessThanEqual(PrescriptionStatus.ACTIVE, now);
        for (Prescription p : toSuspend) {
            p.discontinue();
            prescriptionRepository.save(p);
            // Cancel remaining open doses
            List<MedicationDose> openDoses = doseRepository.findByPrescriptionIdAndStatusIn(
                    p.getId(), List.of(DoseStatus.UPCOMING, DoseStatus.DUE, DoseStatus.LATE, DoseStatus.SUSPENDED));
            for (MedicationDose d : openDoses) {
                d.cancel(DoseCancellationReason.DISCONTINUED, now);
                doseRepository.save(d);
            }
            log.info("Discontinued prescription {} and cancelled open doses at suspendedFrom {}", p.getId(), now);
        }
    }

    private void materializeWindowDoses(Instant now) {
        List<Prescription> activePrescriptions = prescriptionRepository.findByStatus(PrescriptionStatus.ACTIVE);
        Instant horizon = now.plus(30, ChronoUnit.MINUTES);

        for (Prescription p : activePrescriptions) {
            if (p.getPrescriptionType() == PrescriptionType.PRN) {
                continue;
            }
            Medication med = medicationRepository.findById(p.getMedicationId()).orElse(null);
            List<CalculatedDoseSlot> slots = scheduleCalculator.calculate(
                    p, med, now.minus(5, ChronoUnit.MINUTES), horizon, now, ScheduleContext.empty(p.getResidentId()));

            for (CalculatedDoseSlot slot : slots) {
                if (!now.isBefore(slot.windowFrom())) {
                    doseAdministrationService.getOrMaterializeDose(
                            new DoseAdministrationService.ParsedDoseKey(slot.prescriptionId(), slot.scheduledTime()),
                            now,
                            null);
                }
            }
        }
    }

    private void transitionDueToLate(Instant now) {
        List<MedicationDose> dueDoses = doseRepository.findByStatusAndWindowToLessThanEqual(DoseStatus.DUE, now);
        for (MedicationDose d : dueDoses) {
            d.markLate(now);
            if (d.getLateRemindedAt() == null) {
                d.markLateReminded(now);
                log.info("Dose {} transitioned to LATE at window_to {}", d.getId(), d.getWindowTo());
            }
            doseRepository.save(d);
        }
    }

    private void transitionLateToMissed(Instant now) {
        Instant missedThreshold =
                now.minus(parameters.read(ParameterCode.CFG_M07_02).asDuration());
        List<MedicationDose> lateDoses =
                doseRepository.findByStatusAndLateAtLessThanEqual(DoseStatus.LATE, missedThreshold);

        for (MedicationDose d : lateDoses) {
            d.closeMissedBySystem(now);
            doseRepository.save(d);

            DoseResult result = new DoseResult(
                    d.getId(),
                    d.getResidentId(),
                    d.getFloorAreaId(),
                    DoseResultType.MISSED,
                    null,
                    null,
                    null,
                    DoseAuthorityBasis.SYSTEM,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "CFG-M07-02 grace period expired (BR-M07-02)",
                    null,
                    true,
                    false,
                    false,
                    null,
                    businessTime.now());
            doseResultRepository.save(result);

            // Alert missed dose
            eventPublisher.publishEvent(new MedicationAlertRequestedEvent(
                    "BO_LO_LIEU",
                    "TRUNG_BINH",
                    d.getResidentId(),
                    "dose:" + d.getId(),
                    "missed:" + d.getId(),
                    now,
                    List.of()));
            log.warn("Dose {} marked MISSED by SYSTEM after grace period", d.getId());
        }
    }

    private void expireSuspendedDoses(Instant now) {
        List<MedicationDose> suspended =
                doseRepository.findByStatusAndEndedAtIsNullAndWindowToLessThanEqual(DoseStatus.SUSPENDED, now);
        for (MedicationDose d : suspended) {
            d.expireSuspension(now);
            doseRepository.save(d);
            log.info("Suspended dose {} expired after window_to {}", d.getId(), d.getWindowTo());
        }
    }

    /**
     * Runs daily tasks for M07:
     * (1) Expire prescriptions past their ended_at date -> EXPIRED;
     * (2) Cancel open doses for expired prescriptions.
     */
    @Transactional
    public void runDailyTasks(LocalDate today, Instant now) {
        String checkpoint = today.toString();
        if (jobRunStore.isCompleted("DAILY_CHECK", checkpoint)) {
            return;
        }

        // 1. Expire prescriptions past their ended_at date -> EXPIRED (applies to both ACTIVE and SUSPENDED -
        // BR-M07-08)
        List<Prescription> expiredPrescriptions = prescriptionRepository.findExpiredPrescriptions(today);

        for (Prescription p : expiredPrescriptions) {
            p.expire();
            prescriptionRepository.save(p);

            List<MedicationDose> openDoses = doseRepository.findByPrescriptionIdAndStatusIn(
                    p.getId(), List.of(DoseStatus.UPCOMING, DoseStatus.DUE, DoseStatus.LATE, DoseStatus.SUSPENDED));
            for (MedicationDose d : openDoses) {
                d.cancel(DoseCancellationReason.EXPIRED, now);
                doseRepository.save(d);
            }
            log.info("Expired prescription {} on {}", p.getId(), today);
        }

        // 2. Warn of expiring prescriptions within CFG-M07-04 (default 3 days - FR-017)
        LocalDate warningThreshold = today.plusDays(
                parameters.read(ParameterCode.CFG_M07_04).asDuration().toDays());
        List<Prescription> expiringToWarn = prescriptionRepository.findExpiringPrescriptionsToWarn(warningThreshold);
        for (Prescription p : expiringToWarn) {
            p.markExpirationWarned(now);
            prescriptionRepository.save(p);

            eventPublisher.publishEvent(new MedicationAlertRequestedEvent(
                    "DON_SAP_HET_HAN",
                    "NHE",
                    p.getResidentId(),
                    "prescription:" + p.getId(),
                    "expiring:" + p.getId(),
                    now,
                    List.of()));
            log.info("Warned expiring prescription {} ending at {}", p.getId(), p.getEndedAt());
        }

        jobRunStore.recordCompletion("DAILY_CHECK", checkpoint, "COMPLETED", null, now);
    }
}
