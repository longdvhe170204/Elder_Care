package com.eldercare.backend.admission.web;

import com.eldercare.backend.admission.internal.change.service.ResidencyChangeService;
import com.eldercare.backend.admission.web.request.AddendumSignatureBody;
import com.eldercare.backend.admission.web.request.ResidencyChangeRequestInputBody;
import com.eldercare.backend.admission.web.request.ResidentOpinionInputBody;
import com.eldercare.backend.admission.web.response.ResidencyChangeRequestBody;
import com.eldercare.backend.admission.web.response.ResidentOpinionBody;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestParam;
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
                description =
                        "Giá trị không hợp lệ, EFFECTIVE_DATE_BEFORE_CONTRACT_START, SERVICE_TYPE_NOT_REGISTRABLE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ResidencyChangeRequestBody> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidencyChangeRequestInputBody body) {
        long contractId = changes.contractFor(body.contractId(), body.residentId());
        ResidencyChangeService.View view = changes.create(
                changes.targetOf(contractId), body.toContent(contractId), body.requestedEffectiveDate(), body.reason());
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
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Giá trị không hợp lệ, SERVICE_TYPE_NOT_REGISTRABLE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> edit(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ResidencyChangeRequestInputBody body) {
        long contractId = changes.contractFor(body.contractId(), body.residentId());
        ResidencyChangeService.View view = changes.edit(
                changes.targetOf(contractId),
                id,
                ifMatch,
                body.toContent(contractId),
                body.requestedEffectiveDate(),
                body.reason());
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .build();
    }

    /** Gửi yêu cầu ở Nháp cho người đại diện xác nhận trên ứng dụng (FR-043c); người đại diện được báo. */
    @PostMapping("/residency-change-requests/{id}/send-for-confirmation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã gửi"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải Hành chính, Quản lý viện",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "REQUEST_NOT_DRAFT, CONFIRMATION_NOT_REQUIRED, NO_REPRESENTATIVE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "If-Match không khớp",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ResidencyChangeRequestBody> sendForConfirmation(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        ResidencyChangeService.View current = changes.get(id);
        ResidencyChangeService.View view =
                changes.sendForConfirmation(changes.targetOf(current.content().contractId()), id, ifMatch);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .body(ResidencyChangeRequestBody.of(view));
    }

    /** Ghi nhận phụ lục đã ký (FR-043d, Q-419): phụ lục Chờ ký → Chờ hiệu lực; ký muộn thì dời ngày hiệu lực. */
    @PostMapping("/residency-change-requests/{id}/addendum-signature")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã ghi nhận"),
        @ApiResponse(
                responseCode = "403",
                description = "Không phải Hành chính, Quản lý viện",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "ADDENDUM_NOT_PENDING_SIGNATURE, SIGNER_NOT_REPRESENTATIVE, điều kiện giữ giường",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                description = "If-Match không khớp",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "SIGNED_DATE_INVALID, FACILITY_SIGNATORY_INVALID",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<ResidencyChangeRequestBody> recordSignature(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody AddendumSignatureBody body) {
        ResidencyChangeService.View current = changes.get(id);
        ResidencyChangeService.View view =
                changes.recordSignature(changes.targetOf(current.content().contractId()), id, ifMatch, body.toInput());
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .body(ResidencyChangeRequestBody.of(view));
    }

    /** Ý kiến người cao tuổi về yêu cầu, mới nhất trước (FR-043h). */
    @GetMapping("/residency-change-requests/{id}/resident-opinions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "404",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<ResidentOpinionBody> opinions(@PathVariable long id) {
        return changes.opinions(id).stream().map(ResidentOpinionBody::of).toList();
    }

    /** Ghi ý kiến người cao tuổi (FR-043h, Q-416): Hành chính, Bác sĩ, Điều dưỡng phụ trách; trả lịch sử ý kiến. */
    @PostMapping("/residency-change-requests/{id}/resident-opinions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Đã ghi"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "REQUEST_NOT_OPEN",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<ResidentOpinionBody> recordOpinion(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody ResidentOpinionInputBody body) {
        return changes.recordOpinion(id, body.toInput()).stream()
                .map(ResidentOpinionBody::of)
                .toList();
    }

    @GetMapping("/residency-change-requests")
    List<ResidencyChangeRequestBody> list(
            @RequestParam(name = "contractId", required = false) Long contractId,
            @RequestParam(name = "status", required = false) Set<String> status) {
        return changes.list(contractId, status).stream()
                .map(ResidencyChangeRequestBody::of)
                .toList();
    }

    @GetMapping("/residency-change-requests/{id}")
    ResponseEntity<ResidencyChangeRequestBody> get(@PathVariable long id) {
        ResidencyChangeService.View view = changes.get(id);
        return ResponseEntity.ok()
                .eTag(VersionEtag.of(view.details().request().version()))
                .body(ResidencyChangeRequestBody.of(view));
    }
}
