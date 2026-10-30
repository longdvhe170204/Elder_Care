package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.internal.domain.*;
import com.cambae.backend.m07.internal.persistence.*;
import com.cambae.backend.m07.internal.service.request.*;
import com.cambae.backend.m07.internal.service.response.FamilyMedicationBatchResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FamilyMedicationService {

    private final FamilyMedicationBatchRepository batchRepo;
    private final FamilyMedicationPrescriptionRepository batchPrescriptionRepo;
    private final FamilyMedicationMovementRepository movementRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final ResidentMedicationLock residentMedicationLock;
    private final ObjectMapper objectMapper;
    private final BusinessTime businessTime;

    public FamilyMedicationService(
            FamilyMedicationBatchRepository batchRepo,
            FamilyMedicationPrescriptionRepository batchPrescriptionRepo,
            FamilyMedicationMovementRepository movementRepo,
            PrescriptionRepository prescriptionRepo,
            ResidentMedicationLock residentMedicationLock,
            BusinessTime businessTime) {
        this.batchRepo = batchRepo;
        this.batchPrescriptionRepo = batchPrescriptionRepo;
        this.movementRepo = movementRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.residentMedicationLock = residentMedicationLock;
        this.objectMapper = new ObjectMapper();
        this.businessTime = businessTime;
    }

    /**
     * Receives a family-sent medication batch (FR-040, FR-041). Initially in PENDING_RECONCILIATION status.
     */
    public FamilyMedicationBatchResponse receiveBatch(Long residentId, ReceiveBatchBody body, Long receiverId) {
        residentMedicationLock.lock(residentId);

        DelivererType delivererType;
        try {
            delivererType = DelivererType.valueOf(body.getDelivererType());
        } catch (Exception e) {
            delivererType = DelivererType.FAMILY_MEMBER;
        }

        String fileIdsJson = null;
        if (body.getFileIds() != null && !body.getFileIds().isEmpty()) {
            try {
                fileIdsJson = objectMapper.writeValueAsString(body.getFileIds());
            } catch (Exception ignored) {
            }
        }

        FamilyMedicationBatch batch = new FamilyMedicationBatch(
                residentId,
                body.getMedicationId(),
                body.getCustomName(),
                body.getCustomActiveIngredient(),
                body.getCustomConcentration(),
                body.getInitialQuantity(),
                body.getInitialQuantity(), // remaining = initial
                body.getUnit(),
                body.getExpiryDate(),
                body.getPackagingCondition(),
                body.isHasPrescription(),
                fileIdsJson,
                delivererType,
                body.getDelivererId(),
                body.getDelivererName(),
                body.getRelationship(),
                body.getPurchaseRequestId(),
                receiverId,
                body.getReceivedAt() != null ? body.getReceivedAt() : businessTime.now(),
                FamilyMedicationBatchStatus.PENDING_RECONCILIATION,
                null,
                businessTime.now());

        FamilyMedicationBatch saved = batchRepo.save(batch);

        // Initial movement record (RECEIPT)
        FamilyMedicationMovement movement = new FamilyMedicationMovement(
                saved.getId(),
                MovementType.RECEIPT,
                saved.getInitialQuantity(),
                saved.getRemainingQuantity(),
                "RECEIPT",
                saved.getId(),
                receiverId,
                businessTime.now(),
                "Family medication received",
                null,
                null,
                null);
        movementRepo.save(movement);

        return toResponse(saved, List.of(), List.of(movement), 0);
    }

    /**
     * Confirms batch use and binds one or more FAMILY_SENT prescriptions (FR-041, FR-043, BR-M07-10, BR-M07-18).
     */
    public FamilyMedicationBatchResponse confirmUse(Long batchId, ConfirmBatchUseBody body, Long actorId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Family-sent medication batch not found"));

        residentMedicationLock.lock(batch.getResidentId());

        if (batch.getStatus() != FamilyMedicationBatchStatus.PENDING_RECONCILIATION
                && batch.getStatus() != FamilyMedicationBatchStatus.HOLD_ONLY) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Medication batch can only be confirmed for use when in PENDING_RECONCILIATION or HOLD_ONLY status");
        }

        if (body.getPackagingRecognizable() == null || !body.getPackagingRecognizable()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PACKAGING_NOT_RECOGNIZABLE",
                    "Medication packaging information cannot be recognized");
        }

        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(businessTime.today())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "EXPIRED_MEDICATION", "Medication is expired, cannot be approved for use");
        }

        Long medicationId = body.getMedicationId() != null ? body.getMedicationId() : batch.getMedicationId();
        if (medicationId == null) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "MEDICATION_REQUIRED",
                    "Custom batch requires selecting catalog medication when confirming for use");
        }

        // Verify bound prescriptions
        List<Prescription> prescriptions = prescriptionRepo.findAllById(body.getPrescriptionIds());
        if (prescriptions.isEmpty()
                || prescriptions.size() != body.getPrescriptionIds().size()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PRESCRIPTION", "Prescription does not exist");
        }

        for (Prescription p : prescriptions) {
            if (!p.getResidentId().equals(batch.getResidentId())) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "RESIDENT_MISMATCH",
                        "Prescription does not belong to this resident");
            }
            if (!p.getMedicationId().equals(medicationId)) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "MEDICATION_MISMATCH",
                        "Prescription medication does not match batch medication");
            }
        }

        batch.markInUse(medicationId, businessTime.now());
        batchRepo.save(batch);

        // Bind prescriptions
        for (Long pId : body.getPrescriptionIds()) {
            FamilyMedicationPrescription link =
                    new FamilyMedicationPrescription(batch.getId(), pId, businessTime.now());
            batchPrescriptionRepo.save(link);
        }

        return getBatch(batchId);
    }

    /**
     * Hold only (FR-041, BR-M07-10).
     */
    public FamilyMedicationBatchResponse holdOnly(Long batchId, String reason, Long actorId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Family-sent medication batch not found"));

        residentMedicationLock.lock(batch.getResidentId());

        if (batch.getStatus() != FamilyMedicationBatchStatus.PENDING_RECONCILIATION
                && batch.getStatus() != FamilyMedicationBatchStatus.IN_USE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Only batches in PENDING_RECONCILIATION or IN_USE can transition to HOLD_ONLY");
        }

        // Unbind active prescriptions
        List<FamilyMedicationPrescription> activeLinks = batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(batchId);
        for (FamilyMedicationPrescription link : activeLinks) {
            link.unbind(businessTime.now());
            batchPrescriptionRepo.save(link);
        }

        batch.markHoldOnly(reason != null ? reason : "Transitioned to hold only", businessTime.now());
        batchRepo.save(batch);

        return getBatch(batchId);
    }

    /**
     * Stop use (transition to HOLD_ONLY and unbind prescriptions) (FR-041).
     */
    public FamilyMedicationBatchResponse stopUse(Long batchId, String reason, Long actorId) {
        return holdOnly(batchId, reason, actorId);
    }

    /**
     * Adjust inventory quantity (FR-045).
     */
    public FamilyMedicationBatchResponse adjustInventory(Long batchId, AdjustInventoryBody body, Long actorId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Family-sent medication batch not found"));

        residentMedicationLock.lock(batch.getResidentId());

        BigDecimal diff = body.getActualQuantity().subtract(batch.getRemainingQuantity());
        batch.updateRemainingQuantity(body.getActualQuantity(), businessTime.now());
        batchRepo.save(batch);

        FamilyMedicationMovement movement = new FamilyMedicationMovement(
                batch.getId(),
                MovementType.INVENTORY_ADJUSTMENT,
                diff,
                body.getActualQuantity(),
                "INVENTORY_ADJUSTMENT",
                null,
                actorId,
                businessTime.now(),
                body.getReason(),
                null,
                null,
                null);
        movementRepo.save(movement);

        return getBatch(batchId);
    }

    /**
     * Return medication to family (FR-041, FR-045, BR-M07-19).
     * Requires batch in PENDING_RECONCILIATION or HOLD_ONLY; return quantity must equal exactly the remaining quantity.
     */
    public FamilyMedicationBatchResponse returnBatch(Long batchId, ReturnBatchBody body, Long actorId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Family-sent medication batch not found"));

        residentMedicationLock.lock(batch.getResidentId());

        if (batch.getStatus() != FamilyMedicationBatchStatus.PENDING_RECONCILIATION
                && batch.getStatus() != FamilyMedicationBatchStatus.HOLD_ONLY) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Can only return batches in PENDING_RECONCILIATION or HOLD_ONLY status");
        }

        if (body.getQuantity().compareTo(batch.getRemainingQuantity()) != 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "QUANTITY_DIFFERS_FROM_REMAINING",
                    "Return quantity must exactly match remaining quantity (" + batch.getRemainingQuantity() + ")");
        }

        FamilyMedicationMovement movement = new FamilyMedicationMovement(
                batch.getId(),
                MovementType.RETURN,
                body.getQuantity().negate(),
                BigDecimal.ZERO,
                "RETURN",
                null,
                actorId,
                body.getReturnedAt() != null ? body.getReturnedAt() : businessTime.now(),
                "Returned to family",
                null,
                body.getRecipientName(),
                body.getRelationship());
        movementRepo.save(movement);

        batch.markReturned("Returned to " + body.getRecipientName(), businessTime.now());
        batchRepo.save(batch);

        return getBatch(batchId);
    }

    /**
     * Dispose batch per family request (FR-041, FR-045, BR-M07-19).
     * Requires witness and reason; disposal quantity must equal exactly the remaining quantity.
     */
    public FamilyMedicationBatchResponse disposeBatch(Long batchId, DisposeBatchBody body, Long actorId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Family-sent medication batch not found"));

        residentMedicationLock.lock(batch.getResidentId());

        if (batch.getStatus() != FamilyMedicationBatchStatus.PENDING_RECONCILIATION
                && batch.getStatus() != FamilyMedicationBatchStatus.HOLD_ONLY) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Can only dispose batches in PENDING_RECONCILIATION or HOLD_ONLY status");
        }

        if (body.getQuantity().compareTo(batch.getRemainingQuantity()) != 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "QUANTITY_DIFFERS_FROM_REMAINING",
                    "Disposal quantity must exactly match remaining quantity (" + batch.getRemainingQuantity() + ")");
        }

        FamilyMedicationMovement movement = new FamilyMedicationMovement(
                batch.getId(),
                MovementType.DISPOSAL,
                body.getQuantity().negate(),
                BigDecimal.ZERO,
                "DISPOSAL",
                null,
                actorId,
                businessTime.now(),
                body.getReason(),
                body.getWitness(),
                null,
                null);
        movementRepo.save(movement);

        batch.markDisposed(body.getReason(), businessTime.now());
        batchRepo.save(batch);

        return getBatch(batchId);
    }

    /**
     * Records inventory deduction when administering a dose or PRN (BR-M07-10, BR-M07-11).
     */
    public void recordUsage(
            Long batchId, BigDecimal quantity, MovementType type, String sourceType, Long sourceId, Long actorId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Family-sent medication batch not found"));

        if (batch.getStatus() != FamilyMedicationBatchStatus.IN_USE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BATCH_NOT_RECONCILED",
                    "Family medication batch has not been reconciled and confirmed for use");
        }

        if (batch.getRemainingQuantity().compareTo(quantity) < 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INSUFFICIENT_QUANTITY",
                    "Remaining medication is insufficient (available: " + batch.getRemainingQuantity() + ", needed: "
                            + quantity + ")");
        }

        BigDecimal newRemaining = batch.getRemainingQuantity().subtract(quantity);
        batch.updateRemainingQuantity(newRemaining, businessTime.now());
        batchRepo.save(batch);

        FamilyMedicationMovement movement = new FamilyMedicationMovement(
                batch.getId(),
                type,
                quantity.negate(),
                newRemaining,
                sourceType,
                sourceId,
                actorId,
                businessTime.now(),
                "Deducted for medication administration",
                null,
                null,
                null);
        movementRepo.save(movement);
    }

    @Transactional(readOnly = true)
    public FamilyMedicationBatchResponse getBatch(Long batchId) {
        FamilyMedicationBatch batch = batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy lô thuốc gia đình gửi"));

        List<FamilyMedicationPrescription> activeLinks = batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(batchId);
        List<Long> boundPrescriptionIds = activeLinks.stream()
                .map(FamilyMedicationPrescription::getPrescriptionId)
                .toList();
        List<FamilyMedicationMovement> movements = movementRepo.findByBatchIdOrderByMovedAtDesc(batchId);

        int daysRemaining = calculateDaysRemaining(batch, boundPrescriptionIds);

        return toResponse(batch, boundPrescriptionIds, movements, daysRemaining);
    }

    @Transactional(readOnly = true)
    public List<FamilyMedicationBatchResponse> listBatches(Long residentId) {
        List<FamilyMedicationBatch> batches = batchRepo.findByResidentId(residentId);
        return batches.stream()
                .map(b -> {
                    List<FamilyMedicationPrescription> activeLinks =
                            batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(b.getId());
                    List<Long> boundPrescriptionIds = activeLinks.stream()
                            .map(FamilyMedicationPrescription::getPrescriptionId)
                            .toList();
                    int daysRemaining = calculateDaysRemaining(b, boundPrescriptionIds);
                    return toResponse(b, boundPrescriptionIds, List.of(), daysRemaining);
                })
                .toList();
    }

    /**
     * Calculates days of medication remaining (BR-M07-11) based on total daily dose of bound prescriptions.
     */
    public int calculateDaysRemaining(FamilyMedicationBatch batch, List<Long> boundPrescriptionIds) {
        if (batch.getStatus() != FamilyMedicationBatchStatus.IN_USE
                || batch.getRemainingQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        if (boundPrescriptionIds == null || boundPrescriptionIds.isEmpty()) {
            return 0;
        }

        List<Prescription> boundPrescriptions = prescriptionRepo.findAllById(boundPrescriptionIds);
        BigDecimal totalDailyDose = BigDecimal.ZERO;

        for (Prescription p : boundPrescriptions) {
            if (p.getStatus() != PrescriptionStatus.ACTIVE) {
                continue;
            }

            BigDecimal doseAmount = p.getDosage() != null ? p.getDosage() : BigDecimal.ONE;
            int timesPerDay = 1;

            if (p.getPrescriptionType() == PrescriptionType.PERIODIC) {
                if (p.getFrequencyType() == FrequencyType.TIMED_SLOTS && p.getTimedSlots() != null) {
                    try {
                        List<String> slots = objectMapper.readValue(p.getTimedSlots(), new TypeReference<>() {});
                        timesPerDay = Math.max(1, slots.size());
                    } catch (Exception ignored) {
                    }
                } else if (p.getFrequencyType() == FrequencyType.EVERY_N_HOURS
                        && p.getEveryNHours() != null
                        && p.getEveryNHours() > 0) {
                    timesPerDay = Math.max(1, 24 / p.getEveryNHours());
                }
            } else if (p.getPrescriptionType() == PrescriptionType.PRN) {
                timesPerDay = p.getMaxDosesPer24h() != null && p.getMaxDosesPer24h() > 0 ? p.getMaxDosesPer24h() : 1;
            }

            BigDecimal dailyForPrescription = doseAmount.multiply(BigDecimal.valueOf(timesPerDay));
            totalDailyDose = totalDailyDose.add(dailyForPrescription);
        }

        if (totalDailyDose.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        return batch.getRemainingQuantity()
                .divide(totalDailyDose, 0, RoundingMode.FLOOR)
                .intValue();
    }

    /** Danh sách id tệp lưu dạng JSON ở cột file_ids. */
    private List<Long> fileIds(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, Long[].class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private FamilyMedicationBatchResponse toResponse(
            FamilyMedicationBatch batch,
            List<Long> boundPrescriptionIds,
            List<FamilyMedicationMovement> movements,
            int daysRemaining) {

        FamilyMedicationBatchResponse res = new FamilyMedicationBatchResponse();
        res.setId(batch.getId());
        res.setResidentId(batch.getResidentId());
        res.setMedicationId(batch.getMedicationId());
        res.setCustomName(batch.getCustomName());
        res.setCustomActiveIngredient(batch.getCustomActiveIngredient());
        res.setCustomConcentration(batch.getCustomConcentration());
        res.setInitialQuantity(batch.getInitialQuantity());
        res.setRemainingQuantity(batch.getRemainingQuantity());
        res.setUnit(batch.getUnit());
        res.setExpiryDate(batch.getExpiryDate());
        res.setPackagingCondition(batch.getPackagingCondition());
        res.setHasPrescription(batch.isHasPrescription());
        res.setStatus(batch.getStatus().name());
        res.setStatusReason(batch.getStatusReason());
        res.setDaysRemaining(daysRemaining);
        res.setBoundPrescriptionIds(boundPrescriptionIds);
        res.setDelivererType(
                batch.getDelivererType() == null
                        ? null
                        : batch.getDelivererType().name());
        res.setDelivererId(batch.getDelivererId());
        res.setDelivererName(batch.getDelivererName());
        res.setRelationship(batch.getRelationship());
        res.setPurchaseRequestId(batch.getPurchaseRequestId());
        res.setReceivedAt(batch.getReceivedAt());
        res.setFileIds(fileIds(batch.getFileIds()));

        List<FamilyMedicationBatchResponse.MovementResponse> movementResponses = movements.stream()
                .map(m -> {
                    FamilyMedicationBatchResponse.MovementResponse mr =
                            new FamilyMedicationBatchResponse.MovementResponse();
                    mr.setId(m.getId());
                    mr.setMovementType(m.getMovementType().name());
                    mr.setQuantity(m.getQuantity());
                    mr.setRemainingAfter(m.getRemainingAfter());
                    mr.setMovedAt(m.getMovedAt());
                    mr.setReason(m.getReason());
                    mr.setActorId(m.getActorId());
                    return mr;
                })
                .toList();

        res.setMovements(movementResponses);
        return res;
    }
}
