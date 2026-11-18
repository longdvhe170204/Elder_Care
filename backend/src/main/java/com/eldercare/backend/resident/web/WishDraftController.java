package com.eldercare.backend.resident.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWishDraft;
import com.eldercare.backend.resident.internal.wish.domain.TreatmentWishDraft;
import com.eldercare.backend.resident.internal.wish.service.EndOfLifeWishService;
import com.eldercare.backend.resident.internal.wish.service.TreatmentWishService;
import com.eldercare.backend.resident.internal.wish.service.WishDraftService;
import com.eldercare.backend.resident.internal.wish.service.WishDraftService.Saved;
import com.eldercare.backend.resident.web.request.DraftEndOfLifeWishBody;
import com.eldercare.backend.resident.web.request.DraftTreatmentWishBody;
import com.eldercare.backend.resident.web.request.ReasonBody;
import com.eldercare.backend.resident.web.request.RecordTreatmentWishBody;
import com.eldercare.backend.resident.web.request.VerifyEndOfLifeWishBody;
import com.eldercare.backend.resident.web.response.EndOfLifeWishBody;
import com.eldercare.backend.resident.web.response.TreatmentWishBody;
import com.eldercare.backend.resident.web.response.TreatmentWishResultBody;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bản Nháp tự khai của phiếu nguyện vọng do Hành chính lập (001 T111; Q-412, FR-023n → FR-023q; contracts/api-001.yaml,
 * tag {@code end-of-life-wishes}). Lập, sửa, xem, Hủy nháp ở {@link WishDraftService}; kiểm chứng ở
 * {@link EndOfLifeWishService#verifyDraft}, {@link TreatmentWishService#verifyDraft}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "end-of-life-wishes")
class WishDraftController {

    private static final String PROBLEM = "application/problem+json";

    private final WishDraftService drafts;
    private final EndOfLifeWishService endOfLife;
    private final TreatmentWishService treatment;

    WishDraftController(WishDraftService drafts, EndOfLifeWishService endOfLife, TreatmentWishService treatment) {
        this.drafts = drafts;
        this.endOfLife = endOfLife;
        this.treatment = treatment;
    }

    @GetMapping("/residents/{id}/end-of-life-wish-draft")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponse(responseCode = "200", description = "OK")
    EndOfLifeWishBody endOfLifeDraft(@PathVariable long id) {
        return EndOfLifeWishBody.ofDraft(drafts.endOfLife(ResidentProfileService.target(id)), endOfLife);
    }

    @PutMapping("/residents/{id}/end-of-life-wish-draft")
    @ProblemResponses.Forbidden
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã sửa Nháp"),
        @ApiResponse(responseCode = "201", description = "Đã lập Nháp"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_RESPONDENT, INVALID_SIGNER, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE, RESPONDENT_REQUIRED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<EndOfLifeWishBody> saveEndOfLifeDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody DraftEndOfLifeWishBody body) {
        Saved<EndOfLifeWishDraft> saved = drafts.saveEndOfLife(ResidentProfileService.target(id), body.content());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(EndOfLifeWishBody.ofDraft(saved.draft(), endOfLife));
    }

    @PostMapping("/residents/{id}/end-of-life-wish-draft/verify")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nháp đã thành phiên bản Hiệu lực"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_SIGNER, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE, COURT_DECISION_REQUIRED, SELF_DETERMINATION_ASSESSMENT_REQUIRED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    EndOfLifeWishBody verifyEndOfLifeDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody VerifyEndOfLifeWishBody body) {
        return EndOfLifeWishBody.of(
                endOfLife.verifyDraft(ResidentProfileService.target(id), body.command(id)), endOfLife);
    }

    @PostMapping("/residents/{id}/end-of-life-wish-draft/discard")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponse(responseCode = "200", description = "Đã hủy Nháp")
    EndOfLifeWishBody discardEndOfLifeDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        return EndOfLifeWishBody.ofDraft(
                drafts.discardEndOfLife(ResidentProfileService.target(id), body.reason()), endOfLife);
    }

    @GetMapping("/residents/{id}/treatment-wish-draft")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponse(responseCode = "200", description = "OK")
    TreatmentWishBody treatmentDraft(@PathVariable long id) {
        return TreatmentWishBody.ofDraft(drafts.treatment(ResidentProfileService.target(id)), treatment);
    }

    @PutMapping("/residents/{id}/treatment-wish-draft")
    @ProblemResponses.Forbidden
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã sửa Nháp"),
        @ApiResponse(responseCode = "201", description = "Đã lập Nháp"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_FAMILY_MEMBER, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE, TOO_MANY_PREFERRED_HOSPITALS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<TreatmentWishBody> saveTreatmentDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody DraftTreatmentWishBody body) {
        Saved<TreatmentWishDraft> saved = drafts.saveTreatment(ResidentProfileService.target(id), body.content());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(TreatmentWishBody.ofDraft(saved.draft(), treatment));
    }

    @PostMapping("/residents/{id}/treatment-wish-draft/verify")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description =
                        "Nháp đã thành phiên bản Hiệu lực; warnings có NO_FAMILY_MEMBER_TO_NOTIFY khi hỏi gia đình"
                                + " trước mà chưa chọn người cần báo"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_FAMILY_MEMBER, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE, PREFERRED_HOSPITAL_REQUIRED, TOO_MANY_PREFERRED_HOSPITALS",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    TreatmentWishResultBody verifyTreatmentDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordTreatmentWishBody body) {
        return TreatmentWishResultBody.of(
                treatment.verifyDraft(ResidentProfileService.target(id), body.command()), treatment);
    }

    @PostMapping("/residents/{id}/treatment-wish-draft/discard")
    @ProblemResponses.Forbidden
    @ProblemResponses.NotFound
    @ApiResponse(responseCode = "200", description = "Đã hủy Nháp")
    TreatmentWishBody discardTreatmentDraft(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        return TreatmentWishBody.ofDraft(
                drafts.discardTreatment(ResidentProfileService.target(id), body.reason()), treatment);
    }
}
