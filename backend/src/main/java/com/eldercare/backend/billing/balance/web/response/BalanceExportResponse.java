package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.balance.internal.export.domain.BalanceExportRecords.Type;
import com.eldercare.backend.billing.balance.internal.export.domain.ExportFormat;
import com.eldercare.backend.billing.balance.internal.export.service.BalanceExportService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Schema {@code BalanceExport} của api-017: một lần xuất. */
@Schema(name = "BalanceExport")
public record BalanceExportResponse(
        long id, Type type, ExportFormat format, LocalDate from, LocalDate to, int rowCount, Instant at) {

    public static BalanceExportResponse of(BalanceExportService.Export e) {
        return new BalanceExportResponse(e.id(), e.type(), e.format(), e.from(), e.to(), e.rowCount(), e.at());
    }
}
