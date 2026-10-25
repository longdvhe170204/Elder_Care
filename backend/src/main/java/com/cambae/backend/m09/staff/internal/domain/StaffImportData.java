package com.cambae.backend.m09.staff.internal.domain;

import java.time.LocalDate;
import java.util.List;

/** Nội dung import đã kiểm, không giữ file gốc. */
public final class StaffImportData {
    private StaffImportData() {}

    public record Row(
            int rowNumber,
            String fullName,
            String position,
            String phoneNumber,
            LocalDate startDate,
            String specialty,
            String email) {}

    public enum ErrorCode {
        REQUIRED_CELL_MISSING,
        INVALID_DATE_FORMAT,
        INVALID_PHONE_NUMBER_FORMAT,
        INVALID_EMAIL_FORMAT,
        DUPLICATE_STAFF_PROFILE,
        DUPLICATE_FILE_ROW
    }

    public record Error(
            int rowNumber,
            String column,
            ErrorCode errorCode,
            String reason,
            Long duplicateStaffId,
            Integer duplicateRowNumber) {}

    public record Preview(String fileName, int rowCount, List<Row> rows, List<Error> errors, boolean canConfirm) {}

    public record Result(long importBatchId, int createdProfileCount, List<Long> staffIds) {}
}
