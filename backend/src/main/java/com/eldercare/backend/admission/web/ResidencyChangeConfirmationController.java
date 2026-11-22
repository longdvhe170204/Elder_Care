package com.eldercare.backend.admission.web;

import com.eldercare.backend.admission.internal.change.service.RepresentativeChangeConfirmationService;
import com.eldercare.backend.admission.web.request.ResidencyChangeDeclineBody;
import com.eldercare.backend.admission.web.response.ResidencyChangeConfirmationBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Người đại diện xác nhận yêu cầu thay đổi lưu trú trên ứng dụng (FR-043c; contracts/api-004.yaml, tag {@code
 * change-requests}). Mọi logic và kiểm người đại diện ở service.
 */
@RestController
@RequestMapping("/api/v1/family-portal/residency-change-confirmations")
@Tag(name = "change-requests")
class ResidencyChangeConfirmationController {

    private static final String PROBLEM = "application/problem+json";

    private final RepresentativeChangeConfirmationService confirmations;

    ResidencyChangeConfirmationController(RepresentativeChangeConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải tài khoản người thân",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<ResidencyChangeConfirmationBody> pending() {
        return confirmations.pending().stream()
                .map(ResidencyChangeConfirmationBody::of)
                .toList();
    }

    @PostMapping("/{id}/accept")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã đồng ý", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải người đại diện hiện tại",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CONFIRMATION_NOT_AWAITING, REQUEST_NOT_DRAFT",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    void accept(@PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        confirmations.accept(id);
    }

    @PostMapping("/{id}/decline")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã ghi không đồng ý", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải người đại diện hiện tại",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "CONFIRMATION_NOT_AWAITING, REQUEST_NOT_DRAFT",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu lý do",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    void decline(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ResidencyChangeDeclineBody body) {
        confirmations.decline(id, body.reason());
    }
}
