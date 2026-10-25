package com.cambae.backend.m07.web.response;

import com.cambae.backend.m07.internal.domain.Prescription;
import com.cambae.backend.m07.internal.domain.PrescriptionIssue;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Prescription detail response (schema DonThuoc in api-006.yaml).
 */
public record PrescriptionResponse(
        Long id,
        Long residentId,
        Long medicationId,
        BigDecimal dosage,
        String unit,
        String route,
        String prescriptionType,
        FrequencyResponse frequency,
        Integer minIntervalMinutes,
        Integer maxDosesPer24h,
        Instant startedAt,
        LocalDate endedAt,
        Instant suspendedFrom,
        String medicationSource,
        PrescriberResponse prescriber,
        String instructionNote,
        String status,
        String suspensionReason,
        boolean pendingAdmission,
        Long replacesId,
        Long replacedById,
        boolean noMedicationAvailable,
        List<PrescriptionIssueResponse> confirmedIssues,
        List<Long> boundBatchIds,
        List<String> allowedCommands,
        List<String> hiddenFields,
        int version) {

    public record FrequencyResponse(
            String type, List<String> timedSlots, Integer everyNHours, Instant firstSlot, List<Integer> daysOfWeek) {

        public String kieu() {
            return type;
        }

        public List<String> cacMoc() {
            return timedSlots;
        }

        public Integer nGio() {
            return everyNHours;
        }

        public Instant mocDau() {
            return firstSlot;
        }

        public List<Integer> ngayTrongTuan() {
            return daysOfWeek;
        }
    }

    public record PrescriberResponse(
            String type, String facilityName, String prescriberName, LocalDate prescribedAt, Long prescriptionFileId) {

        public String loai() {
            return type;
        }

        public String tenCoSo() {
            return facilityName;
        }

        public String nguoiKe() {
            return prescriberName;
        }

        public LocalDate ngayKe() {
            return prescribedAt;
        }

        public Long tepDonId() {
            return prescriptionFileId;
        }
    }

    public static PrescriptionResponse from(
            Prescription p,
            Long replacedById,
            boolean noMedicationAvailable,
            List<PrescriptionIssue> issues,
            List<Long> boundBatchIds,
            List<String> allowedCommands,
            List<String> hiddenFields) {

        FrequencyResponse freq = null;
        if (p.getFrequencyType() != null) {
            freq = new FrequencyResponse(
                    p.getFrequencyType().name(),
                    null, // parsed from timed_slots if needed
                    p.getEveryNHours(),
                    p.getFirstSlot(),
                    null);
        }

        PrescriberResponse prescriber = new PrescriberResponse(
                p.getPrescriberType() != null ? p.getPrescriberType().name() : null,
                p.getFacilityName(),
                p.getPrescriberName(),
                p.getPrescribedAt(),
                p.getPrescriptionFileId());

        List<PrescriptionIssueResponse> issueResponses = issues != null
                ? issues.stream()
                        .map(i -> new PrescriptionIssueResponse(
                                i.getIssueCode(),
                                i.getIssueType().name(),
                                i.getActiveIngredient(),
                                i.getDuplicatePrescriptionId(),
                                i.getAllergyItemId(),
                                i.getSeverity(),
                                i.isManualReviewRequired(),
                                i.getLineNumber()))
                        .toList()
                : List.of();

        return new PrescriptionResponse(
                p.getId(),
                p.getResidentId(),
                p.getMedicationId(),
                p.getDosage(),
                p.getDosageUnit(),
                p.getRoute(),
                p.getPrescriptionType() != null ? p.getPrescriptionType().name() : null,
                freq,
                p.getMinIntervalMinutes(),
                p.getMaxDosesPer24h(),
                p.getStartedAt(),
                p.getEndedAt(),
                p.getSuspendedFrom(),
                p.getMedicationSource() != null ? p.getMedicationSource().name() : null,
                prescriber,
                p.getInstructionNote(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getSuspensionReason() != null ? p.getSuspensionReason().name() : null,
                p.isPendingAdmission(),
                p.getReplacesId(),
                replacedById,
                noMedicationAvailable,
                issueResponses,
                boundBatchIds != null ? boundBatchIds : List.of(),
                allowedCommands != null ? allowedCommands : List.of(),
                hiddenFields != null ? hiddenFields : List.of(),
                p.getVersion());
    }
}
