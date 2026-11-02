package com.eldercare.backend.common.document;

import com.eldercare.backend.common.error.InvalidValueException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.Row;

/** Một dòng dữ liệu đọc theo mẫu ({@link ExcelDocument#read}); lấy ô theo tên cột của mẫu. */
public final class ExcelRow {

    private final Row row;
    private final Map<String, Integer> columns;

    ExcelRow(Row row, Map<String, Integer> columns) {
        this.row = row;
        this.columns = columns;
    }

    /** Số dòng như Excel hiện (bắt đầu từ 1), để báo lỗi từng dòng. */
    public int rowNumber() {
        return row.getRowNum();
    }

    /** Chữ của ô đã bỏ khoảng trắng hai đầu; ô trống thì {@code null}. */
    public String text(String header) {
        Cell cell = cell(header);
        if (cell == null || cell.getText().isBlank()) {
            return null;
        }
        return cell.getText().strip();
    }

    /** Số của ô; ô trống thì {@code null}, không phải số thì 422. */
    public BigDecimal number(String header) {
        Cell cell = cell(header);
        if (cell == null || cell.getType() == CellType.EMPTY) {
            return null;
        }
        if (cell.getType() != CellType.NUMBER) {
            throw invalid(header, "phải là số");
        }
        return cell.asNumber();
    }

    /** Ngày của ô định dạng ngày; ô trống thì {@code null}, không phải ngày thì 422. */
    public LocalDate date(String header) {
        Cell cell = cell(header);
        if (cell == null || cell.getType() == CellType.EMPTY) {
            return null;
        }
        if (cell.getType() != CellType.NUMBER) {
            throw invalid(header, "phải là ngày");
        }
        return cell.asDate().toLocalDate();
    }

    private Cell cell(String header) {
        Integer index = columns.get(header);
        if (index == null) {
            throw new IllegalArgumentException("Mẫu không có cột " + header);
        }
        return row.getOptionalCell(index).orElse(null);
    }

    private InvalidValueException invalid(String header, String detail) {
        return new InvalidValueException("Dòng " + rowNumber() + ", cột " + header + ": " + detail);
    }
}
