package com.eldercare.backend.admission.web;

import com.eldercare.backend.admission.api.dto.EndCase;
import com.eldercare.backend.admission.internal.ending.service.EndingRequestService;
import com.eldercare.backend.admission.web.response.ResidencyEndingRequestBody;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.web.ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
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

/**
 * Yêu cầu kết thúc lưu trú người đại diện gửi trên app (demo 2026-11-26, E; api-004 tag {@code residency-ending}). Mọi
 * kiểm người đại diện, trạng thái ở service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "residency-ending")
class EndingRequestController {

    private static final String PROBLEM = "application/problem+json";

    private final EndingRequestService requests;

    EndingRequestController(EndingRequestService requests) {
        this.requests = requests;
    }

    /** api-004 {@code ResidencyEndingRequestInput}. */
    @Schema(name = "ResidencyEndingRequestInput")
    record RequestInput(
            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            ResidencyEndingRequestBody.FamilyEndCase endCase,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate expectedEndDate,

            @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    @GetMapping("/family-portal/residents/{residentId}/residency-ending-requests")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải người đại diện hiện tại",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<ResidencyEndingRequestBody> forRepresentative(@PathVariable long residentId) {
        return requests.forRepresentative(residentId).stream()
                .map(ResidencyEndingRequestBody::of)
                .toList();
    }

    @PostMapping("/family-portal/residents/{residentId}/residency-ending-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã gửi"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải người đại diện hiện tại",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "RESIDENT_NOT_IN_RESIDENCE, RESIDENCY_ENDING_ALREADY_OPEN, ENDING_REQUEST_ALREADY_PENDING",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Trường hợp, ngày hoặc lý do không hợp lệ",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidencyEndingRequestBody submit(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RequestInput body) {
        return ResidencyEndingRequestBody.of(requests.submit(
                residentId,
                EndCase.valueOf(body.endCase().name()),
                body.expectedEndDate(),
                body.reason().value()));
    }

    @PostMapping("/family-portal/residency-ending-requests/{id}/cancel")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã hủy"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải người đại diện hiện tại",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "ENDING_REQUEST_NOT_PENDING",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResidencyEndingRequestBody cancel(
            @PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        return ResidencyEndingRequestBody.of(requests.cancel(id));
    }
}
