package com.eldercare.backend.resident.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.resident.internal.assessment.service.CareLevelChangeDecisionService;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.web.request.CareLevelChangeDecisionInputBody;
import com.eldercare.backend.resident.web.response.CareLevelChangeDecisionBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * Quyết định khi gia đình không đồng ý đổi mức chăm sóc (001 FR-036b, Q-424; contracts/api-001.yaml, tag {@code
 * assessments}).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "assessments")
class CareLevelChangeDecisionController {

    private final CareLevelChangeDecisionService decisions;

    CareLevelChangeDecisionController(CareLevelChangeDecisionService decisions) {
        this.decisions = decisions;
    }

    @PostMapping("/residents/{residentId}/care-level-change-decisions")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải Hành chính",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "NOT_LATEST_ASSESSMENT, CARE_LEVEL_CHANGE_NOT_NEEDED, CHANGE_REQUEST_EXISTS, DECISION_EXISTS,"
                                + " RISK_AGREEMENT_REQUIRED",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "REVIEW_DATE_INVALID",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    CareLevelChangeDecisionBody record(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody CareLevelChangeDecisionInputBody body) {
        return CareLevelChangeDecisionBody.of(
                decisions.record(ResidentProfileService.target(residentId), residentId, body.toInput()));
    }

    @GetMapping("/residents/{residentId}/care-level-change-decisions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "Thiếu quyền",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    List<CareLevelChangeDecisionBody> list(@PathVariable long residentId) {
        return decisions.list(ResidentProfileService.target(residentId), residentId).stream()
                .map(CareLevelChangeDecisionBody::of)
                .toList();
    }
}
