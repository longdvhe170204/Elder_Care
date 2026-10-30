package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m07.api.FamilyPortalMedicationApi;
import com.cambae.backend.m07.api.FamilySentMedicationApi;
import com.cambae.backend.m07.api.dto.ConfirmedReconciliationView;
import com.cambae.backend.m07.api.dto.FamilyMedicationView;
import com.cambae.backend.m07.api.dto.FamilyPrescriptionView;
import com.cambae.backend.m07.internal.domain.*;
import com.cambae.backend.m07.internal.persistence.FamilyMedicationBatchRepository;
import com.cambae.backend.m07.internal.persistence.MedicationReconciliationRepository;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.persistence.PrescriptionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefaultFamilyPortalMedicationService implements FamilySentMedicationApi, FamilyPortalMedicationApi {

    private final FamilyMedicationBatchRepository batchRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final MedicationReconciliationRepository reconciliationRepo;
    private final MedicationRepository medicationRepo;

    public DefaultFamilyPortalMedicationService(
            FamilyMedicationBatchRepository batchRepo,
            PrescriptionRepository prescriptionRepo,
            MedicationReconciliationRepository reconciliationRepo,
            MedicationRepository medicationRepo) {
        this.batchRepo = batchRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.reconciliationRepo = reconciliationRepo;
        this.medicationRepo = medicationRepo;
    }

    @Override
    public boolean hasRetainedMedication(long residentId) {
        List<FamilyMedicationBatch> activeBatches = batchRepo.findByResidentIdAndStatusIn(
                residentId,
                List.of(
                        FamilyMedicationBatchStatus.PENDING_RECONCILIATION,
                        FamilyMedicationBatchStatus.IN_USE,
                        FamilyMedicationBatchStatus.HOLD_ONLY));
        return !activeBatches.isEmpty();
    }

    @Override
    public List<FamilyMedicationView> byResident(long residentId) {
        List<FamilyMedicationBatch> batches = batchRepo.findByResidentId(residentId);
        return batches.stream()
                .map(b -> {
                    String medName = b.getCustomName();
                    if (b.getMedicationId() != null) {
                        var med = medicationRepo.findById(b.getMedicationId()).orElse(null);
                        if (med != null) {
                            medName = med.getName() + " " + med.getConcentration();
                        }
                    }
                    int remaining = b.getRemainingQuantity() != null
                            ? b.getRemainingQuantity().intValue()
                            : 0;
                    return new FamilyMedicationView(
                            b.getId(),
                            b.getResidentId(),
                            medName != null ? medName : "Family Medication",
                            b.getStatus().name(),
                            remaining,
                            b.getUnit(),
                            b.getExpiryDate());
                })
                .toList();
    }

    @Override
    public List<FamilyMedicationView> sentMedications(long residentId, long familyMemberId) {
        // FR-049: Family member with active relationship views name, status, remaining qty, expiry date without needing
        // consent form
        return byResident(residentId);
    }

    @Override
    public Optional<List<FamilyPrescriptionView>> prescriptions(long residentId, long familyMemberId) {
        // FR-049: Prescriptions displayed on family portal when active relationship / consent exists
        List<Prescription> prescriptions = prescriptionRepo.findByResidentId(residentId);
        List<FamilyPrescriptionView> views = prescriptions.stream()
                .map(p -> {
                    String medName = "Medication #" + p.getMedicationId();
                    var med = medicationRepo.findById(p.getMedicationId()).orElse(null);
                    if (med != null) {
                        medName = med.getName() + " " + med.getConcentration();
                    }
                    LocalDate startDate = p.getStartedAt() != null
                            ? LocalDate.ofInstant(p.getStartedAt(), ZoneId.of("Asia/Ho_Chi_Minh"))
                            : null;
                    return new FamilyPrescriptionView(
                            p.getId(),
                            p.getResidentId(),
                            medName,
                            p.getInstructionNote(),
                            p.getStatus().name(),
                            startDate,
                            p.getEndedAt());
                })
                .toList();
        return Optional.of(views);
    }

    @Override
    public Optional<List<ConfirmedReconciliationView>> confirmedReconciliations(long residentId, long familyMemberId) {
        // FR-049: Confirmed medication reconciliation sheet
        List<MedicationReconciliation> tickets = reconciliationRepo.findByResidentId(residentId);
        List<ConfirmedReconciliationView> confirmed = tickets.stream()
                .filter(t -> t.getStatus() == ReconciliationStatus.CONFIRMED)
                .map(t -> new ConfirmedReconciliationView(
                        t.getId(), t.getResidentId(), t.getReason().name(), t.getConfirmedAt(), t.getConfirmedById()))
                .toList();
        return Optional.of(confirmed);
    }
}
