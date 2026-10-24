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
        Long nctId,
        Long thuocId,
        BigDecimal lieu,
        String donVi,
        String duongDung,
        String loaiDon,
        FrequencyResponse tanSuat,
        Integer khoangCachToiThieuPhut,
        Integer soLanToiDa24h,
        Instant batDauLuc,
        LocalDate ngayKetThuc,
        Instant ngungTu,
        String nguonThuoc,
        PrescriberResponse coSoKe,
        String ghiChuHuongDan,
        String trangThai,
        String lyDoTamDung,
        boolean dauChoTiepNhan,
        Long thayTheChoId,
        Long biThayTheBoiId,
        boolean chuaCoThuoc,
        List<PrescriptionIssueResponse> vanDeDaXacNhan,
        List<Long> loGan,
        List<String> lenhDuocPhep,
        List<String> truongBiAn,
        int version) {

    public record FrequencyResponse(
            String kieu, List<String> cacMoc, Integer nGio, Instant mocDau, List<Integer> ngayTrongTuan) {}

    public record PrescriberResponse(String loai, String tenCoSo, String nguoiKe, LocalDate ngayKe, Long tepDonId) {}

    public static PrescriptionResponse from(
            Prescription p,
            Long biThayTheBoiId,
            boolean chuaCoThuoc,
            List<PrescriptionIssue> issues,
            List<Long> loGan,
            List<String> lenhDuocPhep,
            List<String> truongBiAn) {

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
                biThayTheBoiId,
                chuaCoThuoc,
                issueResponses,
                loGan != null ? loGan : List.of(),
                lenhDuocPhep != null ? lenhDuocPhep : List.of(),
                truongBiAn != null ? truongBiAn : List.of(),
                p.getVersion());
    }
}
