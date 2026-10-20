package com.cambae.backend.m15.account.offline.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m15.account.offline.internal.PendingReason;
import com.cambae.backend.m15.account.offline.internal.PendingReviewRecord;
import com.cambae.backend.m15.account.offline.internal.PendingReviewService;
import com.cambae.backend.m15.account.offline.internal.ReviewStatus;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bản ghi chờ xem lại (002 T058; contracts/api-002.yaml; FR-041c): danh sách bản ghi người dùng được quyết định, chấp
 * nhận, từ chối kèm lý do. ETag là {@code version} của dòng.
 */
@RestController
@RequestMapping("/api/v1/pending-review-records")
class PendingReviewController {

    private final PendingReviewService reviews;
    private final JsonMapper json;

    PendingReviewController(PendingReviewService reviews, JsonMapper json) {
        this.reviews = reviews;
        this.json = json;
    }

    @Schema(name = "PendingReviewRecord")
    record PendingReviewRecordBody(
            Long id,
            String recordType,
            Map<String, Object> content,
            Long recorderId,
            Instant deviceTime,
            Instant syncedAt,
            Instant lastConnectedAt,
            PendingReason pendingReason,
            Long floorAreaId,
            ReviewStatus status,
            Long decidedBy,
            Instant decidedAt,
            String rejectionReason,
            String reference) {}

    @Schema(name = "PendingReviewRecordPage")
    record PendingReviewRecordPage(List<PendingReviewRecordBody> items, String nextCursor) {}

    record PendingReviewRejectBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    @GetMapping
    @Tag(name = "offline")
    PendingReviewRecordPage list(@RequestParam(required = false) String cursor) {
        PendingReviewService.Page page = reviews.page(cursor);
        return new PendingReviewRecordPage(page.items().stream().map(this::body).toList(), page.nextCursor());
    }

    @PostMapping("/{id}/accept")
    @Tag(name = "offline")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<PendingReviewRecordBody> accept(@PathVariable long id, @RequestHeader("If-Match") String ifMatch) {
        return ok(reviews.accept(id, ifMatch));
    }

    @PostMapping("/{id}/reject")
    @Tag(name = "offline")
    ResponseEntity<PendingReviewRecordBody> reject(
            @PathVariable long id,
            @RequestHeader("If-Match") String ifMatch,
            @RequestBody PendingReviewRejectBody body) {
        return ok(reviews.reject(id, ifMatch, body.reason()));
    }

    private ResponseEntity<PendingReviewRecordBody> ok(PendingReviewRecord record) {
        return ResponseEntity.ok().eTag(VersionEtag.of(record.version())).body(body(record));
    }

    @SuppressWarnings("unchecked")
    private PendingReviewRecordBody body(PendingReviewRecord r) {
        return new PendingReviewRecordBody(
                r.id(),
                r.recordType(),
                json.readValue(r.content(), Map.class),
                r.recorderId(),
                r.deviceTime(),
                r.syncedAt(),
                r.lastConnectedAt(),
                r.pendingReason(),
                r.floorAreaId(),
                r.status(),
                r.decidedBy(),
                r.decidedAt(),
                r.rejectionReason(),
                r.reference());
    }
}
