package com.eldercare.backend.incident.internal.emergency.service;

import com.eldercare.backend.common.document.FacilityInfo;
import com.eldercare.backend.common.document.PdfDocument;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.emergency.domain.TransferSummaryContent;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Phiếu tóm tắt chuyển viện PDF (FR-050h, BR-M05-32; research S-25): dựng từ đúng nội dung bản tóm tắt đã chụp, không đọc
 * lại module khác; có dòng "không thay giấy chuyển cơ sở khám bệnh, chữa bệnh" và ô ký của người lập.
 */
final class TransferSummaryDocument {
    static final String DISCLAIMER = "Phiếu thông tin bàn giao, không thay giấy chuyển cơ sở khám bệnh, chữa bệnh.";
    static final String REPRINT = "Bản in lại";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(BusinessTime.ZONE);

    private TransferSummaryDocument() {}

    static byte[] render(TransferSummaryContent s, FacilityInfo facility) {
        var pdf = PdfDocument.a4(facility, null, "PHIẾU TÓM TẮT CHUYỂN VIỆN")
                .paragraph(DISCLAIMER)
                .paragraph("Người cao tuổi: " + text(s.residentName()))
                .paragraph("Cơ sở tiếp nhận: " + text(s.receivingFacility()))
                .paragraph("Thời điểm chuyển: " + time(s.transferAt()))
                .paragraph("Người đi kèm: " + text(s.companion()));
        if (s.notAskedFirstReason() != null) {
            pdf.paragraph("Lý do chưa hỏi ý kiến gia đình trước: " + s.notAskedFirstReason());
        }
        pdf.paragraph("Dị ứng: " + names(s.allergies()))
                .table(
                        List.of("Thuốc đang dùng", "Hàm lượng", "Liều", "Đường dùng"),
                        rows(s.currentMedications(), "name", "strength", "dosage", "route"))
                .table(
                        List.of("Chỉ số gần nhất", "Giá trị", "Đơn vị", "Thời điểm đo"),
                        rows(s.latestVitalSigns(), "indicatorName", "values", "unit", "measuredAt"))
                .table(
                        List.of("Diễn biến", "Nội dung", "Thời điểm"),
                        rows(s.progressions(), "typeCode", "content", "occurredAt"))
                .paragraph("Nguyện vọng cuối đời: " + wish(s.endOfLifeWish()))
                .paragraph("Nguyện vọng khi điều trị: " + treatment(s.treatmentWish()))
                .paragraph("Người liên hệ chính: "
                        + (s.primaryContact() == null
                                ? "chưa có"
                                : text(s.primaryContact().fullName())
                                        + (s.primaryContact().phoneNumber() == null
                                                ? ""
                                                : " – " + s.primaryContact().phoneNumber())))
                .paragraph("Người lập: " + text(s.preparedBy()) + ", lúc " + time(s.preparedAt()))
                .signatures("Người lập phiếu");
        return pdf.toBytes();
    }

    private static String names(List<Map<String, Object>> items) {
        if (items == null) return "không lấy được";
        if (items.isEmpty()) return "không ghi nhận";
        return items.stream().map(i -> text(i.get("name"))).collect(Collectors.joining(", "));
    }

    private static List<List<String>> rows(List<Map<String, Object>> items, String... keys) {
        if (items == null) return List.of();
        return items.stream()
                .map(i ->
                        java.util.Arrays.stream(keys).map(k -> value(i.get(k))).toList())
                .toList();
    }

    private static String wish(Map<String, Object> wish) {
        if (wish == null) return "không lấy được";
        if (Boolean.TRUE.equals(wish.get("notRecorded"))) return "chưa ghi nhận";
        return text(wish.get("choice")) + (wish.get("note") == null ? "" : " (" + wish.get("note") + ")");
    }

    private static String treatment(Map<String, Object> wish) {
        if (wish == null) return "chưa ghi nhận";
        Object hospitals = wish.get("preferredHospitals");
        return "cơ sở mong muốn " + value(hospitals)
                + (Boolean.TRUE.equals(wish.get("askFamilyFirst")) ? "; cần hỏi ý kiến gia đình trước" : "");
    }

    private static String value(Object v) {
        if (v instanceof Instant at) return time(at);
        if (v instanceof List<?> list) {
            return list.stream()
                    .map(o -> o instanceof Map<?, ?> m && m.get("name") != null
                            ? m.get("name").toString()
                            : value(o))
                    .collect(Collectors.joining(", "));
        }
        if (v instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));
        }
        return v == null ? "" : v.toString();
    }

    private static String time(Instant at) {
        return at == null ? "" : TIME.format(at);
    }

    private static String text(Object v) {
        return Objects.toString(v, "");
    }
}
