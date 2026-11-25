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
import java.util.stream.Stream;

/**
 * Phiếu "Tóm tắt thông tin sức khoẻ" khi chuyển viện (FR-050h, BR-M05-32; research S-25): dựng từ đúng nội dung bản tóm tắt
 * đã chụp, không đọc lại module khác. Thứ tự: thông tin chuyển viện (nơi đến thực tế, giờ chuyển, tình trạng lúc bàn giao),
 * thông tin sức khoẻ, thông tin hỗ trợ bàn giao. Mục thiếu ghi "Chưa có thông tin". Nguyện vọng không in mặc định: chỉ in mục
 * "Nguyện vọng liên quan đến ca này" khi người lập chọn gửi kèm. Giữ dòng "không thay giấy chuyển cơ sở khám bệnh, chữa
 * bệnh" và ô ký của người lập.
 */
final class TransferSummaryDocument {
    static final String DISCLAIMER = "Phiếu thông tin bàn giao, không thay giấy chuyển cơ sở khám bệnh, chữa bệnh.";
    static final String REPRINT = "Bản in lại";
    static final String TITLE = "TÓM TẮT THÔNG TIN SỨC KHOẺ";
    static final String MISSING = "Chưa có thông tin";
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(BusinessTime.ZONE);
    private static final DateTimeFormatter ROW_TIME =
            DateTimeFormatter.ofPattern("HH:mm\ndd/MM/yyyy").withZone(BusinessTime.ZONE);

    /** Nhãn tiếng Việt cho mã loại diễn biến (danh mục M05.PROGRESSION_TYPE); mã lạ thì ghi chung, không in mã. */
    private static final Map<String, String> PROGRESSION = Map.ofEntries(
            Map.entry("CHEST_COMPRESSION", "Ép tim ngoài lồng ngực"),
            Map.entry("VENTILATION_SUPPORT", "Hỗ trợ thông khí"),
            Map.entry("DEFIBRILLATION", "Sốc điện"),
            Map.entry("INTUBATION", "Đặt nội khí quản"),
            Map.entry("EMERGENCY_MEDICATION", "Dùng thuốc cấp cứu"),
            Map.entry("CALL_SUPPORT", "Gọi hỗ trợ"),
            Map.entry("CALL_EMERGENCY", "Gọi cấp cứu"),
            Map.entry("REPORT_HEALTH_AUTHORITY", "Báo cơ quan y tế"),
            Map.entry("SOURCE_RESOLVED", "Nguồn đã xử lý"),
            Map.entry("TRANSFER_REFUSED", "Chuyển viện bị từ chối"),
            Map.entry("FOUND", "Đã tìm thấy"),
            Map.entry("CANCELLATION_REQUESTED", "Đề nghị hủy"),
            Map.entry("TRANSFERRED_OUT", "Đã chuyển viện"),
            Map.entry("FIRST_AID", "Sơ cứu, chăm sóc tại chỗ"),
            Map.entry("OTHER_ACTION", "Xử lý khác"),
            Map.entry("OTHER_ASSESSMENT", "Đánh giá khác"));

    private static final Map<String, String> WISH_CHOICE = Map.of(
            "TRANSFER_TO_HOSPITAL", "Chuyển bệnh viện",
            "RETURN_HOME", "Đưa về nhà",
            "STAY_PALLIATIVE", "Ở lại chăm sóc giảm nhẹ");

    private TransferSummaryDocument() {}

    /** Giới hạn độ dài từng mục để phiếu luôn vừa một trang; thử từ rộng tới chặt cho tới khi chỉ còn một trang. */
    private record Caps(boolean compact, int progressions, int chars, int items) {}

    private static final List<Caps> ATTEMPTS =
            List.of(
                    new Caps(false, 8, 400, 6), // chữ thoáng khi nội dung ngắn
                    new Caps(true, 8, 400, 6),
                    new Caps(true, 5, 260, 4),
                    new Caps(true, 3, 160, 3),
                    new Caps(true, 2, 100, 2));

    static byte[] render(TransferSummaryContent s, FacilityInfo facility) {
        byte[] last = null;
        for (var caps : ATTEMPTS) {
            last = renderOnce(s, facility, caps);
            if (pages(last) <= 1) return last;
        }
        return last;
    }

