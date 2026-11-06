package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.nutrition.internal.assignment.domain.NewDietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.service.DietAssignmentService;
import com.eldercare.backend.nutrition.internal.assignment.service.DietAssignmentService.Proposal;
import com.eldercare.backend.nutrition.web.request.ApplyDietAssignmentRequest;
import com.eldercare.backend.nutrition.web.request.DietAssignmentRequest;
import com.eldercare.backend.nutrition.web.request.NutritionReason;
import com.eldercare.backend.nutrition.web.response.DietAssignmentResponse;
import com.eldercare.backend.nutrition.web.response.MenuCheckResultResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bản gán chế độ ăn của api-011 (thẻ {@code diet}; FR-010 → FR-017, BR-M08-03). Lập đề xuất trả kết quả đối chiếu và có
 * cần lý do chuyên môn ({@code MenuCheckResult}); id bản gán mới ở header {@code Location}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "diet")
public class DietAssignmentController {

    private static final String PROBLEM = "application/problem+json";

    private final DietAssignmentService assignments;

    public DietAssignmentController(DietAssignmentService assignments) {
        this.assignments = assignments;
    }

    @GetMapping("/residents/{residentId}/diet-assignments")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            description = "Không có quyền",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public List<DietAssignmentResponse> list(@PathVariable long residentId) {
        return assignments.list(DietAssignmentService.target(residentId, 0), residentId).stream()
                .map(DietAssignmentResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/diet-assignments")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<MenuCheckResultResponse> propose(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long residentId,
            @RequestBody DietAssignmentRequest body) {
        Proposal proposal = assignments.propose(
                DietAssignmentService.target(residentId, 0),
                residentId,
                new NewDietAssignment(
                        body.dietId() == null ? 0 : body.dietId(),
                        body.texture(),
                        body.personalRestrictions(),
                        body.personalInstruction(),
                        body.basis(),
                        body.basisId(),
                        body.reviewNoticeId(),
                        body.effectiveFrom()));
        return ResponseEntity.created(URI.create("/api/v1/diet-assignments/" + proposal.id()))
                .body(new MenuCheckResultResponse(
                        List.of(),
                        List.of(),
                        List.of(),
                        proposal.professionalReasonRequired(),
                        proposal.warningCodes()));
    }

    @PostMapping("/diet-assignments/{id}/apply")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> apply(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody ApplyDietAssignmentRequest body) {
        assignments.apply(assignments.targetOf(id), id, body.professionalReason(), body.confirmedWarnings());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/diet-assignments/{id}/cancel")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> cancel(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody NutritionReason body) {
        assignments.cancel(assignments.targetOf(id), id, body.reason());
        return ResponseEntity.ok().build();
    }
}
