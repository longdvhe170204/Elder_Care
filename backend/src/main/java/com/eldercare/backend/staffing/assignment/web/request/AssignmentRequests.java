package com.eldercare.backend.staffing.assignment.web.request;

import com.eldercare.backend.common.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.staffing.assignment.internal.domain.AssignmentInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.*;
import org.springframework.http.HttpStatus;

public final class AssignmentRequests {
    private AssignmentRequests() {}

    @Schema(name = "NewAssignment")
    public record New(
            @NotNull Long staffId,

            @NotBlank @Schema(allowableValues = {"FLOOR", "AREA", "ROOM", "RESIDENT"})
            String targetType,

            @NotNull Long targetId,
            Long taskTypeId,
            @NotBlank String role,

            @NotBlank @Schema(allowableValues = {"PRIMARY", "SUPPORT"})
            String assignmentKind,

            Boolean assignedNurse,
            Instant startsAt,
            Instant endsAt) {
        public AssignmentInput input() {
            try {
                return new AssignmentInput(
                        staffId,
                        targetType,
                        targetId,
                        taskTypeId,
                        Role.valueOf(role),
                        assignmentKind,
                        Boolean.TRUE.equals(assignedNurse),
                        startsAt,
                        endsAt);
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", "Vai trò phân công không hợp lệ");
            }
        }
    }

    public record Floor(
            @NotNull Long staffId,
            @NotNull Long floorAreaId,
            @NotNull LocalDate from,
            LocalDate to,
            @Size(max = 2000) String reason) {
        public Reason optionalReason() {
            return reason == null || reason.isBlank() ? null : new Reason(reason);
        }
    }

    public record EndFloor(
            @NotNull LocalDate to,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    public record ReasonBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    @Schema(name = "CopyAssignmentsRequest")
    public record Copy(@NotNull Long sourceShiftId) {}
}