    private static int pages(byte[] pdf) {
        try {
            var reader = new org.openpdf.text.pdf.PdfReader(pdf);
            int pages = reader.getNumberOfPages();
            reader.close();
            return pages;
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static byte[] renderOnce(TransferSummaryContent s, FacilityInfo facility, Caps caps) {
        // Một trang A4, bố cục theo trình tự bàn giao chuẩn (SBAR): hành chính (hai cột) → tình trạng, lý do → thông tin cần
        // lưu ý (ba cột) → diễn biến, xử trí → liên hệ, bàn giao (hai cột) → nguyện vọng (khi chọn gửi kèm) → ký hai bên.
        var pdf = (caps.compact() ? PdfDocument.a4Compact(facility, null, TITLE) : PdfDocument.a4(facility, null, TITLE))
                .note("(Bàn giao kèm người cao tuổi khi chuyển đến cơ sở khám bệnh, chữa bệnh)", true)
                .heading("I. HÀNH CHÍNH");
        var admin = new java.util.ArrayList<String>(List.of(
                "Người cao tuổi", orMissing(s.residentName()),
                "Nơi chuyển đến thực tế", clip(orMissing(s.receivingFacility()), caps.chars()),
                "Thời điểm chuyển", s.transferAt() == null ? MISSING : time(s.transferAt())));
        if (s.expectedReturnAt() != null) {
            admin.add("Dự kiến về");
            admin.add(time(s.expectedReturnAt()));
        }
        pdf.fields(2, admin.toArray(String[]::new));

        pdf.heading("II. TÌNH TRẠNG LÚC CHUYỂN")
                .field("Lý do chuyển và tình trạng lúc bàn giao", clip(orMissing(s.handoverCondition()), caps.chars()))
                .field("Chỉ số gần nhất", vitals(s.latestVitalSigns(), caps));

        pdf.heading("III. THÔNG TIN CẦN LƯU Ý")
                .fields(
                        3,
                        "Dị ứng", clip(names(s.allergies()), caps.chars()),
                        "Bệnh nền", clip(names(s.chronicConditions()), caps.chars()),
                        "Thuốc đang dùng", medications(s.currentMedications(), caps));

        pdf.heading("IV. DIỄN BIẾN VÀ XỬ TRÍ TẠI VIỆN");
        var progressions = progressionRows(s.progressions(), caps);
        if (progressions.rows().isEmpty()) {
            pdf.field("Diễn biến và xử trí", MISSING);
        } else {
            // Cột thời điểm hẹp, cột diễn biến rộng để có chỗ ghi chữ
            pdf.table(List.of("Thời điểm", "Diễn biến và xử trí"), progressions.rows(), new float[] {1f, 6f});
            if (progressions.hidden() > 0) {
                pdf.note("(Còn " + progressions.hidden() + " diễn biến cũ hơn trong hồ sơ sự cố.)", false);
            }
        }

        pdf.heading("V. LIÊN HỆ VÀ BÀN GIAO")
                .fields(2, "Người liên hệ chính", contact(s), "Người đi cùng", clip(orMissing(s.companion()), caps.chars()));
        if (s.notAskedFirstReason() != null) {
            pdf.field("Lý do chưa hỏi ý kiến gia đình trước", clip(s.notAskedFirstReason(), caps.chars()));
        }

        if (Boolean.TRUE.equals(s.includeWishes())) {
            wishes(pdf, s, caps);
        }

        pdf.signatures("Người lập phiếu", "Đại diện nơi nhận")
                .note(
                        "Lập lúc " + time(s.preparedAt()) + " bởi " + orMissing(s.preparedBy())
                                + ". Nguồn: hồ sơ người cao tuổi và bản ghi chuyển viện trên Elder_Care.",
                        false)
                .note(DISCLAIMER, false);
        return pdf.toBytes();
    }

    private static String clip(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 1)).stripTrailing() + "…";
    }

    /** Mục chỉ in khi nhân viên chọn gửi kèm; không in toàn văn phiếu nguyện vọng. */
    private static void wishes(PdfDocument pdf, TransferSummaryContent s, Caps caps) {
        pdf.heading("VI. Nguyện vọng liên quan đến ca này");
        var end = s.endOfLifeWish();
        if (end == null || Boolean.TRUE.equals(end.get("notRecorded"))) {
            pdf.field("Nguyện vọng cuối đời", MISSING);
        } else {
            String choice = WISH_CHOICE.getOrDefault(text(end.get("choice")), MISSING);
            pdf.field(
                    "Nguyện vọng cuối đời",
                    choice + (blank(end.get("note")) ? "" : " (" + clip(text(end.get("note")), caps.chars()) + ")"));
        }
        var treatment = s.treatmentWish();
        if (treatment == null) {
            pdf.field("Nguyện vọng khi điều trị", MISSING);
            return;
        }
        String preferred = hospitalNames(treatment.get("preferredHospitals"));
        if (preferred.isEmpty()) {
            pdf.field("Bệnh viện ưu tiên theo hồ sơ", MISSING);
        } else {
            boolean same = s.receivingFacility() != null
                    && preferred
                            .toLowerCase()
                            .contains(s.receivingFacility().trim().toLowerCase());
            pdf.field(
                    "Bệnh viện ưu tiên theo hồ sơ",
                    clip(preferred, caps.chars()) + (same ? "" : " (khác nơi chuyển đến thực tế ở mục 1)"));
        }
        if (Boolean.TRUE.equals(treatment.get("askFamilyFirst"))) {
            pdf.field(
                    "Ghi chú theo hồ sơ",
                    "Có ghi mong muốn hỏi ý kiến gia đình trước. Nhân viên có thể liên hệ song song với việc xử lý ca khẩn."
                            + (blank(treatment.get("note")) ? "" : " " + clip(text(treatment.get("note")), caps.chars())));
        } else if (!blank(treatment.get("note"))) {
            pdf.field("Ghi chú theo hồ sơ", clip(text(treatment.get("note")), caps.chars()));
        }
    }

