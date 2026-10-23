package com.cambae.backend.m01.web;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.wish.service.EndOfLifeWishService;
import com.cambae.backend.m01.web.request.RecordEndOfLifeWishBody;
import com.cambae.backend.m01.web.response.EndOfLifeWishBody;
import com.cambae.backend.m01.web.response.EndOfLifeWishSummaryBody;
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
 * Phiếu nguyện vọng cuối đời (001 T070; contracts/api-001.yaml, tag {@code end-of-life-wishes}). Mọi logic và kiểm quyền
 * ở {@link EndOfLifeWishService}; nguồn {@code CRITICAL_RECONFIRMATION} chỉ 007 ghi qua {@code EndOfLifeWishApi}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "end-of-life-wishes")
class EndOfLifeWishController {

    private static final String PROBLEM = "application/problem+json";

    private final EndOfLifeWishService wishes;

    EndOfLifeWishController(EndOfLifeWishService wishes) {
        this.wishes = wishes;
    }

    @GetMapping("/residents/{id}/end-of-life-wishes")
    EndOfLifeWishSummaryBody get(@PathVariable long id) {
        return EndOfLifeWishSummaryBody.of(id, wishes.get(ResidentProfileService.target(id)), wishes);
    }

    @PostMapping("/residents/{id}/end-of-life-wishes")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi phiên bản mới"),
        @ApiResponse(
                responseCode = "409",
                description = "INVALID_SIGNER, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "INVALID_OPINION_SOURCE, COURT_DECISION_REQUIRED, SELF_DETERMINATION_ASSESSMENT_REQUIRED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<EndOfLifeWishBody> record(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordEndOfLifeWishBody body) {
        if (body.source() == WishRecordSource.CRITICAL_RECONFIRMATION) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "SOURCE_NOT_ALLOWED",
                    "Nguồn xác nhận lại lúc nguy kịch chỉ do phân hệ cấp cứu ghi");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EndOfLifeWishBody.of(
                        wishes.record(ResidentProfileService.target(id), body.command(id), body.source()), wishes));
    }
}
