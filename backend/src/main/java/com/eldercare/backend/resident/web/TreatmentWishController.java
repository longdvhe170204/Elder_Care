package com.eldercare.backend.resident.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.resident.web.request.RecordTreatmentWishBody;
import com.eldercare.backend.resident.web.response.TreatmentWishResultBody;
import com.eldercare.backend.resident.web.response.TreatmentWishSummaryBody;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phiếu nguyện vọng khi điều trị (001 T080; contracts/api-001.yaml, tag {@code end-of-life-wishes}). Mọi logic và kiểm
 * quyền ở {@link TreatmentWishService}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "end-of-life-wishes")
class TreatmentWishController {

    private static final String PROBLEM = "application/problem+json";

    private final TreatmentWishService wishes;

    TreatmentWishController(TreatmentWishService wishes) {
        this.wishes = wishes;
    }

    @GetMapping("/residents/{id}/treatment-wishes")
    TreatmentWishSummaryBody get(@PathVariable long id) {
        return TreatmentWishSummaryBody.of(id, wishes.get(ResidentProfileService.target(id)), wishes);
    }

    @PostMapping("/residents/{id}/treatment-wishes")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Đã ghi phiên bản mới; warnings có NO_FAMILY_MEMBER_TO_NOTIFY khi hỏi gia đình trước mà"
                        + " chưa chọn người cần báo"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_FAMILY_MEMBER, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE, PREFERRED_HOSPITAL_REQUIRED, TOO_MANY_PREFERRED_HOSPITALS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<TreatmentWishResultBody> record(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordTreatmentWishBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TreatmentWishResultBody.of(
                        wishes.record(ResidentProfileService.target(id), body.command()), wishes));
    }
}
