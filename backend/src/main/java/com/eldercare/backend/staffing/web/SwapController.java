package com.eldercare.backend.staffing.web;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.staffing.internal.swap.domain.RecipientShift;
import com.eldercare.backend.staffing.internal.swap.domain.SwapChecks;
import com.eldercare.backend.staffing.internal.swap.domain.SwapView;
import com.eldercare.backend.staffing.internal.swap.service.SwapService;
import com.eldercare.backend.staffing.web.request.SwapRequests;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import com.eldercare.backend.staffing.web.response.SwapResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC-54: yêu cầu đổi ca (contracts/api-015.yaml, nhóm {@code shift-swaps}); chỉ trực tuyến (FR-043). */
@RestController
@RequestMapping("/api/v1/shift-swaps")
class SwapController {

    private final SwapService swaps;

    SwapController(SwapService swaps) {
        this.swaps = swaps;
    }

    @GetMapping
    List<SwapView> list() {
        return swaps.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập; Location là yêu cầu mới"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = M09ProblemBody.class)))
    })
    SwapChecks create(
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody SwapRequests.CreateShiftSwap body,
            HttpServletResponse response) {
        SwapService.Created created =
                swaps.create(body.kind(), body.shiftXId(), body.recipientId(), body.shiftYId(), body.reason());
        response.setHeader("Location", "/api/v1/shift-swaps/" + created.id());
        return created.checks();
    }

    @GetMapping("/recipient-shifts")
    List<RecipientShift> recipientShifts(@RequestParam long shiftXId, @RequestParam long recipientId) {
        return swaps.recipientShifts(shiftXId, recipientId);
    }

    @GetMapping("/{id}/checks")
    SwapChecks checks(@PathVariable long id) {
        return swaps.checks(id);
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
            @Parameter(
                            schema =
                                    @Schema(
                                            allowableValues = {
                                                "accept",
                                                "decline",
                                                "withdraw-acceptance",
                                                "cancel",
                                                "approve",
                                                "reject"
                                            }))
                    @PathVariable
                    String command,
            @RequestHeader("Idempotency-Key") UUID key,
            @RequestBody(required = false) SwapRequests.ShiftSwapCommand body) {
        SwapRequests.ShiftSwapCommand b =
                body == null ? new SwapRequests.ShiftSwapCommand(null, null, null, null) : body;
        switch (command) {
            case "accept" -> swaps.accept(id);
            case "decline" -> swaps.decline(id, required(b));
            case "withdraw-acceptance" -> swaps.withdraw(id, b.reason());
            case "cancel" -> swaps.cancel(id, required(b));
            case "reject" -> swaps.reject(id, required(b));
            case "approve" -> {
                return new SwapResponses.CommandResult(
                        swaps.approve(
                                id,
                                new SwapService.Decision(
                                        b.reason() == null ? null : b.reason().value(),
                                        b.newLeadXId(),
                                        b.newLeadYId(),
                                        Boolean.TRUE.equals(b.confirmWarning()))),
                        null);
            }
            default -> throw new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lệnh không tồn tại");
        }
        return new SwapResponses.CommandResult(null, null);
    }

    private static Reason required(SwapRequests.ShiftSwapCommand body) {
        if (body.reason() == null) {
            throw new ReasonRequiredException();
        }
        return body.reason();
    }
}
