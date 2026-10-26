package com.cambae.backend.m02.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m02.internal.change.service.ResidencyChangeService;
import com.cambae.backend.m02.web.request.ResidencyChangeRequestInputBody;
import com.cambae.backend.m02.web.response.ResidencyChangeRequestBody;
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
 * Yêu cầu thay đổi lưu trú (004 US5; contracts/api-004.yaml, tag {@code change-requests}). Chỉ lập ở Nháp và xem; gửi duyệt,
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

    @GetMapping("/residency-change-requests/{id}")
    ResponseEntity<ResidencyChangeRequestBody> get(@PathVariable long id) {
        ResidencyChangeService.View view = changes.get(id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .body(ResidencyChangeRequestBody.of(view));
    }
}
