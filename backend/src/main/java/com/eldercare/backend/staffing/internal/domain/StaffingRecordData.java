package com.eldercare.backend.staffing.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Metadata lần import giữ ID logic và dữ liệu gốc; không lưu tệp bảng tính. */
public final class StaffingRecordData {
    private StaffingRecordData() {}

    public record ImportBatch(
            @NotNull String fileName, @Min(1) int rowCount, Legacy legacy) {}

    public static final SharedKind<ImportBatch> IMPORT_BATCH =
            SharedKind.of("M09.STAFF_IMPORT_BATCH", ImportBatch.class);
}
