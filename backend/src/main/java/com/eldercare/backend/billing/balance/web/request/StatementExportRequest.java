package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.billing.balance.internal.export.domain.ExportFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Thân {@code POST /balance-exports/statement} (FR-025 (a)). */
public record StatementExportRequest(
        @NotEmpty List<@NotNull Long> residentIds,
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotNull ExportFormat format) {}
