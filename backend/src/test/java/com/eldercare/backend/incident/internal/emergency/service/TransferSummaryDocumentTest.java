package com.eldercare.backend.incident.internal.emergency.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.emergency.domain.EmergencyCard;
import com.eldercare.backend.incident.internal.emergency.domain.TransferSummaryContent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

/**
 * FR-050h, BR-M05-32: phiếu "Tóm tắt thông tin sức khoẻ": tiêu đề, thứ tự mục, nhãn tiếng Việt thay mã, mục thiếu ghi
 * "Chưa có thông tin", và nguyện vọng chỉ in khi người lập chọn gửi kèm.
 */
class TransferSummaryDocumentTest {
    static final Instant AT = Instant.parse("2026-11-22T12:16:00Z");

    @Test
    void FR_050h_caDuDuLieu_dungTieuDeThuTuVaNhanTiengViet() throws Exception {
        String text = text(full(false));

        assertThat(text).contains("TÓM TẮT THÔNG TIN SỨC KHOẺ").doesNotContain("PHIẾU TÓM TẮT CHUYỂN VIỆN");
        assertThat(text).contains("CẦN BIẾT NGAY", "Chuyển lúc 19:16 ngày 22/11/2026", "Xảy ra lúc 18:46");
        assertThat(text).contains("Sự cố SC-20 · Phòng 101", "Nguyễn Thị An · 84 tuổi");
        assertThat(text.indexOf("CẦN BIẾT NGAY"))
                .isLessThan(text.indexOf("I. LÝ DO CHUYỂN"))
                .isLessThan(text.indexOf("II. BỆNH NỀN"))
                .isLessThan(text.indexOf("III. ĐÃ XỬ TRÍ"))
                .isLessThan(text.indexOf("IV. NƠI TIẾP NHẬN"));
        assertThat(text).contains("Ngã trong nhà vệ sinh");
        assertThat(text).contains("Sơ cứu, chăm sóc tại chỗ", "Băng ép vết thương");
        assertThat(text).doesNotContain("Đã chuyển viện sang").doesNotContain("Đã chuyển viện: Đã chuyển viện");
        assertThat(text).doesNotContain("FIRST_AID").doesNotContain("TRANSFERRED_OUT");
        assertThat(text).contains("Dị ứng Sữa bò, phấn hoa", "• Tăng huyết áp", "Paracetamol 500mg");
        assertThat(text).contains("Nơi tiếp nhận Bệnh viện X", "Liên hệ gia đình Nguyễn Văn Rạng · 0911000001");
        assertThat(text).contains("Người đi cùng Nguyễn Văn Hùng");
        assertThat(text).doesNotContain("Liên hệ gia đình Chưa có thông tin");
    }

    @Test
    void FR_050h_nguyenVongKhongInMacDinh() throws Exception {
        String text = text(full(false));

        assertThat(text).doesNotContain("NGUYỆN VỌNG LIÊN QUAN ĐẾN CA NÀY").doesNotContain("Bệnh viện Bạch Mai");
        assertThat(text).doesNotContain("Đưa về nhà");
    }

    @Test
    void FR_050h_guiKemNguyenVong_inMucRiengVaGhiRiengBenhVienUuTien() throws Exception {
        String text = text(full(true));

        assertThat(text).contains("V. NGUYỆN VỌNG LIÊN QUAN ĐẾN CA NÀY");
        assertThat(text).contains("Nguyện vọng cuối đời: Đưa về nhà");
        assertThat(text)
                .contains("Bệnh viện ưu tiên theo hồ sơ: Bệnh viện Bạch Mai (khác nơi tiếp nhận ở mục IV)");
        assertThat(text).contains("Nơi tiếp nhận Bệnh viện X");
        assertThat(text).contains("Có ghi mong muốn hỏi ý kiến gia đình trước");
    }

    @Test
    void FR_050h_caThieuDuLieu_ghiChuaCoThongTinVaKhongInMa() throws Exception {
        String text = text(sparse());

        assertThat(text).contains("TÓM TẮT THÔNG TIN SỨC KHOẺ");
        assertThat(text).contains("Dị ứng Chưa khai báo, hãy hỏi người nhà", "Bệnh nền Chưa có thông tin");
        assertThat(text).contains("Thuốc đang dùng Chưa có thông tin", "chưa có chỉ số nào được ghi");
        assertThat(text).contains("I. LÝ DO CHUYỂN VÀ TÌNH TRẠNG LÚC CHUYỂN");
        assertThat(text).contains("Liên hệ gia đình Chưa có thông tin");
        assertThat(text).contains("Sơ cứu, chăm sóc tại chỗ Nội dung chưa rõ.");
        assertThat(text).doesNotContain("FIRST_AID").doesNotContain("null").doesNotContain("không lấy được");
    }

