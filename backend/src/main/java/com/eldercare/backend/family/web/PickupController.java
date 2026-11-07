package com.eldercare.backend.family.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.internal.pickup.service.PickupService;
import com.eldercare.backend.family.web.request.PickupExceptionCommandRequest;
import com.eldercare.backend.family.web.request.PickupExceptionRequest;
import com.eldercare.backend.family.web.request.RecordPickupRequest;
import com.eldercare.backend.family.web.response.CreatedResponse;
import com.eldercare.backend.family.web.response.PickupRecordResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quy trình đón, ngoại lệ đón (012 T021; contracts/api-012.yaml, tag {@code pickups}). Chỉ trực tuyến (FR-028): không có
 * đường ghi ngoại tuyến. Lập, lệnh trên ngoại lệ trả {@code {id}} dù hợp đồng không khai nội dung (như lệnh US1, US3).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "pickups")
class PickupController {

    private static final String PROBLEM = "application/problem+json";

    private final PickupService pickups;

    PickupController(PickupService pickups) {
        this.pickups = pickups;
    }

    @PostMapping("/residents/{residentId}/pickups")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED, SOURCE_COMMAND_PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "PICKUP_BLOCKED, PICKUP_EXCEPTION_EXPIRED (kèm blockReason), STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<PickupRecordResponse> record(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody RecordPickupRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PickupRecordResponse.of(pickups.record(residentId, body.command())));
    }

    @GetMapping("/residents/{residentId}/pickup-records")
    List<PickupRecordResponse> records(@PathVariable long residentId) {
        return pickups.list(new Target("M01.RESIDENT", String.valueOf(residentId), residentId, null)).stream()
                .map(PickupRecordResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/pickup-exceptions")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> createException(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody PickupExceptionRequest body) {
        long id = pickups.createException(
                        residentId, body.fullName(), body.idDocumentNumber(), body.relationship(), body.reason())
                .id();
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedResponse(id));
    }

    @PostMapping("/pickup-exceptions/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> command(
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"confirm", "approve", "reject", "cancel"}) String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody(required = false) PickupExceptionCommandRequest body) {
        PickupExceptionCommandRequest b = body == null ? PickupExceptionCommandRequest.EMPTY : body;
        Target target = pickups.exceptionTarget(id);
        var result = switch (command) {
            case "confirm" -> pickups.confirm(target, id);
            case "approve" -> pickups.approve(target, id, b.reason());
            case "reject" -> pickups.reject(target, id, b.reason());
            case "cancel" -> pickups.cancel(target, id, b.reason());
            default -> throw new NotFoundException();
        };
        return ResponseEntity.ok(new CreatedResponse(result.id()));
    }
}
