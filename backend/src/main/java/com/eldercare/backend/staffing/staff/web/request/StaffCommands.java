package com.eldercare.backend.staffing.staff.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class StaffCommands {
    private StaffCommands() {}

    public record Terminate(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            @NotNull Instant effectiveAt,
            Long replacementStaffId) {}

    @Schema(name = "ReasonBody")
    public record ReasonBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record ChangeDepartment(
            @NotNull Long departmentId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason,

            Long replacementStaffId) {}

    public record AssignLead(
            @NotNull Long staffId,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record NewDepartment(@NotBlank String name, Boolean active) {}
}
