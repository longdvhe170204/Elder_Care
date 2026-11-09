package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.nutrition.internal.closing.service.AdjustmentService;
import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService;
import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService.DailyMealRow;
import com.eldercare.backend.nutrition.web.request.AssignDishRequest;
import com.eldercare.backend.nutrition.web.request.ExtraPortionRequestBody;
import com.eldercare.backend.nutrition.web.response.AdjustmentResponse;
import com.eldercare.backend.nutrition.web.response.ClosingResultResponse;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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
 * Chốt suất và phát sinh của api-011 (thẻ {@code portion-closing}; FR-033 → FR-043, BR-M08-01, 09, 13). {@code GET
 * /daily-meals} không khai schema trong hợp đồng; thân trả bữa ngày, giờ dự kiến, mốc mở chốt, đã chốt chưa.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "portion-closing")
public class PortionClosingController {

    private static final String PROBLEM = "application/problem+json";

    private final PortionClosingService closing;
    private final AdjustmentService adjustments;

    public PortionClosingController(PortionClosingService closing, AdjustmentService adjustments) {
        this.closing = closing;
        this.adjustments = adjustments;
    }

    @GetMapping("/daily-meals")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public List<Map<String, Object>> dailyMeals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return closing.dailyMeals(PortionClosingService.target(0), date).stream()
                .map(PortionClosingController::row)
                .toList();
    }

    @GetMapping("/daily-meals/{id}/precomputed")
    @ApiResponse(responseCode = "200", description = "OK")
    public ClosingResultResponse precomputed(@PathVariable long id) {
        return ClosingResultResponse.of(closing.precomputed(PortionClosingService.target(id), id));
    }

    @PostMapping("/daily-meals/{id}/close-portions")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<ClosingResultResponse> close(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @PathVariable long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClosingResultResponse.of(closing.close(PortionClosingService.target(id), id)));
    }

    @GetMapping("/daily-meals/{id}/adjustments")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<AdjustmentResponse> adjustments(@PathVariable long id) {
        return closing.adjustments(PortionClosingService.target(id), id).stream()
                .map(AdjustmentResponse::of)
                .toList();
    }

    @PostMapping("/adjustments/{id}/confirm")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> confirm(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @PathVariable long id) {
        adjustments.confirm(new Target("M08.ADJUSTMENT", Long.toString(id), null, null), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/daily-meals/{id}/extra-portions")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> extraPortion(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody ExtraPortionRequestBody body) {
        if (body.residentId() == null || body.floorAreaId() == null) {
            throw new InvalidValueException("Thiếu người cao tuổi hoặc tầng/khu");
        }
        long request = adjustments.requestExtra(
                new Target("M08.DAILY_MEAL", Long.toString(id), body.residentId(), body.floorAreaId()),
                id,
                body.residentId(),
                body.floorAreaId(),
                body.reason());
        return ResponseEntity.created(URI.create("/api/v1/adjustments/" + request))
                .build();
    }

    @PostMapping("/special-portions/{id}/serve-held-portion")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> serveHeld(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @PathVariable long id) {
        adjustments.serveHeld(adjustments.targetOfSpecial(id), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/special-portions/{id}/discard-held-portion")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> discardHeld(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @PathVariable long id) {
        adjustments.discardHeld(adjustments.targetOfSpecial(id), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/special-portions/{id}/assign-dish")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> assignDish(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @RequestBody AssignDishRequest body) {
        if (body.dishId() == null) {
            throw new InvalidValueException("Thiếu món chỉ định");
        }
        adjustments.assignDish(adjustments.targetOfSpecial(id), id, body.dishId(), body.note());
        return ResponseEntity.ok().build();
    }

    private static Map<String, Object> row(DailyMealRow r) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dailyMealId", r.meal().id());
        out.put("mealId", r.meal().mealId());
        out.put("mealName", r.meal().mealName());
        out.put("date", r.meal().date());
        out.put("expectedTime", r.meal().mealTime().orElse(null));
        out.put("closingOpensAt", r.closingOpensAt().orElse(null));
        out.put("closed", r.meal().closed());
        return out;
    }
}
