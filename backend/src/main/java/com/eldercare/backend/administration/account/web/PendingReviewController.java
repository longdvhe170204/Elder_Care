package com.eldercare.backend.administration.account.web;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.spi.FloorAreaNameApi;
import com.eldercare.backend.administration.account.internal.offline.domain.PendingReviewRecord;
import com.eldercare.backend.administration.account.internal.offline.service.PendingReviewService;
import com.eldercare.backend.administration.account.web.request.PendingReviewRejectBody;
import com.eldercare.backend.administration.account.web.response.PendingReviewRecordBody;
import com.eldercare.backend.administration.account.web.response.PendingReviewRecordPage;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.web.VersionEtag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
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
 * nhận, từ chối kèm lý do. ETag là {@code version} của dòng. Họ tên người ghi, tên tầng chỉ để hiển thị; chưa có
 * bản cài tên tầng thì để trống.
 */
@RestController
@RequestMapping("/api/v1/pending-review-records")
class PendingReviewController {

    private final PendingReviewService reviews;
    private final JsonMapper json;
    private final AccountApi accounts;
    private final ObjectProvider<FloorAreaNameApi> floorNames;

    PendingReviewController(
            PendingReviewService reviews,
            JsonMapper json,
            AccountApi accounts,
            ObjectProvider<FloorAreaNameApi> floorNames) {
        this.reviews = reviews;
        this.json = json;
        this.accounts = accounts;
        this.floorNames = floorNames;
    }

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
                accounts.fullName(r.recorderId()).orElse(null),
                r.deviceTime(),
                r.syncedAt(),
                r.lastConnectedAt(),
                r.pendingReason(),
                r.floorAreaId(),
                floorName(r.floorAreaId()),
                r.status(),
                r.decidedBy(),
                r.decidedAt(),
                r.rejectionReason(),
                r.reference(),
                r.version());
    }

    private String floorName(Long floorAreaId) {
        FloorAreaNameApi api = floorNames.getIfAvailable();
        return floorAreaId == null || api == null ? null : api.name(floorAreaId).orElse(null);
    }
}
