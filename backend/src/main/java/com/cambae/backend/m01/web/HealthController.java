package com.cambae.backend.m01.web;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.StatusFilter;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService;
import com.cambae.backend.m01.internal.health.service.InitialHealthRecordService.RecordView;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.web.request.CorrectionRequestBody;
import com.cambae.backend.m01.web.request.HealthRecordContentBody;
import com.cambae.backend.m01.web.request.ReasonBody;
import com.cambae.backend.m01.web.request.RecordHealthItemBody;
import com.cambae.backend.m01.web.response.CorrectionResultBody;
import com.cambae.backend.m01.web.response.HealthItemBody;
import com.cambae.backend.m01.web.response.HealthItemResultBody;
import com.cambae.backend.m01.web.response.InitialHealthRecordBody;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
 * Hồ sơ sức khỏe ban đầu và mục sức khỏe (001 T032; contracts/api-001.yaml, tag {@code health}). Mục sức khỏe không có
 * lệnh sửa hay xóa (DBR-04); đính chính hồ sơ ban đầu chuyển tới 000.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "health")
class HealthController {

    private static final String PROBLEM = "application/problem+json";

    private final InitialHealthRecordService records;
    private final HealthItemService items;
    private final CorrectionApi corrections;

    HealthController(InitialHealthRecordService records, HealthItemService items, CorrectionApi corrections) {
        this.records = records;
        this.items = items;
        this.corrections = corrections;
    }

    @GetMapping("/residents/{id}/initial-health-record")
    ResponseEntity<InitialHealthRecordBody> getInitialRecord(@PathVariable long id) {
        RecordView view = records.get(ResidentProfileService.target(id)).orElseThrow(NotFoundException::new);
        return ok(view);
    }

    @PutMapping("/residents/{id}/initial-health-record")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "ALREADY_CONFIRMED, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "412",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<InitialHealthRecordBody> saveInitialRecord(
            @PathVariable long id,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody HealthRecordContentBody body) {
        return ok(records.saveDraft(ResidentProfileService.target(id), ifMatch, body.content()));
    }

    @PostMapping("/residents/{id}/initial-health-record/confirm")
    ResponseEntity<InitialHealthRecordBody> confirmInitialRecord(
            @PathVariable long id, @RequestHeader("If-Match") String ifMatch) {
        return ok(records.confirm(ResidentProfileService.target(id), ifMatch));
    }

    @PostMapping("/initial-health-records/{id}/correct")
    ResponseEntity<CorrectionResultBody> correctInitialRecord(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CorrectionRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CorrectionResultBody.of(corrections.create(
                        InitialHealthRecordService.RECORD_TYPE,
                        id,
                        body.type().correctionType(),
                        body.correctedContent(),
                        body.reason())));
    }

    @GetMapping("/residents/{id}/health-items")
    List<HealthItemBody> listItems(
            @PathVariable long id, @RequestParam(name = "status", defaultValue = "ACTIVE") StatusFilter status) {
        return items.list(ResidentProfileService.target(id), status).stream()
                .map(HealthItemBody::of)
                .toList();
    }

    @PostMapping("/residents/{id}/health-items")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi nhận"),
        @ApiResponse(
                responseCode = "409",
                description = "DUPLICATE_HEALTH_ITEM, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<HealthItemResultBody> recordItem(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordHealthItemBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HealthItemResultBody.of(items.record(ResidentProfileService.target(id), body.command())));
    }

    @PostMapping("/health-items/{id}/exclude")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "409",
                description = "ALREADY_EXCLUDED, PROFILE_CLOSED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    HealthItemBody excludeItem(
            @PathVariable long id,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody ReasonBody body) {
        return HealthItemBody.of(items.exclude(items.targetOf(id), id, body.reason()));
    }

    private static ResponseEntity<InitialHealthRecordBody> ok(RecordView view) {
        return ResponseEntity.ok().eTag(VersionEtag.of(view.version())).body(InitialHealthRecordBody.of(view));
    }
}
