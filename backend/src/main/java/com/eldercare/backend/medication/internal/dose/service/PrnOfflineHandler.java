package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.OfflineHandler;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.catalog.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.dose.domain.RecordPrnBody;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.persistence.PrescriptionRepository;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Đồng bộ lần dùng thuốc PRN ngoại tuyến (T045, FR-024, research D-11). */
@Component
public class PrnOfflineHandler implements OfflineHandler<PrnOfflineContent> {

    public static final String RECORD_TYPE = "M07.PRN_ADMINISTRATION";

    private final PrnMedicationService service;
    private final PrescriptionRepository prescriptions;
    private final MedicationRepository medications;
    private final CurrentUser user;

    public PrnOfflineHandler(
            PrnMedicationService service,
            PrescriptionRepository prescriptions,
            MedicationRepository medications,
            CurrentUser user) {
        this.service = service;
        this.prescriptions = prescriptions;
        this.medications = medications;
        this.user = user;
    }

    @Override
    public String recordType() {
        return RECORD_TYPE;
    }

    @Override
    public PermissionKey permissionKey() {
        return new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.PROFESSIONAL);
    }

    @Override
    public Target target(PrnOfflineContent content) {
        return new Target("PRN_ADMINISTRATION", String.valueOf(content.prescriptionId()), content.residentId(), null);
    }

    @Override
    public void validate(PrnOfflineContent content) {
        Prescription prescription =
                prescriptions.findById(content.prescriptionId()).orElse(null);
        if (prescription == null) {
            return;
        }
        Medication medication =
                medications.findById(prescription.getMedicationId()).orElse(null);
        if (medication != null && medication.isSpecialControl()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "REQUIRES_ONLINE",
                    "Thuốc kiểm soát đặc biệt bắt buộc ghi lần dùng PRN trực tuyến (BR-M07-20)");
        }
    }

    @Override
    public String apply(PrnOfflineContent content, SyncOutcome outcome, Instant effectiveAt) {
        RecordPrnBody body = new RecordPrnBody();
        body.setDosage(content.dosage());
        body.setReason(content.reason());
        body.setReportedBy(content.reportedBy());
        body.setAdministeredAt(content.administeredAt());
        body.setOfflineDeviceTime(effectiveAt);
        body.setAdverseReaction(content.adverseReaction());
        body.setNote(content.note());
        body.setRecordedAfterDisconnect(true);
        var saved = service.recordPrn(content.prescriptionId(), body, user.accountId());
        return "prn:" + saved.getId();
    }
}
