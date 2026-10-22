package com.cambae.backend.common.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.document.ExcelDocument.Column;
import com.cambae.backend.common.document.ExcelDocument.Format;
import com.cambae.backend.common.error.InvalidValueException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.ReadingOptions;
import org.dhatim.fastexcel.reader.Row;
import org.junit.jupiter.api.Test;

/**
 * 007 T104 (research S-25): Excel ghi bảng có tiêu đề, định dạng tiền, ngày; đọc lại file theo mẫu (008 import, 017
 * chứng từ, sao kê).
 */
class ExcelDocumentTest {

    private static final FacilityInfo FACILITY = new FacilityInfo(
            "Viện dưỡng lão Bình An", "12 đường Hoà Bình, Hà Nội", "0101234567", "024 3999 8888", null);

    private static final List<Column> COLUMNS = List.of(
            new Column("Ngày", Format.DATE),
            new Column("Thời điểm", Format.DATE_TIME),
            new Column("Mô tả", Format.TEXT),
            new Column("Số lượng", Format.NUMBER),
            new Column("Số tiền", Format.MONEY));

    private static byte[] sample() {
        ExcelDocument excel = new ExcelDocument();
        excel.sheet("Sao kê")
                .header(FACILITY, "SAO KÊ SỐ DƯ")
                .line("Người cao tuổi: Nguyễn Thị Ánh")
                .table(
                        COLUMNS,
                        List.of(
                                List.of(
                                        LocalDate.of(2026, 10, 1),
                                        LocalDateTime.of(2026, 10, 1, 8, 30),
                                        "Nộp tiền mặt",
                                        1,
                                        15_200_000L),
                                java.util.Arrays.asList(
                                        LocalDate.of(2026, 10, 31), null, "Thanh toán bảng tháng 10", 2, -9_000_000L)));
        return excel.toBytes();
    }

    @Test
    void written_readsBackByTemplateHeaders() {
        List<ExcelRow> rows = ExcelDocument.read(sample(), List.of("Ngày", "Mô tả", "Số tiền"), 100);

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst().date("Ngày")).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(rows.getFirst().text("Mô tả")).isEqualTo("Nộp tiền mặt");
        assertThat(rows.getFirst().number("Số tiền")).isEqualByComparingTo(new BigDecimal("15200000"));
        assertThat(rows.get(1).number("Số tiền")).isEqualByComparingTo(new BigDecimal("-9000000"));
        assertThat(rows.get(1).text("Thời điểm")).isNull();
        // Số dòng theo Excel (1-based) để báo lỗi từng dòng khi import
        // (bốn dòng thông tin viện, một dòng trống, tiêu đề, một dòng trống, một dòng chữ, dòng tiêu đề bảng)
        assertThat(rows.getFirst().rowNumber()).isEqualTo(10);
        assertThat(rows.get(1).rowNumber()).isEqualTo(11);
    }

    @Test
    void read_cellOfWrongType_is422WithRowAndColumn() {
        ExcelRow first = ExcelDocument.read(sample(), List.of("Mô tả"), 100).getFirst();

        assertThatThrownBy(() -> first.number("Mô tả"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("Dòng 10")
                .hasMessageContaining("Mô tả");
    }

    @Test
    void written_hasHeaderLines_andMoneyDateFormats() throws IOException {
        try (ReadableWorkbook workbook =
                new ReadableWorkbook(new ByteArrayInputStream(sample()), new ReadingOptions(true, false))) {
            List<Row> rows = workbook.getFirstSheet().read();
            assertThat(workbook.getFirstSheet().getName()).isEqualTo("Sao kê");
            assertThat(rows.get(0).getCellText(0)).isEqualTo("Viện dưỡng lão Bình An");
            assertThat(rows.stream().map(r -> r.getCellText(0)))
                    .contains("SAO KÊ SỐ DƯ", "Người cao tuổi: Nguyễn Thị Ánh");
            Row first = rows.stream()
                    .filter(r -> "Nộp tiền mặt".equals(r.getCellText(2)))
                    .findFirst()
                    .orElseThrow();
            assertThat(first.getCell(4).getDataFormatString()).isEqualTo("#,##0");
            assertThat(first.getCell(0).getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(first.getCell(1).getDataFormatString()).isEqualTo("dd/mm/yyyy hh:mm");
        }
    }

    @Test
    void read_missingTemplateHeader_is422() {
        assertThatThrownBy(() -> ExcelDocument.read(sample(), List.of("Ngày", "Số điện thoại"), 100))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("Số điện thoại");
    }

    @Test
    void read_moreRowsThanLimit_is422() {
        assertThatThrownBy(() -> ExcelDocument.read(sample(), List.of("Ngày"), 1))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("1");
    }

    @Test
    void read_notXlsx_is422() {
        assertThatThrownBy(() -> ExcelDocument.read("không phải excel".getBytes(), List.of("Ngày"), 10))
                .isInstanceOf(InvalidValueException.class);
    }
}
