package com.eldercare.backend.medication.internal.prescription.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Nội dung dòng {@code record_event} loại {@code M07.PRESCRIPTION_BASIS} (data-model 006 mục 0.1, BR-M07-16). */
public record PrescriptionBasisContent(
        Long prescriptionId,
        PrescriptionBasisCommand command,
        String facilityName,
        String prescriberName,
        LocalDate prescribedDate,
        PrescriptionBasisType basisType,
        String callerName,
        String receiverName,
        Instant calledAt,
        Long documentFileId,
        Long recorderId,
        Instant recordedAt,
        Map<String, Object> legacy) {

    public static final SharedKind<PrescriptionBasisContent> KIND = SharedKind.of(
                    "M07.PRESCRIPTION_BASIS", PrescriptionBasisContent.class)
            .notDeletable();

    public static PrescriptionBasisContent of(PrescriptionBasis b) {
        return new PrescriptionBasisContent(
                b.getPrescriptionId(),
                b.getCommand(),
                b.getFacilityName(),
                b.getPrescriberName(),
                b.getPrescribedDate(),
                b.getBasisType(),
                b.getCallerName(),
                b.getReceiverName(),
                b.getCalledAt(),
                b.getDocumentFileId(),
                b.getRecorderId(),
                b.getRecordedAt(),
                null);
    }

    public PrescriptionBasis toDomain(long id) {
        PrescriptionBasis basis = new PrescriptionBasis(
                prescriptionId,
                command,
                facilityName,
                prescriberName,
                prescribedDate,
                basisType,
                callerName,
                receiverName,
                calledAt,
                documentFileId,
                recorderId,
                recordedAt,
                recordedAt);
        basis.assignId(id);
        return basis;
    }
}
