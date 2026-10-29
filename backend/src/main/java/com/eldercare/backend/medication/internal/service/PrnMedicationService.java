package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.domain.PrnAdministration;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.persistence.PrnAdministrationRepository;
import com.eldercare.backend.medication.internal.persistence.ResidentMedicationLock;
import com.eldercare.backend.medication.internal.service.request.RecordPrnBody;
import com.eldercare.backend.medication.internal.service.response.PrnAdministrationResponse;
import com.eldercare.backend.medication.internal.service.response.PrnHistoryResponse;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business service for PRN medication administrations with interval and 24-hour rolling limit checks (FR-024, FR-025, FR-025a, BR-M07-04).
 */
@Service
public class PrnMedicationService {

    private final PrnAdministrationRepository prnRepo;
    private final PrescriptionRepository prescriptionRepo;
    private final ResidentMedicationLock residentMedicationLock;
    private final BusinessTime businessTime;
    private final FamilyMedicationService familyMedications;

    @Autowired
    public PrnMedicationService(
            PrnAdministrationRepository prnRepo,
            PrescriptionRepository prescriptionRepo,
            ResidentMedicationLock residentMedicationLock,
            BusinessTime businessTime,
            FamilyMedicationService familyMedications) {
        this.prnRepo = prnRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.residentMedicationLock = residentMedicationLock;
        this.businessTime = businessTime;
        this.familyMedications = familyMedications;
    }

