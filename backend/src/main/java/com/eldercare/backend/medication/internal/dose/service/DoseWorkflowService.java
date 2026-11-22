package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.medication.internal.dose.domain.DoseDetailView;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.dose.domain.MedicationDose;
import com.eldercare.backend.medication.internal.dose.persistence.MedicationDoseRepository;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ResidentMedicationLock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hoãn có chủ đích và Nhận liều (BR-M07-26, Q-426, Q-427). Hoãn không đổi trạng thái hay mốc Bỏ lỡ của liều (BR-M07-02);
 * Nhận liều không đổi trạng thái liều và tự hết sau CFG-M07-08.
 */
@Service
public class DoseWorkflowService {

    private final DoseAdministrationService administration;
    private final MedicationDoseRepository doses;
    private final PrescriptionRepository prescriptions;
    private final ResidentMedicationLock residentLock;
    private final ParameterApi parameters;

    public DoseWorkflowService(
            DoseAdministrationService administration,
            MedicationDoseRepository doses,
            PrescriptionRepository prescriptions,
            ResidentMedicationLock residentLock,
            ParameterApi parameters) {
        this.administration = administration;
        this.doses = doses;
        this.prescriptions = prescriptions;
        this.residentLock = residentLock;
        this.parameters = parameters;
    }

    @Transactional
    public DoseDetailView postpone(String doseKey, String reason, Long accountId, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("REASON_REQUIRED", "Lý do hoãn có chủ đích bắt buộc (BR-M07-26)")));
        }
        MedicationDose dose = lockedOpenDose(doseKey, now);
        rejectIfClaimedByOther(dose, accountId, now);
        dose.postpone(accountId, reason.trim(), now);
        doses.save(dose);
        return administration.getDoseDetail(doseKey, now);
    }

    @Transactional
    public DoseDetailView claim(String doseKey, Long accountId, Instant now) {
        MedicationDose dose = lockedOpenDose(doseKey, now);
        rejectIfClaimedByOther(dose, accountId, now);
        dose.claim(accountId, now.plus(parameters.read(ParameterCode.CFG_M07_08).asDuration()), now);
        doses.save(dose);
        return administration.getDoseDetail(doseKey, now);
    }

    @Transactional
    public DoseDetailView release(String doseKey, Long accountId, Instant now) {
        MedicationDose dose = lockedOpenDose(doseKey, now);
        rejectIfClaimedByOther(dose, accountId, now);
        if (dose.getClaimedById() != null) {
            dose.clearClaim(now);
            doses.save(dose);
        }
        return administration.getDoseDetail(doseKey, now);
    }

    /** Liều đã vật hóa ở Đến giờ hoặc Trễ; liều khác trả 409 INVALID_TRANSITION. */
    private MedicationDose lockedOpenDose(String doseKey, Instant now) {
        DoseAdministrationService.ParsedDoseKey parsed = administration.parseDoseKey(doseKey);
        residentLock.lock(prescriptions
                .findById(parsed.prescriptionId())
                .map(Prescription::getResidentId)
                .orElse(0L));
        MedicationDose dose = administration.getOrMaterializeDose(parsed, now, null);
        if (dose.getStatus() != DoseStatus.DUE && dose.getStatus() != DoseStatus.LATE) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_TRANSITION",
                    "Chỉ liều Đến giờ hoặc Trễ mới Hoãn, Nhận liều (BR-M07-26); liều đang ở " + dose.getStatus());
        }
        return dose;
    }

    private static void rejectIfClaimedByOther(MedicationDose dose, Long accountId, Instant now) {
        if (!dose.isClaimActive(now) || dose.getClaimedById().equals(accountId)) {
            return;
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("claimedBy", dose.getClaimedById().toString());
        properties.put("claimExpiresAt", dose.getClaimExpiresAt());
        throw new BusinessException(
                HttpStatus.CONFLICT, "DOSE_CLAIMED", "Liều đã có Điều dưỡng nhận (BR-M07-26)", properties);
    }
}