    private static String vitals(List<Map<String, Object>> items, Caps caps) {
        if (items == null || items.isEmpty()) return MISSING;
        var shown = items.stream()
                .limit(caps.items())
                .map(i -> text(i.get("indicatorName")) + " " + value(i.get("values"))
                        + (blank(i.get("unit")) ? "" : " " + i.get("unit"))
                        + (i.get("measuredAt") == null ? "" : " (" + value(i.get("measuredAt")) + ")"))
                .collect(Collectors.joining("; "));
        return more(shown, items.size(), caps);
    }

    private static String more(String shown, int total, Caps caps) {
        return total > caps.items() ? shown + "; … (còn " + (total - caps.items()) + " mục)" : shown;
    }

    /** Các dòng diễn biến mới nhất vừa chỗ; {@code hidden} là số dòng cũ hơn không in. */
    private record ProgressionRows(List<List<String>> rows, int hidden) {}

    private static ProgressionRows progressionRows(List<Map<String, Object>> items, Caps caps) {
        if (items == null) return new ProgressionRows(List.of(), 0);
        int skip = Math.max(0, items.size() - caps.progressions());
        var rows = items.stream()
                .skip(skip)
                .map(i -> {
                    String label = PROGRESSION.getOrDefault(text(i.get("typeCode")), "diễn biến");
                    String content = text(i.get("content")).trim();
                    String detail = content.isEmpty()
                            ? "Có bản ghi " + label.substring(0, 1).toLowerCase() + label.substring(1)
                                    + ", nội dung chưa rõ."
                            : content.toLowerCase().startsWith(label.toLowerCase()) ? content : label + ": " + content;
                    detail = clip(detail, caps.chars());
                    String at = i.get("occurredAt") instanceof Instant t ? ROW_TIME.format(t) : "";
                    return List.of(at, detail);
                })
                .toList();
        return new ProgressionRows(rows, skip);
    }

    private static String names(List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) return MISSING;
        return items.stream().map(i -> text(i.get("name"))).collect(Collectors.joining(", "));
    }

    private static String medications(List<Map<String, Object>> items, Caps caps) {
        if (items == null || items.isEmpty()) return MISSING;
        var shown = items.stream()
                .limit(caps.items())
                .map(i -> Stream.of("name", "strength", "dosage", "route")
                        .map(k -> text(i.get(k)).trim())
                        .filter(v -> !v.isEmpty())
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining("; "));
        return more(shown, items.size(), caps);
    }

    private static String contact(TransferSummaryContent s) {
        var c = s.primaryContact();
        if (c == null || blank(c.fullName())) return MISSING;
        return c.fullName() + (blank(c.phoneNumber()) ? "" : " · " + c.phoneNumber());
    }

    private static String hospitalNames(Object hospitals) {
        if (!(hospitals instanceof List<?> list)) return "";
        return list.stream()
                .map(o -> o instanceof Map<?, ?> m && m.get("name") != null
                        ? m.get("name").toString()
                        : "")
                .filter(n -> !n.isBlank())
                .collect(Collectors.joining(", "));
    }

    private static String value(Object v) {
        if (v instanceof Instant at) return time(at);
        if (v instanceof List<?> list) {
            return list.stream().map(TransferSummaryDocument::value).collect(Collectors.joining(", "));
        }
        if (v instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(", "));
        }
        return v == null ? "" : v.toString();
    }

    private static String orMissing(String v) {
        return v == null || v.isBlank() ? MISSING : v;
    }

    private static boolean blank(Object v) {
        return v == null || v.toString().isBlank();
    }

    private static String time(Instant at) {
        return at == null ? "" : TIME.format(at).replace(" ", " ngày ");
    }

    private static String text(Object v) {
        return Objects.toString(v, "");
    }
}