    @Transactional
    public PrnAdministrationResponse recordPrn(Long prescriptionId, RecordPrnBody body, Long currentUserId) {

        Prescription prescription = prescriptionRepo.findById(prescriptionId).orElseThrow(NotFoundException::new);
        residentMedicationLock.lock(prescription.getResidentId());

        if (prescription.getPrescriptionType() != PrescriptionType.PRN) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_PRESCRIPTION_TYPE",
                    "Prescription is not a PRN (as-needed) type");
        }

        if (prescription.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_STATE", "PRN prescription is not in ACTIVE status");
        }

        // FR-044 (Q-393): nguồn gia đình gửi cần còn lô Được sử dụng chưa hết hạn; lần ghi theo báo lại (đang vắng)
        // không áp
        if (prescription.getMedicationSource() == MedicationSource.FAMILY_SENT
                && body.getReportedBy() == null
                && !familyMedications.hasUsableBatch(prescription.getResidentId(), prescriptionId)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NO_MEDICATION_AVAILABLE",
                    "Đơn không còn lô thuốc gia đình gửi Được sử dụng chưa hết hạn dùng (FR-044)");
        }

        if (body.getReason() == null || body.getReason().isBlank()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "PRN reason is required (FR-024)");
        }

        if (body.getDosage() == null || body.getDosage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "PRN dosage must be greater than 0");
        }

        if (prescription.getDosage() != null && body.getDosage().compareTo(prescription.getDosage()) > 0) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "EXCEEDS_PRESCRIBED_DOSE",
                    "Dosage (" + body.getDosage() + ") exceeds maximum single dose in prescription ("
                            + prescription.getDosage() + ")");
        }

        Instant now = businessTime.now();
        // Thời điểm dùng do người ghi nhập (api-006 administeredAt); bản ngoại tuyến dùng giờ thiết bị
        Instant adminTime = body.getAdministeredAt() != null
                ? body.getAdministeredAt()
                : body.getOfflineDeviceTime() != null ? body.getOfflineDeviceTime() : now;

        // Collect all prescription IDs in the replacement chain (BR-M07-04)
        Set<Long> chainPrescriptionIds = getReplacementChainIds(prescription);

        // Check PRN limits
        Instant earliestAllowed = null;
        boolean limitExceeded = false;

        // 1. Minimum interval between doses (minIntervalMinutes)
        Integer minInterval = prescription.getMinIntervalMinutes();
        if (minInterval != null && minInterval > 0) {
            List<PrnAdministration> recentList =
                    prnRepo.findInWindow(chainPrescriptionIds, adminTime.minus(Duration.ofMinutes(minInterval)));
            if (!recentList.isEmpty()) {
                PrnAdministration mostRecent = recentList.get(0);
                Instant minAvailable = mostRecent.getAdministeredAt().plus(Duration.ofMinutes(minInterval));
                if (minAvailable.isAfter(adminTime)) {
                    limitExceeded = true;
                    earliestAllowed = minAvailable;
                }
            }
        }

        // 2. Maximum doses in rolling 24-hour window (maxDosesPer24h)
        Integer maxDoses = prescription.getMaxDosesPer24h();
        if (maxDoses != null && maxDoses > 0) {
            Instant window24hStart = adminTime.minus(Duration.ofHours(24));
            List<PrnAdministration> list24h = prnRepo.findInWindow(chainPrescriptionIds, window24hStart);
            if (list24h.size() >= maxDoses) {
                limitExceeded = true;
                // Expiry time of the oldest dose within the max-dose window
                PrnAdministration oldestInMaxWindow = list24h.get(maxDoses - 1);
                Instant maxAvailable = oldestInMaxWindow.getAdministeredAt().plus(Duration.ofHours(24));
                if (earliestAllowed == null || maxAvailable.isAfter(earliestAllowed)) {
                    earliestAllowed = maxAvailable;
                }
            }
        }

        if (limitExceeded) {
            if (body.isReported()) {
                // Saved but flagged limitExceeded = true (FR-027)
            } else {
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "PRN_LIMIT_EXCEEDED",
                        "Exceeded PRN limit. Earliest allowed time: " + earliestAllowed,
                        earliestAllowed != null
                                ? Map.of(
                                        "earliestAllowedAt",
                                        earliestAllowed.toString(),
                                        "earliestAllowed",
                                        earliestAllowed.toString())
                                : Map.of());
            }
        }

        PrnAdministration prn = new PrnAdministration(
                prescriptionId,
                prescription.getResidentId(),
                adminTime,
                body.getDosage(),
                prescription.getDosageUnit(),
                body.getReason(),
                currentUserId,
                body.getReportedBy(),
                limitExceeded,
                body.getOfflineDeviceTime(),
                body.getAdverseReaction(),
                body.getNote(),
                body.isRecordedAfterDisconnect(),
                businessTime.now());

        PrnAdministration saved = prnRepo.save(prn);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrnHistoryResponse getPrnHistory(Long prescriptionId) {
        Prescription prescription = prescriptionRepo.findById(prescriptionId).orElseThrow(NotFoundException::new);
        Set<Long> chainPrescriptionIds = getReplacementChainIds(prescription);

        List<PrnAdministration> administrations =
                prnRepo.findByPrescriptionIdInOrderByAdministeredAtDesc(chainPrescriptionIds);

        Instant now = businessTime.now();
        Instant earliestAllowed = null;

        // Calculate earliest next allowed administration time
        Integer minInterval = prescription.getMinIntervalMinutes();
        if (minInterval != null && minInterval > 0 && !administrations.isEmpty()) {
            PrnAdministration mostRecent = administrations.get(0);
            Instant minAvailable = mostRecent.getAdministeredAt().plus(Duration.ofMinutes(minInterval));
            if (minAvailable.isAfter(now)) {
                earliestAllowed = minAvailable;
            }
        }

        Integer maxDoses = prescription.getMaxDosesPer24h();
        if (maxDoses != null && maxDoses > 0) {
            Instant window24hStart = now.minus(Duration.ofHours(24));
            List<PrnAdministration> list24h = administrations.stream()
                    .filter(a -> !a.getAdministeredAt().isBefore(window24hStart))
                    .toList();
            if (list24h.size() >= maxDoses) {
                PrnAdministration oldestInMax = list24h.get(maxDoses - 1);
                Instant maxAvailable = oldestInMax.getAdministeredAt().plus(Duration.ofHours(24));
                if (earliestAllowed == null || maxAvailable.isAfter(earliestAllowed)) {
                    earliestAllowed = maxAvailable;
                }
            }
        }

        PrnHistoryResponse res = new PrnHistoryResponse();
        res.setPrescriptionId(prescriptionId);
        res.setAdministrations(administrations.stream().map(this::toResponse).toList());
        res.setEarliestNextAllowedTime(earliestAllowed);
        res.setCanAdministerNow(earliestAllowed == null || !earliestAllowed.isAfter(now));
        return res;
    }

    private Set<Long> getReplacementChainIds(Prescription p) {
        Set<Long> ids = new HashSet<>();
        ids.add(p.getId());

        // Traverse backward through replacement chain (previous prescriptions)
        Long currentReplacesId = p.getReplacesId();
        while (currentReplacesId != null) {
            ids.add(currentReplacesId);
            currentReplacesId = prescriptionRepo
                    .findById(currentReplacesId)
                    .map(Prescription::getReplacesId)
                    .orElse(null);
        }

        return ids;
    }

    private PrnAdministrationResponse toResponse(PrnAdministration a) {
        PrnAdministrationResponse res = new PrnAdministrationResponse();
        res.setId(a.getId());
        res.setPrescriptionId(a.getPrescriptionId());
        res.setResidentId(a.getResidentId());
        res.setAdministeredAt(a.getAdministeredAt());
        res.setDosage(a.getDosage());
        res.setDosageUnit(a.getDosageUnit());
        res.setReason(a.getReason());
        res.setAdministeredById(a.getAdministeredById());
        res.setReportedBy(a.getReportedBy());
        res.setExceededLimit(a.isExceededLimit());
        res.setAdverseReaction(a.getAdverseReaction());
        res.setNote(a.getNote());
        res.setRecordedAfterDisconnect(a.isRecordedAfterDisconnect());
        return res;
    }
}
