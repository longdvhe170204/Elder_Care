package com.eldercare.backend.family.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.internal.stay.domain.FamilyStay;
import com.eldercare.backend.family.internal.stay.service.FamilyStayService;
import com.eldercare.backend.family.web.request.FamilyStayCommandRequest;
import com.eldercare.backend.family.web.request.FamilyStayRegistrationRequest;
import com.eldercare.backend.family.web.response.CreatedResponse;
import com.eldercare.backend.family.web.response.FamilyStayResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lượt người thân ở lại (012 T035; contracts/api-012.yaml, tag {@code family-stays}; US8, FR-040 → FR-045). Lệnh trên lượt
 * trả {@code {id}} như lệnh của các story khác.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "family-stays")
class FamilyStayController {

    private static final String PROBLEM = "application/problem+json";

    private final FamilyStayService stays;

    FamilyStayController(FamilyStayService stays) {
        this.stays = stays;
    }

    @GetMapping("/family-stays")
    List<FamilyStayResponse> list(@RequestParam(required = false) Long residentId) {
        return stays.list(residentId).stream().map(this::response).toList();
    }

    @PostMapping("/family-stays")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "FAMILY_STAY_OVERLAP, ISOLATION_ACTIVE, RESIDENT_NOT_PRESENT",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<FamilyStayResponse> register(
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @Valid @RequestBody FamilyStayRegistrationRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(response(stays.register(body.registration())));
    }

    @PostMapping("/family-stays/{id}/{command}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                description = "STATE_CHANGED, FAMILY_STAY_OVERLAP, ROOMMATE_OPINION_REQUIRED, ISOLATION_ACTIVE,"
                        + " RESIDENT_NOT_PRESENT",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<CreatedResponse> command(
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"confirm", "reject", "start", "cancel", "extend", "end"})
                    String command,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody(required = false) FamilyStayCommandRequest body) {
        FamilyStayCommandRequest b = body == null ? FamilyStayCommandRequest.EMPTY : body;
        Target target = stays.stayTarget(id);
        var result = switch (command) {
            case "confirm" -> stays.confirm(id, b.roommateOpinion());
            case "reject" -> stays.reject(id, b.reason());
            case "start" -> stays.start(target, id, b.at());
            case "cancel" -> stays.cancel(target, id, b.reason());
            case "extend" -> stays.extend(target, id, b.newExpectedEndAt(), b.reason());
            case "end" -> stays.end(target, id, b.at());
            default -> throw new NotFoundException();
        };
        return ResponseEntity.ok(new CreatedResponse(result.id()));
    }

    private FamilyStayResponse response(SimpleRecord<FamilyStay> stay) {
        return FamilyStayResponse.of(stay, stays.nightCount(stay.content()));
    }
}
