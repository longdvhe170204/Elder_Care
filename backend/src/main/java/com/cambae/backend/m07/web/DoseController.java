package com.cambae.backend.m07.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.dto.AdministerDoseRequest;
import com.cambae.backend.m07.api.dto.RefuseDoseRequest;
import com.cambae.backend.m07.api.dto.SkipDoseRequest;
import com.cambae.backend.m07.internal.domain.DoseReviewStatus;
import com.cambae.backend.m07.internal.service.DoseAdministrationService;
import com.cambae.backend.m07.internal.service.DoseReviewService;
import com.cambae.backend.m07.internal.service.DoseViewService;
import com.cambae.backend.m07.web.request.DoseRequests.DoseResultRequest;
import com.cambae.backend.m07.web.request.DoseRequests.DoseReviewResultRequest;
import com.cambae.backend.m07.web.request.DoseRequests.ReviewResult;
import com.cambae.backend.m07.web.response.DoseBodies.DoseBody;
import com.cambae.backend.m07.web.response.DoseBodies.DoseReviewBody;
import com.cambae.backend.m15.account.api.CurrentUser;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Xác nhận liều và xem lại bản ghi theo api-006 (thẻ {@code dose}; FR-020 → FR-033, FR-029). */
@RestController
@Tag(name = "dose")
public class DoseController {

    private static final String PROBLEM = "application/problem+json";

    private final DoseAdministrationService doseAdministrationService;
    private final DoseReviewService doseReviewService;
    private final DoseViewService doseViews;
    private final CurrentUser currentUser;
    private final BusinessTime businessTime;

    public DoseController(
            DoseAdministrationService doseAdministrationService,
            DoseReviewService doseReviewService,
            DoseViewService doseViews,
            CurrentUser currentUser,
            BusinessTime businessTime) {
        this.doseAdministrationService = doseAdministrationService;
        this.doseReviewService = doseReviewService;
        this.doseViews = doseViews;
        this.currentUser = currentUser;
        this.businessTime = businessTime;
    }

    @PostMapping("/api/v1/medication-doses/{doseKey}/administer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã dùng"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public DoseBody administer(
            @PathVariable("doseKey") String doseKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DoseResultRequest body) {
        var reporter = body.reportedBy();
        AdministerDoseRequest request = new AdministerDoseRequest(
                body.administeredAt() != null ? body.administeredAt() : businessTime.now(),
                null,
                reporter == null || reporter.type() == null
                        ? null
                        : reporter.type().name(),
                reporter == null ? null : reporter.id(),
                reporter == null ? null : reporter.fullName(),
                reporter == null ? null : reporter.relationship(),
                body.adverseReaction(),
                body.note(),
                Boolean.TRUE.equals(body.recordedAfterDisconnect()),
                body.familyMedicationBatchId(),
                null);
        return DoseBody.of(doseViews.view(doseAdministrationService.administerDose(
                doseKey, request, currentUser.accountId(), null, businessTime.now())));
    }

    @PostMapping("/api/v1/medication-doses/{doseKey}/refuse")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Từ chối", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> refuse(
            @PathVariable("doseKey") String doseKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DoseResultRequest body) {
        doseAdministrationService.refuseDose(
                doseKey,
                new RefuseDoseRequest(
                        body.reason(), body.note(), null, Boolean.TRUE.equals(body.recordedAfterDisconnect())),
                currentUser.accountId(),
                null,
                businessTime.now());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/v1/medication-doses/{doseKey}/skip")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Không thực hiện", content = @Content),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> skip(
            @PathVariable("doseKey") String doseKey,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DoseResultRequest body) {
        doseAdministrationService.skipDose(
                doseKey,
                new SkipDoseRequest(
                        body.reason(), body.note(), null, Boolean.TRUE.equals(body.recordedAfterDisconnect())),
                currentUser.accountId(),
                null,
                businessTime.now());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/v1/dose-reviews")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<DoseReviewBody> listReviews(@RequestParam(name = "status", required = false) DoseReviewStatus status) {
        return doseReviewService.listReviews(status).stream()
                .map(DoseReviewBody::of)
                .toList();
    }

    @PostMapping("/api/v1/dose-reviews/{id}/result")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Void> processReview(
            @PathVariable("id") Long reviewId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DoseReviewResultRequest body) {
        doseReviewService.reviewDose(
                reviewId,
                body.result() == ReviewResult.ACCEPTED,
                body.note(),
                currentUser.accountId(),
                businessTime.now());
        return ResponseEntity.ok().build();
    }
}
