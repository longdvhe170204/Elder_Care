package com.eldercare.backend.medication.internal.dose.service;

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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hoãn có chủ đích (BR-M07-26, Q-427). Hoãn không đổi trạng thái hay mốc Bỏ lỡ của liều (BR-M07-02).
 */
@Service
public class DoseWorkflowService {

    private final DoseAdministrationService administration;
    private final MedicationDoseRepository doses;
    private final PrescriptionRepository prescriptions;
    private final ResidentMedicationLock residentLock;
    private final DoseAssigneeResolver assigneeResolver;

    public DoseWorkflowService(
            DoseAdministrationService administration,
            MedicationDoseRepository doses,
            PrescriptionRepository prescriptions,
            ResidentMedicationLock residentLock,
            DoseAssigneeResolver assigneeResolver) {
        this.administration = administration;
        this.doses = doses;
        this.prescriptions = prescriptions;
        this.residentLock = residentLock;
        this.assigneeResolver = assigneeResolver;
    }

    @Transactional
    public DoseDetailView postpone(String doseKey, String reason, Long accountId, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("REASON_REQUIRED", "Lý do hoãn có chủ đích bắt buộc (BR-M07-26)")));
        }
        MedicationDose dose = lockedOpenDose(doseKey, now);
        assigneeResolver.requireAuthorizedNurse(dose.getResidentId(), dose.getFloorAreaId(), accountId, now);
        dose.postpone(accountId, reason.trim(), now);
        doses.save(dose);
        return administration.getDoseDetail(doseKey, now);
    }

    /** Tạm giữ 1 liều vì chỉ số đo bất thường (M07-11): không ngừng cả đơn, không tính Bỏ lỡ. */
    @Transactional
    public DoseDetailView hold(String doseKey, String reason, Long accountId, Instant now) {
        if (reason == null || reason.isBlank()) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("REASON_REQUIRED", "Bắt buộc nhập lý do tạm giữ liều")));
        }
        MedicationDose dose = lockedOpenDose(doseKey, now);
        assigneeResolver.requireAuthorizedNurse(dose.getResidentId(), dose.getFloorAreaId(), accountId, now);
        dose.hold(accountId, reason.trim(), now);
        doses.save(dose);
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
                    "Chỉ liều Đến giờ hoặc Trễ mới Hoãn (BR-M07-26); liều đang ở " + dose.getStatus());
        }
        return dose;
    }
}
