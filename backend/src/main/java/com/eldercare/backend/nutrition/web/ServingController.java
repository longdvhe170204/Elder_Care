package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.nutrition.internal.serving.service.ServingService;
import com.eldercare.backend.nutrition.internal.serving.service.ServingService.Command;
import com.eldercare.backend.nutrition.internal.serving.service.ServingService.IncidentData;
import com.eldercare.backend.nutrition.web.request.ServingConfirmationRequest;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Xác nhận phục vụ suất đặc biệt của api-011 (thẻ {@code meal-slip}; FR-049 → FR-051, BR-M08-14). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "meal-slip")
public class ServingController {

    private static final String PROBLEM = "application/problem+json";

    private final ServingService serving;

    ServingController(ServingService serving) {
        this.serving = serving;
    }

    @PostMapping("/special-portions/{id}/confirm-serving")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> confirmServing(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @Valid @RequestBody ServingConfirmationRequest body) {
        IncidentData incident = body.incident() == null
                ? null
                : new IncidentData(
                        body.incident().severity(),
                        body.incident().symptoms(),
                        body.incident().treatment());
        serving.confirm(
                serving.targetOf(id), id, new Command(body.residentId(), body.hasEaten(), incident, body.deviceTime()));
        return ResponseEntity.status(201).build();
    }
}