    /** BF-06: phiếu luôn vừa một trang A4, kể cả khi dữ liệu rất nhiều (diễn biến, thuốc, chữ dài, gửi kèm nguyện vọng). */
    @Test
    void BF_06_phieuLuonMotTrang_keCaKhiDuLieuRatNhieu() throws Exception {
        assertThat(pages(full(false))).isEqualTo(1);
        assertThat(pages(full(true))).isEqualTo(1);
        assertThat(pages(sparse())).isEqualTo(1);

        String longText = "Cụ ngã trong nhà vệ sinh, đau hông trái, huyết áp cao, ".repeat(30);
        var progressions = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < 40; i++) {
            progressions.add(Map.<String, Object>of(
                    "typeCode", "OTHER_ACTION", "content", "Diễn biến số " + i + ": " + longText, "occurredAt", AT));
        }
        var medications = new java.util.ArrayList<Map<String, Object>>();
        for (int i = 0; i < 15; i++) {
            medications.add(Map.<String, Object>of("name", "Thuốc số " + i, "strength", "500mg", "dosage", "2 lần/ngày"));
        }
        byte[] big = TransferSummaryDocument.render(
                content(
                        true,
                        longText,
                        List.of(Map.<String, Object>of("name", longText)),
                        List.of(Map.<String, Object>of("name", longText)),
                        medications,
                        progressions,
                        Map.of("choice", "RETURN_HOME", "note", longText),
                        Map.of("preferredHospitals", List.of(Map.of("name", "Bệnh viện Bạch Mai")), "askFamilyFirst", true),
                        new EmergencyCard.Contact("Nguyễn Văn Rạng", "0911000001")),
                null);

        assertThat(pages(big)).isEqualTo(1);
        assertThat(text(big)).contains("TÓM TẮT THÔNG TIN SỨC KHOẺ", "diễn biến cũ hơn trong hồ sơ sự cố");
    }

    private static int pages(byte[] pdf) throws Exception {
        var reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();
        return pages;
    }

    private static String text(byte[] pdf) throws Exception {
        var reader = new PdfReader(pdf);
        var extractor = new PdfTextExtractor(reader);
        var all = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            all.append(extractor.getTextFromPage(page)).append('\n');
        }
        return all.toString().replaceAll("\\s+", " ");
    }

    private static byte[] full(boolean includeWishes) {
        return TransferSummaryDocument.render(
                content(
                        includeWishes,
                        "Ngã trong nhà vệ sinh",
                        List.of(Map.<String, Object>of("name", "Sữa bò"), Map.<String, Object>of("name", "phấn hoa")),
                        List.of(Map.<String, Object>of("name", "Tăng huyết áp")),
                        List.of(Map.<String, Object>of("name", "Paracetamol", "strength", "500mg")),
                        List.of(
                                Map.<String, Object>of(
                                        "typeCode", "FIRST_AID", "content", "Băng ép vết thương", "occurredAt", AT),
                                Map.<String, Object>of(
                                        "typeCode",
                                        "TRANSFERRED_OUT",
                                        "content",
                                        "Đã chuyển viện sang Bệnh viện X",
                                        "occurredAt",
                                        AT)),
                        Map.of("choice", "RETURN_HOME", "note", "ký bởi người đại diện"),
                        Map.of(
                                "preferredHospitals",
                                List.of(Map.of("name", "Bệnh viện Bạch Mai")),
                                "askFamilyFirst",
                                true),
                        new EmergencyCard.Contact("Nguyễn Văn Rạng", "0911000001")),
                null);
    }

    private static byte[] sparse() {
        return TransferSummaryDocument.render(
                content(
                        false,
                        null,
                        null,
                        List.of(),
                        null,
                        List.of(Map.<String, Object>of("typeCode", "FIRST_AID", "content", "", "occurredAt", AT)),
                        null,
                        null,
                        null),
                null);
    }

    private static TransferSummaryContent content(
            boolean includeWishes,
            String condition,
            List<Map<String, Object>> allergies,
            List<Map<String, Object>> chronic,
            List<Map<String, Object>> medications,
            List<Map<String, Object>> progressions,
            Map<String, Object> endOfLife,
            Map<String, Object> treatment,
            EmergencyCard.Contact contact) {
        return new TransferSummaryContent(
                1L,
                "Nguyễn Thị An",
                "Bệnh viện X",
                AT,
                null,
                "Nguyễn Văn Hùng",
                null,
                null,
                allergies,
                medications,
                null,
                progressions,
                endOfLife,
                treatment,
                null,
                contact,
                0L,
                2L,
                "Phạm Đức Bình",
                AT,
                3L,
                includeWishes,
                condition,
                chronic,
                20L,
                AT.minusSeconds(1800),
                java.time.LocalDate.of(1942, 3, 5),
                "Phòng 101");
    }
}
