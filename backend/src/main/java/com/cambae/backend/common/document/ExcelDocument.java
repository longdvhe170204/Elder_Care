package com.cambae.backend.common.document;

import com.cambae.backend.common.error.InvalidValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;

/**
 * File Excel dùng chung (tên logic {@code ExcelBieuMau}; 007 research S-25), bằng fastexcel: ghi trang có đầu trang
 * thông tin viện, tiêu đề, dòng chữ và bảng có định dạng tiền, ngày; đọc file {@code .xlsx} theo mẫu (008 import).
 */
public final class ExcelDocument {

    /** Định dạng ô của một cột. Tiền theo đồng, không có số lẻ. */
    public enum Format {
        TEXT,
        NUMBER,
        MONEY,
        DATE,
        DATE_TIME
    }

    public record Column(String header, Format format) {}

    private final List<Sheet> sheets = new ArrayList<>();

    /** Thêm một trang tính; ghi theo thứ tự gọi. */
    public Sheet sheet(String name) {
        Sheet sheet = new Sheet(name);
        sheets.add(sheet);
        return sheet;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            Workbook workbook = new Workbook(out, "ElderCare", "1.0");
            for (Sheet sheet : sheets) {
                Worksheet worksheet = workbook.newWorksheet(sheet.name);
                int[] row = {0};
                sheet.parts.forEach(part -> part.accept(worksheet, row));
            }
            workbook.finish();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /**
     * Đọc trang đầu của file theo mẫu: dòng tiêu đề là dòng đầu tiên (trong 10 dòng đầu) chứa đủ {@code headers}; các
     * dòng sau tới hết file là dữ liệu, bỏ dòng trống. Không phải {@code .xlsx}, thiếu cột của mẫu hay quá {@code maxRows}
     * dòng thì 422.
     */
    public static List<ExcelRow> read(byte[] xlsx, List<String> headers, int maxRows) {
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(xlsx));
                Stream<Row> stream = workbook.getFirstSheet().openStream()) {
            Iterator<Row> rows = stream.iterator();
            Map<String, Integer> columns = null;
            for (int i = 0; i < 10 && rows.hasNext() && columns == null; i++) {
                columns = columns(rows.next(), headers);
            }
            if (columns == null) {
                throw new InvalidValueException("File không theo mẫu: thiếu cột " + String.join(", ", headers));
            }
            List<ExcelRow> result = new ArrayList<>();
            while (rows.hasNext()) {
                Row row = rows.next();
                if (row.stream().allMatch(cell -> cell == null || cell.getText().isBlank())) {
                    continue;
                }
                if (result.size() == maxRows) {
                    throw new InvalidValueException("File có quá " + maxRows + " dòng dữ liệu");
                }
                result.add(new ExcelRow(row, columns));
            }
            return result;
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidValueException invalid) {
                throw invalid;
            }
            throw new InvalidValueException("Không đọc được file Excel (.xlsx)");
        }
    }

    /** Vị trí từng cột của mẫu nếu {@code row} là dòng tiêu đề; thiếu cột thì {@code null}. */
    private static Map<String, Integer> columns(Row row, List<String> headers) {
        Map<String, Integer> found = new HashMap<>();
        row.forEach(cell -> {
            if (cell != null) {
                found.putIfAbsent(cell.getText().strip(), cell.getColumnIndex());
            }
        });
        return found.keySet().containsAll(headers) ? found : null;
    }

    /** Một trang tính: các phần ghi lần lượt từ trên xuống. */
    public static final class Sheet {

        private final String name;
        private final List<BiConsumer<Worksheet, int[]>> parts = new ArrayList<>();

        private Sheet(String name) {
            this.name = name;
        }

        /** Đầu trang thông tin viện (CFG-M11-11; {@code null} thì bỏ) và tiêu đề in đậm. */
        public Sheet header(FacilityInfo facility, String title) {
            if (facility != null) {
                text(facility.name(), "", true);
                text(facility.address(), "Địa chỉ: ", false);
                text(facility.taxCode(), "Mã số thuế: ", false);
                text(facility.phone(), "Điện thoại: ", false);
            }
            parts.add((ws, row) -> {
                row[0]++;
                ws.value(row[0], 0, title);
                ws.style(row[0], 0).bold().fontSize(14).set();
                row[0] += 2;
            });
            return this;
        }

        public Sheet line(String text) {
            return text(text, "", false);
        }

        /** Bảng: dòng tiêu đề in đậm, mỗi dòng dữ liệu có đủ số ô như {@code columns}; ô {@code null} để trống. */
        public Sheet table(List<Column> columns, List<? extends List<?>> rows) {
            parts.add((ws, row) -> {
                for (int c = 0; c < columns.size(); c++) {
                    ws.value(row[0], c, columns.get(c).header());
                    ws.style(row[0], c).bold().set();
                    ws.width(c, columns.get(c).format() == Format.TEXT ? 30 : 16);
                }
                row[0]++;
                for (List<?> values : rows) {
                    for (int c = 0; c < columns.size(); c++) {
                        cell(ws, row[0], c, columns.get(c).format(), values.get(c));
                    }
                    row[0]++;
                }
                row[0]++;
            });
            return this;
        }

        private Sheet text(String value, String label, boolean bold) {
            if (value != null && !value.isBlank()) {
                parts.add((ws, row) -> {
                    ws.value(row[0], 0, label + value);
                    if (bold) {
                        ws.style(row[0], 0).bold().set();
                    }
                    row[0]++;
                });
            }
            return this;
        }

        private static void cell(Worksheet ws, int row, int column, Format format, Object value) {
            if (value == null) {
                return;
            }
            switch (format) {
                case TEXT -> ws.value(row, column, value.toString());
                case NUMBER -> ws.value(row, column, (Number) value);
                case MONEY -> {
                    ws.value(row, column, (Number) value);
                    ws.style(row, column).format("#,##0").set();
                }
                case DATE -> {
                    ws.value(row, column, (LocalDate) value);
                    ws.style(row, column).format("dd/mm/yyyy").set();
                }
                case DATE_TIME -> {
                    ws.value(row, column, (LocalDateTime) value);
                    ws.style(row, column).format("dd/mm/yyyy hh:mm").set();
                }
            }
        }
    }
}
