package com.eldercare.backend.family.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.internal.visit.service.VisitService;
import com.eldercare.backend.family.web.request.VisitCommandRequest;
import com.eldercare.backend.family.web.request.VisitRegistrationRequest;
import com.eldercare.backend.family.web.request.VisitRegistrationRequest.CompanionBody;
import com.eldercare.backend.family.web.response.CreatedResponse;
import com.eldercare.backend.family.web.response.VisitResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
 * Đăng ký thăm, ghi vào/ra (012 T025; contracts/api-012.yaml, tag {@code visits}; US4, FR-029 → FR-039). Lệnh trên lượt
 * trả {@code {id}} dù hợp đồng không khai nội dung (như lệnh US1 → US3).
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "visits")
class VisitController {

    private static final String PROBLEM = "application/problem+json";

    private final VisitService visits;

    VisitController(VisitService visits) {
        this.visits = visits;
    }

    @GetMapping("/visit-slots")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    List<VisitService.SlotView> slots(@RequestParam LocalDate date) {
        return visits.slots(date);
    }

    @GetMapping("/visits")
    List<VisitResponse> list(
            @RequestParam(required = false) LocalDate date, @RequestParam(required = false) Long residentId) {
        return visits.list(residentId, date).stream().map(VisitResponse::of).toList();
    }

    @PostMapping("/visits")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                description = "Không đạt FR-030: mọi lý do ở unmetConditions",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<VisitResponse> register(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody VisitRegistrationRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(VisitResponse.of(visits.register(body.registration())));
    }

    @PostMapping("/visits/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED, START_TIME_PASSED, ISOLATION_ACTIVE, RESIDENT_NOT_PRESENT",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> command(
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"cancel", "check-in", "check-out"}) String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody(required = false) VisitCommandRequest body) {
        VisitCommandRequest b = body == null ? VisitCommandRequest.EMPTY : body;
        Target target = visits.visitTarget(id);
        var result = switch (command) {
            case "cancel" -> visits.cancel(target, id, b.reason());
            case "check-in" -> visits.checkIn(target, id, CompanionBody.all(b.actualVisitors()));
            case "check-out" -> visits.checkOut(target, id, b.checkOutAt());
            default -> throw new NotFoundException();
        };
        return ResponseEntity.ok(new CreatedResponse(result.id()));
    }
}
