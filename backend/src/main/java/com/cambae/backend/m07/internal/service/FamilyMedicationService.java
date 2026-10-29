package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.events.FamilyMedicationRunningLow;
import com.cambae.backend.m07.internal.domain.*;
import com.cambae.backend.m07.internal.persistence.*;
import com.cambae.backend.m07.internal.service.request.*;
import com.cambae.backend.m07.internal.service.response.FamilyMedicationBatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FamilyMedicationService {

    private final FamilyMedicationBatchRepository batchRepo;
    private final FamilyMedicationPrescriptionRepository batchPrescriptionRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final MedicationRepository medicationRepo;
    private final ResidentMedicationLock residentMedicationLock;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;
    private final BusinessTime businessTime;

    public FamilyMedicationService(
            FamilyMedicationBatchRepository batchRepo,
            FamilyMedicationPrescriptionRepository batchPrescriptionRepo,
            PrescriptionRepository prescriptionRepo,
            MedicationRepository medicationRepo,
            ResidentMedicationLock residentMedicationLock,
            ApplicationEventPublisher events,
            BusinessTime businessTime) {
        this.batchRepo = batchRepo;
        this.batchPrescriptionRepo = batchPrescriptionRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.medicationRepo = medicationRepo;
        this.residentMedicationLock = residentMedicationLock;
        this.events = events;
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
        return toResponse(saved, List.of());
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
     * Return the whole batch to family (FR-041, FR-045, BR-M07-19). Records the quantity counted at return; Q-393 keeps
     * no remaining quantity, so any counted quantity is accepted.
     */
    public FamilyMedicationBatchResponse returnBatch(Long batchId, ReturnBatchBody body, Long actorId) {
        FamilyMedicationBatch batch = closable(batchId, "return");
        batch.markReturned(
                "Returned to " + body.getRecipientName(),
                body.getQuantity(),
                body.getRecipientName(),
                body.getRelationship(),
                actorId,
                body.getReturnedAt() != null ? body.getReturnedAt() : businessTime.now());
        batchRepo.save(batch);
        return getBatch(batchId);
    }

    /** Dispose the whole batch per family request (FR-041, FR-045, BR-M07-19); records counted quantity and witness. */
    public FamilyMedicationBatchResponse disposeBatch(Long batchId, DisposeBatchBody body, Long actorId) {
        FamilyMedicationBatch batch = closable(batchId, "dispose");
        batch.markDisposed(body.getReason(), body.getQuantity(), body.getWitness(), actorId, businessTime.now());
        batchRepo.save(batch);
        return getBatch(batchId);
    }

    /** Ghi dùng hết (FR-041, FR-045, Q-393): lô Được sử dụng chuyển Đã dùng hết và gỡ liên kết đơn. */
    public FamilyMedicationBatchResponse markDepleted(Long batchId, Long actorId) {
        FamilyMedicationBatch batch = inUse(batchId);
        for (FamilyMedicationPrescription link : batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(batchId)) {
            link.unbind(businessTime.now());
            batchPrescriptionRepo.save(link);
        }
        batch.markDepleted(actorId, businessTime.now());
        batchRepo.save(batch);
        return getBatch(batchId);
    }

    /** Báo gia đình sắp hết (FR-046, Q-393): không đổi trạng thái; 009 báo người liên hệ chính, người đại diện. */
    public void notifyRunningLow(Long batchId, String note, Long actorId) {
        FamilyMedicationBatch batch = inUse(batchId);
        events.publishEvent(new FamilyMedicationRunningLow(
                batch.getId(), batch.getResidentId(), medicationName(batch), note, actorId, businessTime.now()));
    }

    /** Đơn còn lô Được sử dụng chưa hết hạn dùng (FR-044): điều kiện xác nhận liều, lần dùng PRN nguồn gia đình gửi. */
    @Transactional(readOnly = true)
    public boolean hasUsableBatch(Long residentId, long prescriptionId) {
        Set<Long> bound = new HashSet<>();
        batchPrescriptionRepo
                .findByPrescriptionIdAndBoundToIsNull(prescriptionId)
                .forEach(l -> bound.add(l.getBatchId()));
        LocalDate today = businessTime.today();
        return batchRepo.findByResidentIdAndStatus(residentId, FamilyMedicationBatchStatus.IN_USE).stream()
                .anyMatch(b -> bound.contains(b.getId())
                        && (b.getExpiryDate() == null || !b.getExpiryDate().isBefore(today)));
    }

    @Transactional(readOnly = true)
    public FamilyMedicationBatchResponse getBatch(Long batchId) {
        return toResponse(find(batchId), boundPrescriptionIds(batchId));
    }

    @Transactional(readOnly = true)
    public List<FamilyMedicationBatchResponse> listBatches(Long residentId) {
        return batchRepo.findByResidentId(residentId).stream()
                .map(b -> toResponse(b, boundPrescriptionIds(b.getId())))
                .toList();
    }

    private List<Long> boundPrescriptionIds(Long batchId) {
        return batchPrescriptionRepo.findByBatchIdAndBoundToIsNull(batchId).stream()
                .map(FamilyMedicationPrescription::getPrescriptionId)
                .toList();
    }

    private FamilyMedicationBatch find(Long batchId) {
        return batchRepo
                .findById(batchId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "NOT_FOUND", "Không tìm thấy lô thuốc gia đình gửi"));
    }

    private FamilyMedicationBatch inUse(Long batchId) {
        FamilyMedicationBatch batch = find(batchId);
        residentMedicationLock.lock(batch.getResidentId());
        if (batch.getStatus() != FamilyMedicationBatchStatus.IN_USE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "Chỉ lô Được sử dụng mới ghi dùng hết hoặc báo sắp hết");
        }
        return batch;
    }

    /** Hoàn trả, Hủy chỉ từ Chờ đối chiếu, Chỉ giữ hộ; lô Được sử dụng phải Ngừng sử dụng trước (11.4). */
    private FamilyMedicationBatch closable(Long batchId, String action) {
        FamilyMedicationBatch batch = find(batchId);
        residentMedicationLock.lock(batch.getResidentId());
        if (batch.getStatus() != FamilyMedicationBatchStatus.PENDING_RECONCILIATION
                && batch.getStatus() != FamilyMedicationBatchStatus.HOLD_ONLY) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATE",
                    "Can only " + action + " batches in PENDING_RECONCILIATION or HOLD_ONLY status");
        }
        return batch;
    }

    private String medicationName(FamilyMedicationBatch batch) {
        if (batch.getMedicationId() != null) {
            var med = medicationRepo.findById(batch.getMedicationId()).orElse(null);
            if (med != null) {
                return med.getName() + " " + med.getConcentration();
            }
        }
        return batch.getCustomName() != null ? batch.getCustomName() : "thuốc gia đình gửi";
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

    private FamilyMedicationBatchResponse toResponse(FamilyMedicationBatch batch, List<Long> boundPrescriptionIds) {
        FamilyMedicationBatchResponse res = new FamilyMedicationBatchResponse();
        res.setId(batch.getId());
        res.setResidentId(batch.getResidentId());
        res.setMedicationId(batch.getMedicationId());
        res.setCustomName(batch.getCustomName());
        res.setCustomActiveIngredient(batch.getCustomActiveIngredient());
        res.setCustomConcentration(batch.getCustomConcentration());
        res.setInitialQuantity(batch.getInitialQuantity());
        res.setUnit(batch.getUnit());
        res.setExpiryDate(batch.getExpiryDate());
        res.setPackagingCondition(batch.getPackagingCondition());
        res.setHasPrescription(batch.isHasPrescription());
        res.setStatus(batch.getStatus().name());
        res.setStatusReason(batch.getStatusReason());
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
        res.setFinalQuantity(batch.getFinalQuantity());
        res.setClosedAt(batch.getClosedAt());
        return res;
    }
}
