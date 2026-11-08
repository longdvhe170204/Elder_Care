package com.eldercare.backend.staffing.web;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveInput;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveView;
import com.eldercare.backend.staffing.internal.swap.service.LeaveService;
import com.eldercare.backend.staffing.web.request.LeaveRequests;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import com.eldercare.backend.staffing.web.response.SwapResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Yêu cầu nghỉ đột xuất (contracts/api-015.yaml, nhóm {@code leave-requests}); chỉ trực tuyến (FR-043). */
@RestController
@RequestMapping("/api/v1/leave-requests")
class LeaveController {

    private final LeaveService leaves;

    LeaveController(LeaveService leaves) {
        this.leaves = leaves;
    }

    @GetMapping
    List<LeaveView> list() {
        return leaves.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Các yêu cầu đã tạo"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    SwapResponses.CreatedLeaveRequests create(
            @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody LeaveRequests.CreateLeave body) {
        return new SwapResponses.CreatedLeaveRequests(leaves.create(new LeaveInput(
                body.kind(),
                body.staffId(),
                body.shiftIds(),
                body.fromDate(),
                body.toDate(),
                body.dates(),
                body.scopeType(),
                body.scopeId(),
                body.reason())));
    }

    @GetMapping("/{id}/pre-approval")
    List<Map<String, Object>> preApproval(@PathVariable long id) {
        return leaves.preApproval(id);
    }

    @PostMapping("/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    SwapResponses.CommandResult command(
            @PathVariable long id,
            @Parameter(schema = @Schema(allowableValues = {"approve", "reject", "cancel"})) @PathVariable
                    String command,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody(required = false) LeaveRequests.LeaveDecision body) {
        LeaveRequests.LeaveDecision b = body == null ? new LeaveRequests.LeaveDecision(null, null) : body;
        switch (command) {
            case "approve" -> {
                List<LeaveService.ItemDecision> items = b.items() == null
                        ? null
                        : b.items().stream()
                                .map(i -> new LeaveService.ItemDecision(
                                        i.shiftId(),
                                        i.date(),
                                        i.result() == LeaveRequests.Result.APPROVED,
                                        i.newLeadId()))
                                .toList();
                return new SwapResponses.CommandResult(
                        null, leaves.approve(id, new LeaveService.Decision(items, b.reason())));
            }
            case "reject" -> leaves.reject(id, reason(b));
            case "cancel" -> leaves.cancel(id, reason(b));
            default -> throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lệnh không tồn tại");
        }
        return new SwapResponses.CommandResult(null, null);
    }

    private static Reason reason(LeaveRequests.LeaveDecision body) {
        if (body.reason() == null || body.reason().isBlank()) {
            throw new ReasonRequiredException();
        }
        return new Reason(body.reason());
    }
}
