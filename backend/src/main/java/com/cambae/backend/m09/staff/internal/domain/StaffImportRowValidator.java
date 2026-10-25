package com.cambae.backend.m09.staff.internal.domain;

import com.cambae.backend.common.document.ExcelRow;
import java.time.LocalDate;
import java.time.format.*;
import java.util.*;

/** FR-001b: hàm thuần kiểm ô; trùng file và trùng dữ liệu được ghép sau theo tập số. */
public final class StaffImportRowValidator {
    private StaffImportRowValidator() {}

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    public static StaffImportData.Row validate(ExcelRow row, List<StaffImportData.Error> errors) {
        String name = row.text("Họ tên"),
                position = row.text("Chức danh"),
                phone = row.text("Số điện thoại"),
                date = row.text("Ngày bắt đầu làm việc"),
                email = row.text("Email");
        for (var entry : Map.of(
                        "fullName",
                        name == null ? "" : name,
                        "position",
                        position == null ? "" : position,
                        "phoneNumber",
                        phone == null ? "" : phone,
                        "startDate",
                        date == null ? "" : date)
                .entrySet())
            if (entry.getValue().isBlank())
                error(
                        errors,
                        row.rowNumber(),
                        entry.getKey(),
                        StaffImportData.ErrorCode.REQUIRED_CELL_MISSING,
                        "Ô bắt buộc chưa được điền");
        if (phone != null) {
            phone = phone.replaceAll("[\\s.\\-]", "");
            if (!phone.matches("0[0-9]{9}"))
                error(
                        errors,
                        row.rowNumber(),
                        "phoneNumber",
                        StaffImportData.ErrorCode.INVALID_PHONE_NUMBER_FORMAT,
                        "Cần 10 chữ số và bắt đầu bằng 0");
        }
        LocalDate day = null;
        if (date != null) {
            try {
                day = date.contains("/") ? LocalDate.parse(date, DATE) : row.date("Ngày bắt đầu làm việc");
            } catch (RuntimeException ex) {
                error(
                        errors,
                        row.rowNumber(),
                        "startDate",
                        StaffImportData.ErrorCode.INVALID_DATE_FORMAT,
                        "Cần ngày Excel hoặc dd/MM/yyyy hợp lệ");
            }
        }
        if (email != null && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            error(
                    errors,
                    row.rowNumber(),
                    "email",
                    StaffImportData.ErrorCode.INVALID_EMAIL_FORMAT,
                    "Email không đúng định dạng");
        return new StaffImportData.Row(row.rowNumber(), name, position, phone, day, row.text("Chuyên môn"), email);
    }

    private static void error(
            List<StaffImportData.Error> errors, int row, String column, StaffImportData.ErrorCode code, String reason) {
        errors.add(new StaffImportData.Error(row, column, code, reason, null, null));
    }
}
