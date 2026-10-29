package com.eldercare.backend.staffing.staff.web.response;

import com.eldercare.backend.staffing.staff.internal.domain.StaffImportData;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

public final class StaffImportResponses {
    private StaffImportResponses() {}

    @Schema(name = "StaffImportRow")
    public record Row(
            int rowNumber,
            String fullName,
            String position,
            String phoneNumber,
            LocalDate startDate,
            String specialty,
            String email) {}

    @Schema(name = "StaffImportRowError")
    public record Error(
            int rowNumber,
            String column,
            StaffImportData.ErrorCode errorCode,
            String reason,
            @Schema(nullable = true) Long duplicateStaffId,
            @Schema(nullable = true) Integer duplicateRowNumber) {}

    @Schema(name = "StaffImportPreview")
    public record Preview(String fileName, int rowCount, List<Row> rows, List<Error> errors, boolean canConfirm) {
        public static Preview of(StaffImportData.Preview p) {
            return new Preview(
                    p.fileName(),
                    p.rowCount(),
                    p.rows().stream()
                            .map(r -> new Row(
                                    r.rowNumber(),
                                    r.fullName(),
                                    r.position(),
                                    r.phoneNumber(),
                                    r.startDate(),
                                    r.specialty(),
                                    r.email()))
                            .toList(),
                    p.errors().stream()
                            .map(e -> new Error(
                                    e.rowNumber(),
                                    e.column(),
                                    e.errorCode(),
                                    e.reason(),
                                    e.duplicateStaffId(),
                                    e.duplicateRowNumber()))
                            .toList(),
                    p.canConfirm());
        }
    }

    @Schema(name = "StaffImportResult")
    public record Result(long importBatchId, int createdProfileCount, List<Long> staffIds) {
        public static Result of(StaffImportData.Result r) {
            return new Result(r.importBatchId(), r.createdProfileCount(), r.staffIds());
        }
    }
}
