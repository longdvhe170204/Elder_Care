package com.eldercare.backend.medication.web;

import com.eldercare.backend.medication.internal.service.request.ChangeDosageBody;
import com.eldercare.backend.medication.internal.service.request.ConfirmedIssueBody;
import com.eldercare.backend.medication.internal.service.request.CreatePrescriptionBody;
import com.eldercare.backend.medication.internal.service.request.DiscontinuePrescriptionBody;
import com.eldercare.backend.medication.internal.service.request.ExtendPrescriptionBody;
import com.eldercare.backend.medication.internal.service.request.ExternalBasisBody;
import com.eldercare.backend.medication.internal.service.request.FrequencyBody;
import com.eldercare.backend.medication.internal.service.request.OrderPrescriptionBody;
import com.eldercare.backend.medication.internal.service.request.PrescriberBody;
import com.eldercare.backend.medication.internal.service.request.ReconciliationItemBody;
import com.eldercare.backend.medication.web.request.ExternalBasisRequest;
import com.eldercare.backend.medication.web.request.NewPrescriptionRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.ChangeDosageRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.DiscontinueRequest;
import com.eldercare.backend.medication.web.request.PrescriptionChangeRequests.ExtendRequest;
import com.eldercare.backend.medication.web.request.PrescriptionCommandRequest;
import com.eldercare.backend.medication.web.request.ReconciliationRequests.ReconciliationItemRequest;

/** Chuyển thân request theo api-006 sang lệnh của service m07 (tách lớp web và service, R-02 của 000). */
final class M07RequestMapper {

    private M07RequestMapper() {}

    static CreatePrescriptionBody prescription(NewPrescriptionRequest r) {
        CreatePrescriptionBody b = new CreatePrescriptionBody();
        b.setMedicationId(r.medicationId());
        b.setDosage(r.dosage());
        b.setRoute(r.route());
        b.setPrescriptionType(r.prescriptionType().name());
        if (r.frequency() != null) {
            FrequencyBody f = new FrequencyBody();
            f.setType(r.frequency().type().name());
            f.setTimedSlots(r.frequency().timedSlots());
            f.setEveryNHours(r.frequency().everyNHours());
            f.setFirstSlot(r.frequency().firstSlot());
            f.setDaysOfWeek(r.frequency().daysOfWeek());
            b.setFrequency(f);
        }
        b.setMinIntervalMinutes(r.minIntervalMinutes());
        b.setMaxDosesPer24h(r.maxDosesPer24h());
        b.setStartedAt(r.startedAt());
        b.setEndedAt(r.endedAt());
        b.setMedicationSource(r.medicationSource().name());
        PrescriberBody p = new PrescriberBody();
        p.setType(r.prescriber().type().name());
        p.setFacilityName(r.prescriber().facilityName());
        p.setPrescriberName(r.prescriber().prescriberName());
        p.setPrescribedAt(r.prescriber().prescribedAt());
        p.setPrescriptionFileId(r.prescriber().prescriptionFileId());
        b.setPrescriber(p);
        b.setInstructionNote(r.instructionNote());
        if (r.issueConfirmations() != null) {
            b.setConfirmedIssues(r.issueConfirmations().stream()
                    .map(c -> new ConfirmedIssueBody(c.issueCode(), c.reason()))
                    .toList());
        }
        return b;
    }

    static ExternalBasisBody basis(ExternalBasisRequest r) {
        if (r == null) {
            return null;
        }
        ExternalBasisBody b = new ExternalBasisBody();
        b.setFacilityName(r.facilityName());
        b.setPrescriberName(r.prescriberName());
        b.setPrescribedDate(r.prescribedDate());
        b.setBasisType(r.basisType().name());
        b.setCallerName(r.callerName());
        b.setReceiverName(r.receiverName());
        b.setCalledAt(r.calledAt());
        b.setDocumentFileId(r.documentFileId());
        return b;
    }

    static OrderPrescriptionBody order(PrescriptionCommandRequest r) {
        OrderPrescriptionBody b = new OrderPrescriptionBody();
        b.setReason(r.reason());
        b.setExternalBasis(basis(r.externalBasis()));
        return b;
    }

    static DiscontinuePrescriptionBody discontinue(DiscontinueRequest r) {
        DiscontinuePrescriptionBody b = new DiscontinuePrescriptionBody();
        b.setReason(r.reason());
        b.setDiscontinueAt(r.discontinueAt());
        b.setExternalBasis(basis(r.externalBasis()));
        return b;
    }

    static ExtendPrescriptionBody extend(ExtendRequest r) {
        ExtendPrescriptionBody b = new ExtendPrescriptionBody();
        b.setReason(r.reason());
        b.setNewEndedAt(r.newEndedAt());
        b.setExternalBasis(basis(r.externalBasis()));
        return b;
    }

    static ChangeDosageBody changeDosage(ChangeDosageRequest r) {
        ChangeDosageBody b = new ChangeDosageBody();
        b.setNewPrescription(prescription(r.newPrescription()));
        b.setEffectiveFrom(r.effectiveFrom());
        b.setReason(r.reason());
        b.setExternalBasis(basis(r.externalBasis()));
        return b;
    }

    static ReconciliationItemBody reconciliationItem(ReconciliationItemRequest r) {
        ReconciliationItemBody b = new ReconciliationItemBody();
        b.setId(r.id());
        b.setSource(r.source().name());
        b.setPrescriptionId(r.prescriptionId());
        b.setAdmissionMedicationItemId(r.admissionMedicationItemId());
        b.setMedicationDescription(r.medicationDescription());
        b.setDecision(r.decision() == null ? null : r.decision().name());
        b.setReason(r.reason());
        b.setNewPrescription(r.newPrescription() == null ? null : prescription(r.newPrescription()));
        return b;
    }
}
