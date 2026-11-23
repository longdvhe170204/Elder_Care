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
        assertThat(text).contains("Nơi chuyển đến thực tế: Bệnh viện X", "Thời điểm chuyển: 19:16 ngày 22/11/2026");
        assertThat(text.indexOf("Nơi chuyển đến thực tế"))
                .isLessThan(text.indexOf("Thời điểm chuyển"))
                .isLessThan(text.indexOf("Bệnh nền"));
        assertThat(text.indexOf("Thời điểm chuyển")).isLessThan(text.indexOf("Dị ứng"));
        assertThat(text).contains("Lý do chuyển và tình trạng lúc bàn giao: Ngã trong nhà vệ sinh");
        assertThat(text).contains("Sơ cứu, chăm sóc tại chỗ: Băng ép vết thương");
        assertThat(text).contains("Đã chuyển viện sang Bệnh viện X").doesNotContain("Đã chuyển viện: Đã chuyển viện");
        assertThat(text).doesNotContain("FIRST_AID").doesNotContain("TRANSFERRED_OUT");
        assertThat(text).contains("Dị ứng: Sữa bò, phấn hoa", "Bệnh nền: Tăng huyết áp", "Paracetamol 500mg");
        assertThat(text)
                .contains("Người liên hệ chính: Nguyễn Văn Rạng · 0911000001", "Người đi cùng: Nguyễn Văn Hùng");
        assertThat(text).doesNotContain("Bệnh nền: Chưa có thông tin", "Người liên hệ chính: Chưa có thông tin");
    }

    @Test
    void FR_050h_nguyenVongKhongInMacDinh() throws Exception {
        String text = text(full(false));

        assertThat(text).doesNotContain("Nguyện vọng liên quan đến ca này").doesNotContain("Bệnh viện Bạch Mai");
        assertThat(text).doesNotContain("Đưa về nhà");
    }

    @Test
    void FR_050h_guiKemNguyenVong_inMucRiengVaGhiRiengBenhVienUuTien() throws Exception {
        String text = text(full(true));

        assertThat(text).contains("Nguyện vọng liên quan đến ca này");
        assertThat(text).contains("Nguyện vọng cuối đời: Đưa về nhà");
        assertThat(text)
                .contains("Bệnh viện ưu tiên theo hồ sơ: Bệnh viện Bạch Mai (khác nơi chuyển đến thực tế ở mục 1)");
        assertThat(text).contains("Nơi chuyển đến thực tế: Bệnh viện X");
        assertThat(text).contains("Có ghi mong muốn hỏi ý kiến gia đình trước");
    }

    @Test
    void FR_050h_caThieuDuLieu_ghiChuaCoThongTinVaKhongInMa() throws Exception {
        String text = text(sparse());

        assertThat(text).contains("TÓM TẮT THÔNG TIN SỨC KHOẺ");
        assertThat(text).contains("Dị ứng: Chưa có thông tin", "Bệnh nền: Chưa có thông tin");
        assertThat(text).contains("Thuốc đang dùng: Chưa có thông tin", "Chỉ số gần nhất: Chưa có thông tin");
        assertThat(text).contains("Lý do chuyển và tình trạng lúc bàn giao: Chưa có thông tin");
        assertThat(text).contains("Người liên hệ chính: Chưa có thông tin");
        assertThat(text).contains("Có bản ghi sơ cứu, chăm sóc tại chỗ, nội dung chưa rõ.");
        assertThat(text).doesNotContain("FIRST_AID").doesNotContain("null").doesNotContain("không lấy được");
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
                chronic);
    }
}
