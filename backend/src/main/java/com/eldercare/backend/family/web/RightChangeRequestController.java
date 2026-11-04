package com.eldercare.backend.family.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest;
import com.eldercare.backend.family.internal.right.service.RightChangeRequestService;
import com.eldercare.backend.family.internal.right.service.VisitPickupWishService;
import com.eldercare.backend.family.web.request.CreateRightChangeRequestBody;
import com.eldercare.backend.family.web.request.RightChangeCommandRequest;
import com.eldercare.backend.family.web.request.VisitPickupWishRequest;
import com.eldercare.backend.family.web.response.RightChangeRequestResponse;
import com.eldercare.backend.family.web.response.VisitPickupWishResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Yêu cầu thay đổi quyền, danh sách đón, mong muốn của người cao tuổi (012 T017; contracts/api-012.yaml, tag
 * {@code right-change-requests}, {@code family-members}). Danh sách đón và lệnh trả bản ghi hiện hành nhưng hợp đồng không
 * khai nội dung nên OpenAPI không có. Mọi kiểm tra ở {@link RightChangeRequestService}, {@link VisitPickupWishService}.
 */
@RestController
@RequestMapping("/api/v1")
class RightChangeRequestController {

    private static final String PROBLEM = "application/problem+json";

    private final RightChangeRequestService requests;
    private final VisitPickupWishService wishes;

    RightChangeRequestController(RightChangeRequestService requests, VisitPickupWishService wishes) {
        this.requests = requests;
        this.wishes = wishes;
    }

    @GetMapping("/right-change-requests")
    @Tag(name = "right-change-requests")
    List<RightChangeRequestResponse> list(
            @RequestParam(name = "residentId", required = false) Long residentId,
            @RequestParam(name = "status", required = false) RightChangeRequest.Status status) {
        return requests.list(residentId, status).stream()
                .map(RightChangeRequestResponse::of)
                .toList();
    }

    @PostMapping("/right-change-requests")
    @Tag(name = "right-change-requests")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "REQUEST_NOT_ALLOWED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "DUPLICATE_PENDING_REQUEST, ID_DOCUMENT_REQUIRED, CONSENT_NOT_INCLUDED, DBR_02_VIOLATED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<RightChangeRequestResponse> create(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody CreateRightChangeRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RightChangeRequestResponse.of(requests.create(body.command(), body.reason())));
    }

    @PostMapping("/right-change-requests/{id}/{command}")
    @Tag(name = "right-change-requests")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "REQUEST_NOT_ALLOWED, NOMINEE_NOT_CONFIRMED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED, WISH_CONFLICT_REQUIRES_MANAGER",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<RightChangeRequestResponse> command(
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"confirm", "approve", "reject", "cancel"}) String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody(required = false) RightChangeCommandRequest body) {
        RightChangeCommandRequest b = body == null ? RightChangeCommandRequest.EMPTY : body;
        Target target = requests.requestTarget(id);
        var result = switch (command) {
            case "confirm" -> requests.confirm(target, id, b.decision());
            case "approve" -> requests.approve(target, id, b.reason(), b.decision());
            case "reject" -> requests.reject(target, id, b.reason());
            case "cancel" -> requests.cancel(target, id, b.reason());
            default -> throw new NotFoundException();
        };
        return ResponseEntity.ok(RightChangeRequestResponse.of(result));
    }

    @GetMapping("/residents/{residentId}/pickup-list")
    @Tag(name = "family-members")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    List<FamilyMemberView> pickupList(
            @PathVariable long residentId, @RequestParam(name = "at", required = false) Instant at) {
        return requests.pickupList(FamilyMemberService.residentTarget(residentId), at);
    }

    @GetMapping("/residents/{residentId}/visit-pickup-wishes")
    @Tag(name = "family-members")
    VisitPickupWishResponse.Read wishes(@PathVariable long residentId) {
        return VisitPickupWishResponse.Read.of(wishes.wishes(FamilyMemberService.residentTarget(residentId)));
    }

    @PostMapping("/residents/{residentId}/visit-pickup-wishes")
    @Tag(name = "family-members")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã ghi"),
        @ApiResponse(responseCode = "403", description = "Ngoài phạm vi hoặc không có quyền", content = @Content),
        @ApiResponse(
                responseCode = "422",
                description = "Thiếu người được nói tới; INVALID_OPINION_SOURCE",
                content = @Content)
    })
    ResponseEntity<VisitPickupWishResponse> recordWish(
            @PathVariable long residentId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody VisitPickupWishRequest body) {
        Target target = FamilyMemberService.residentTarget(residentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VisitPickupWishResponse.of(wishes.record(target, body.data())));
    }
}
