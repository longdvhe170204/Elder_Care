package com.eldercare.backend.common.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

/**
 * 007 T104 (research S-25): chứng từ PDF khổ A4 có đầu trang thông tin viện (CFG-M11-11), bảng, ô ký, số trang; dấu
 * tiếng Việt trích lại đúng nhờ font Be Vietnam Pro nhúng; đóng dấu "Bản in lại" chỉ thêm lớp phủ, nội dung trang gốc
 * giữ nguyên (FR-050h).
 */
class PdfDocumentTest {

    private static final FacilityInfo FACILITY = new FacilityInfo(
            "Viện dưỡng lão Bình An", "12 đường Hoà Bình, phường Đống Đa, Hà Nội", "0101234567", "024 3999 8888", null);

    private static byte[] sample() {
        return PdfDocument.a4(FACILITY, null, "PHIẾU TÓM TẮT CHUYỂN VIỆN")
                .paragraph("Người cao tuổi: Nguyễn Thị Ánh, 82 tuổi; dị ứng: Penicillin")
                .table(
                        List.of("Khoản", "Số lượng", "Thành tiền"),
                        List.of(List.of("Tiền phòng tháng 10", "1", "9.000.000")))
                .signatures("Người lập phiếu", "Người đi kèm")
                .paragraph("Phiếu thông tin bàn giao, không thay giấy chuyển cơ sở khám bệnh, chữa bệnh")
                .toBytes();
    }

    private static String text(byte[] pdf, int page) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(page);
        } finally {
            reader.close();
        }
    }

    @Test
    void vietnameseText_extractsBackUnchanged() throws IOException {
        String page = text(sample(), 1);

        assertThat(page)
                .contains("Viện dưỡng lão Bình An")
                .contains("12 đường Hoà Bình, phường Đống Đa, Hà Nội")
                .contains("0101234567")
                .contains("PHIẾU TÓM TẮT CHUYỂN VIỆN")
                .contains("Nguyễn Thị Ánh")
                .contains("Tiền phòng tháng 10")
                .contains("Người lập phiếu")
                .contains("Người đi kèm")
                .contains("Trang 1/1");
    }

    @Test
    void page_isA4_withFontEmbedded() throws IOException {
        PdfReader reader = new PdfReader(sample());
        try {
            assertThat(reader.getPageSize(1).getWidth()).isEqualTo(PageSize.A4.getWidth());
            assertThat(reader.getPageSize(1).getHeight()).isEqualTo(PageSize.A4.getHeight());
        } finally {
            reader.close();
        }
        assertThat(new String(sample(), java.nio.charset.StandardCharsets.ISO_8859_1))
                .contains("BeVietnamPro")
                .contains("/FontFile2");
    }

    @Test
    void tableRowWithWrongCellCount_isRejected() {
        PdfDocument pdf = PdfDocument.a4(null, null, "SAO KÊ");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> pdf.table(List.of("Mô tả", "Số tiền"), List.of(List.of("Chỉ một ô"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void facilityNotSet_skipsHeader() throws IOException {
        byte[] pdf = PdfDocument.a4(null, null, "SAO KÊ").paragraph("Nội dung").toBytes();

        assertThat(text(pdf, 1)).contains("SAO KÊ").doesNotContain("Mã số thuế");
    }

    @Test
    void longTable_breaksPages_andNumbersEveryPage() throws IOException {
        List<List<String>> rows = java.util.stream.IntStream.range(0, 120)
                .mapToObj(i -> List.of("Dòng " + i, "x"))
                .toList();
        byte[] pdf = PdfDocument.a4(FACILITY, null, "SAO KÊ")
                .table(List.of("Mô tả", "Ghi chú"), rows)
                .toBytes();

        PdfReader reader = new PdfReader(pdf);
        int pages = reader.getNumberOfPages();
        reader.close();
        assertThat(pages).isGreaterThan(1);
        assertThat(text(pdf, pages)).contains("Trang " + pages + "/" + pages).contains("Dòng 119");
    }

    @Test
    void stamp_addsOverlay_keepsOriginalContent() throws IOException {
        byte[] original = sample();

        byte[] stamped = PdfDocument.stamp(original, "Bản in lại");

        // Dấu ghi nối thêm vào sau file gốc: các byte của bản đã phát hành giữ nguyên
        assertThat(stamped).startsWith(original);
        String before = text(original, 1);
        String after = text(stamped, 1);
        assertThat(after).contains("Bản in lại");
        assertThat(after.replace("Bản in lại", "")).contains(before.strip());
        assertThat(text(original, 1)).isEqualTo(before).doesNotContain("Bản in lại");
        PdfReader reader = new PdfReader(stamped);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        reader.close();
    }
}
