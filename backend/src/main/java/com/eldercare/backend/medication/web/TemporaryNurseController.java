package com.eldercare.backend.medication.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.domain.TemporaryFloorNurseAssignment;
import com.eldercare.backend.medication.internal.dose.service.TemporaryNurseAssignmentService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Điều dưỡng tầng khác phụ trách tạm khi tầng không có Điều dưỡng (BR-M07-31, Q-436). */
@RestController
@Tag(name = "dose")
public class TemporaryNurseController {

    private static final String PROBLEM = "application/problem+json";

    private final TemporaryNurseAssignmentService service;
    private final CurrentUser currentUser;
    private final BusinessTime businessTime;

    public TemporaryNurseController(
            TemporaryNurseAssignmentService service, CurrentUser currentUser, BusinessTime businessTime) {
        this.service = service;
        this.currentUser = currentUser;
        this.businessTime = businessTime;
    }

    /** api-006 {@code TemporaryNurseRequest}. */
    public record TemporaryNurseRequest(
            @Schema(requiredMode = RequiredMode.REQUIRED) @NotNull
            Long nurseAccountId) {}

    /** api-006 {@code TemporaryNurse}. */
    public record TemporaryNurseBody(
            Long id,
            Long floorAreaId,
            Long shiftId,
            Long nurseAccountId,
            Long assignedById,
            Instant assignedAt,
            Instant expiresAt) {

        static TemporaryNurseBody of(TemporaryFloorNurseAssignment a) {
            return new TemporaryNurseBody(
                    a.getId(),
                    a.getFloorAreaId(),
                    a.getShiftId(),
                    a.getNurseAccountId(),
                    a.getAssignedById(),
                    a.getAssignedAt(),
                    a.getExpiresAt());
        }
    }

    @PostMapping("/api/v1/floors/{floorAreaId}/temporary-nurses")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã chỉ định"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public TemporaryNurseBody assign(
            @PathVariable("floorAreaId") Long floorAreaId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TemporaryNurseRequest body) {
        if (!currentUser.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException("Chỉ Quản lý viện chỉ định Điều dưỡng phụ trách tạm (BR-M07-31)");
        }
        return TemporaryNurseBody.of(
                service.assign(floorAreaId, body.nurseAccountId(), currentUser.accountId(), businessTime.now()));
    }

    @GetMapping("/api/v1/floors/{floorAreaId}/temporary-nurses")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<TemporaryNurseBody> list(@PathVariable("floorAreaId") Long floorAreaId) {
        return service.active(floorAreaId, businessTime.now()).stream()
                .map(TemporaryNurseBody::of)
                .toList();
    }
}
