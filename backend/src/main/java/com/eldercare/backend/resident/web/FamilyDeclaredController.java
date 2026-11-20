package com.eldercare.backend.resident.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.FamilyDeclaredView;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.web.request.FamilyDeclaredBody;
import com.eldercare.backend.resident.web.response.FamilyDeclaredAssessmentBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phần "Gia đình khai" của lần đánh giá đầu vào do Hành chính ghi (001 T112; Q-412, FR-025a; contracts/api-001.yaml, tag
 * {@code assessments}). Mọi logic và kiểm quyền ở {@link AssessmentService}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "assessments")
class FamilyDeclaredController {

    private static final String PROBLEM = "application/problem+json";

    private final AssessmentService assessments;

    FamilyDeclaredController(AssessmentService assessments) {
        this.assessments = assessments;
    }

    @GetMapping("/residents/{id}/initial-assessment/family-declared")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponse(responseCode = "200", description = "OK")
    FamilyDeclaredAssessmentBody get(@PathVariable long id) {
        return FamilyDeclaredAssessmentBody.of(assessments.familyDeclared(ResidentProfileService.target(id)));
    }

    @PutMapping("/residents/{id}/initial-assessment/family-declared")
    @ProblemResponses.Forbidden
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã ghi vào bản Nháp đầu vào đang mở"),
        @ApiResponse(responseCode = "201", description = "Đã tạo bản Nháp đầu vào"),
        @ApiResponse(
                responseCode = "409",
                description = "INITIAL_ASSESSMENT_EXISTS, STATUS_NOT_ASSESSABLE, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<FamilyDeclaredAssessmentBody> record(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody FamilyDeclaredBody body) {
        FamilyDeclaredView saved = assessments.recordFamilyDeclared(ResidentProfileService.target(id), body.input());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(FamilyDeclaredAssessmentBody.of(saved));
    }
}
