package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.billing.balance.internal.export.domain.ExportFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Thân {@code POST /family-portal/residents/{residentId}/balance-statement} (FR-026). */
public record PortalStatementRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotNull ExportFormat format) {}
