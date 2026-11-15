package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.billing.balance.internal.export.domain.ExportFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Thân {@code POST /balance-exports/income-expense-report} (FR-025 (b), Q-386). */
public record IncomeExpenseExportRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotNull ExportFormat format) {}
