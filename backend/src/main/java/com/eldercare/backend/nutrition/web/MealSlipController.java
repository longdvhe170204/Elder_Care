package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.nutrition.internal.slip.service.MealSlipService;
import com.eldercare.backend.nutrition.web.request.MealSlipCommandBody;
import com.eldercare.backend.nutrition.web.request.SupplementCommandBody;
import com.eldercare.backend.nutrition.web.response.FloorMealSlipResponse;
import com.eldercare.backend.nutrition.web.response.KitchenMealSlipResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Set;
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
 * Phiếu bữa ăn của api-011 (thẻ {@code meal-slip}; FR-044 → FR-048b, BR-M08-09 → 12): bếp nhận {@code KitchenMealSlip},
 * nhân viên tại tầng nhận {@code FloorMealSlip} trong phạm vi (SC-008). Lệnh của bếp và của người nhận tại tầng chung một
 * đường dẫn {@code /{command}}; controller chuyển theo lệnh.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "meal-slip")
public class MealSlipController {

    private static final String PROBLEM = "application/problem+json";
    private static final Set<String> KITCHEN = Set.of("mark-prepared", "deliver", "resolve-discrepancy");
    private static final Set<String> FLOOR = Set.of("receive", "report-discrepancy");

    private final MealSlipService slips;

    public MealSlipController(MealSlipService slips) {
        this.slips = slips;
    }

    @GetMapping("/meal-slips")
    @ApiResponse(
            responseCode = "200",
            description = "OK",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(oneOf = {KitchenMealSlipList.class, FloorMealSlipList.class})))
    public List<?> list(
            @RequestParam(required = false) Long dailyMealId, @RequestParam(required = false) Long floorAreaId) {
        if (dailyMealId == null) {
            throw new InvalidValueException("Thiếu bữa ngày");
        }
        if (slips.isKitchen()) {
            return slips.kitchenSlips(MealSlipService.target(0, floorAreaId), dailyMealId, floorAreaId).stream()
                    .map(KitchenMealSlipResponse::of)
                    .toList();
        }
        return slips.floorSlips(dailyMealId, floorAreaId).stream()
                .map(FloorMealSlipResponse::of)
                .toList();
    }

    @PostMapping("/special-portions/{id}/mark-prepared")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> markSpecialPrepared(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @PathVariable long id) {
        slips.markSpecialPrepared(slips.targetOfSpecial(id), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/meal-slips/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "403",
            description = "Lỗi theo RFC 9457 (research R-03 của 000)",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> command(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @PathVariable
                    @Schema(
                            allowableValues = {
                                "mark-prepared",
                                "deliver",
                                "receive",
                                "report-discrepancy",
                                "resolve-discrepancy"
                            })
                    String command,
            @RequestBody(required = false) MealSlipCommandBody body) {
        MealSlipCommandBody b = body == null ? new MealSlipCommandBody(null, null, null) : body;
        if (KITCHEN.contains(command)) {
            slips.kitchenCommand(slips.targetOf(id), id, command, b.resolution(), b.description());
        } else if (FLOOR.contains(command)) {
            slips.floorCommand(id, command, b.content());
        } else {
            throw new NotFoundException();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/supplementary-portions/{id}/{command}")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> supplementCommand(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @PathVariable @Schema(allowableValues = {"deliver", "receive", "report-discrepancy"}) String command,
            @RequestBody(required = false) SupplementCommandBody body) {
        switch (command) {
            case "deliver" -> slips.deliverSupplement(slips.targetOfSupplement(id), id);
            case "receive", "report-discrepancy" ->
                slips.supplementFloorCommand(id, command, body == null ? null : body.content());
            default -> throw new NotFoundException();
        }
        return ResponseEntity.ok().build();
    }

    /** Kiểu mảng cho {@code oneOf} của {@code GET /meal-slips} (springdoc). */
    @ArraySchema(schema = @Schema(implementation = KitchenMealSlipResponse.class))
    interface KitchenMealSlipList {}

    @ArraySchema(schema = @Schema(implementation = FloorMealSlipResponse.class))
    interface FloorMealSlipList {}
}
