package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.events.AllergyRecordedEvent;
import com.cambae.backend.m07.api.events.MedicationAlertRequestedEvent;
import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens for new allergy recorded events (FR-012, BR-M01-07, BR-M07-06).
 * Checks all active, suspended, and pending effective prescriptions for the resident.
 * For each conflict, fires MedicationAlertRequestedEvent(XUNG_DOT_DI_UNG, TRUNG_BINH); prescriptions do not auto-stop.
 * Then calls healthAllergyPort.markAllergyChecked(allergyId, "THUOC").
 * Idempotent per allergyId.
 */
@Component
public class PrescriptionAllergyListener {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionAllergyListener.class);

    private final PrescriptionRepository prescriptionRepository;
    private final HealthAllergyPort healthAllergyPort;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessTime time;
    private final Set<Long> processedAllergies = ConcurrentHashMap.newKeySet();

    public PrescriptionAllergyListener(
            PrescriptionRepository prescriptionRepository,
            HealthAllergyPort healthAllergyPort,
            ApplicationEventPublisher eventPublisher,
            BusinessTime time) {
        this.prescriptionRepository = prescriptionRepository;
        this.healthAllergyPort = healthAllergyPort;
        this.eventPublisher = eventPublisher;
        this.time = time;
    }

    @EventListener
    @Transactional
    public void onAllergyRecorded(AllergyRecordedEvent event) {
        checkAndAlert(event.allergyId(), event.residentId(), event.allergenId());
    }

    public void checkAndAlert(long allergyId, long residentId, long allergenId) {
        if (!processedAllergies.add(allergyId)) {
            log.info(
                    "Allergy {} has already been processed for resident {}, skipping (idempotent)",
                    allergyId,
                    residentId);
            return;
        }

        List<Prescription> unended = prescriptionRepository.findUnendedByResidentId(residentId);
        int conflictCount = 0;

        for (Prescription p : unended) {
            if (p.getAllergenIds() != null && p.getAllergenIds().contains(allergenId)) {
                conflictCount++;
                log.warn(
                        "Prescription {} conflicts with new allergy {} (allergen {}) for resident {}",
                        p.getId(),
                        allergyId,
                        allergenId,
                        residentId);

                MedicationAlertRequestedEvent alert = new MedicationAlertRequestedEvent(
                        "XUNG_DOT_DI_UNG",
                        "TRUNG_BINH",
                        residentId,
                        "prescription:" + p.getId(),
                        "ALLERGY:" + allergyId + ":PRESCRIPTION:" + p.getId(),
                        time.now(),
                        Collections.emptyList());
                eventPublisher.publishEvent(alert);
            }
        }

        log.info(
                "Checked allergy {} for resident {}: found {} conflicting prescriptions",
                allergyId,
                residentId,
                conflictCount);

        healthAllergyPort.markAllergyChecked(allergyId, "THUOC");
    }
}
