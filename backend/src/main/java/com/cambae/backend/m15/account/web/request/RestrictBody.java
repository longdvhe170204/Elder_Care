package com.cambae.backend.m15.account.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.internal.permission.domain.PermissionViews.RestrictionKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân lệnh thu hẹp quyền theo vai trò hoặc tài khoản (contracts {@code RestrictBody}; FR-025). */
public record RestrictBody(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Mã dòng Phụ lục 27, ví dụ COST_ADJUSTMENT")
        String function,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Operation operation,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        RestrictionKind kind,

        @Schema(description = "Chỉ với DASHBOARD_REPORT (feature 016 FR-010a)")
        String metricGroup,

        Channel channel,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason) {

    /** Kênh của thu hẹp chi tiết dòng "Dashboard, báo cáo" (016 FR-010a). */
    public enum Channel {
        DASHBOARD,
        REPORT
    }
}
