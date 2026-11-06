package com.eldercare.backend.admission.web;

import com.eldercare.backend.admission.internal.change.service.ResidencyChangeService;
import com.eldercare.backend.admission.web.request.ResidencyChangeRequestInputBody;
import com.eldercare.backend.admission.web.response.ResidencyChangeRequestBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
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
 * Yêu cầu thay đổi lưu trú (004 US5; contracts/api-004.yaml, tag {@code change-requests}). Lập ở Nháp, sửa Nháp và xem; gửi duyệt,
 * duyệt, từ chối, hủy qua {@code /approval-requests} của 000. Mọi logic và kiểm quyền ở service.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "change-requests")
class ResidencyChangeRequestController {

    private static final String PROBLEM = "application/problem+json";

    private final ResidencyChangeService changes;

    ResidencyChangeRequestController(ResidencyChangeService changes) {
        this.changes = changes;
    }

    @PostMapping("/residency-change-requests")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã lập"),
        @ApiResponse(
                responseCode = "403",
                description = "Không có quyền lập loại này",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Giá trị không hợp lệ, EFFECTIVE_DATE_BEFORE_CONTRACT_START",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ResidencyChangeRequestBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidencyChangeRequestInputBody body) {
        ResidencyChangeService.View view = changes.create(
                changes.targetOf(body.contractId()), body.toContent(), body.requestedEffectiveDate(), body.reason());
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(VersionEtag.of(view.details().request().version()))
                .body(ResidencyChangeRequestBody.of(view));
    }

    /**
     * Sửa yêu cầu còn Nháp (T043, T047; FR-038, FR-039); gửi duyệt qua {@code /approval-requests/{id}/submit}. Hợp đồng
     * không có thân phản hồi; ETag mới để sửa tiếp, xem lại bằng {@code GET}.
     */
    @PutMapping("/residency-change-requests/{id}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Yêu cầu không còn Nháp, CONTRACT_NOT_ACTIVE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "If-Match không khớp",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> edit(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidencyChangeRequestInputBody body) {
        ResidencyChangeService.View view = changes.edit(
                changes.targetOf(body.contractId()),
                id,
                ifMatch,
                body.toContent(),
                body.requestedEffectiveDate(),
                body.reason());
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .build();
    }

    @GetMapping("/residency-change-requests/{id}")
    ResponseEntity<ResidencyChangeRequestBody> get(@PathVariable long id) {
        ResidencyChangeService.View view = changes.get(id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .body(ResidencyChangeRequestBody.of(view));
    }
}
