package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.nutrition.internal.familyfood.service.FamilyFoodService;
import com.eldercare.backend.nutrition.internal.familyfood.service.FamilyFoodService.Command;
import com.eldercare.backend.nutrition.web.request.FamilyFoodCommandBody;
import com.eldercare.backend.nutrition.web.request.FamilyFoodRequest;
import com.eldercare.backend.nutrition.web.response.FamilyFoodResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Đồ ăn gia đình mang vào của api-011 (thẻ {@code family-food}; FR-052 → FR-057, BR-M08-04). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "family-food")
public class FamilyFoodController {

    private static final String PROBLEM = "application/problem+json";

    private final FamilyFoodService foods;

    FamilyFoodController(FamilyFoodService foods) {
        this.foods = foods;
    }

    @GetMapping("/residents/{residentId}/family-foods")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<FamilyFoodResponse> list(@PathVariable long residentId) {
        return foods.list(FamilyFoodService.target(residentId, 0), residentId).stream()
                .map(FamilyFoodResponse::of)
                .toList();
    }

    @PostMapping("/residents/{residentId}/family-foods")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "422",
            description = "Lỗi",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<FamilyFoodResponse> register(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long residentId,
            @Valid @RequestBody FamilyFoodRequest body) {
        var row = foods.register(
                FamilyFoodService.target(residentId, 0),
                residentId,
                new Command(
                        body.senderName(),
                        body.familyMemberId(),
                        body.type(),
                        body.description(),
                        body.ingredients(),
                        body.ingredientsUnknown(),
                        body.expiresAt(),
                        body.storageMethod()));
        return ResponseEntity.status(201).body(FamilyFoodResponse.of(row));
    }

    @PostMapping("/family-foods/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> command(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"approve-use", "reject-use", "record-disposal", "end"})
                    String command,
            @RequestBody(required = false) FamilyFoodCommandBody body) {
        FamilyFoodCommandBody b = body == null ? new FamilyFoodCommandBody(null, null, null, null) : body;
        var target = foods.targetOf(id);
        switch (command) {
            case "approve-use" -> foods.approveUse(target, id, b.note());
            case "reject-use" -> foods.rejectUse(target, id, b.reason());
            case "record-disposal" -> foods.recordDisposal(target, id, b.method(), b.returnedTo());
            case "end" -> foods.end(target, id, b.method(), b.returnedTo());
            default -> throw new NotFoundException();
        }
        return ResponseEntity.ok().build();
    }
}
