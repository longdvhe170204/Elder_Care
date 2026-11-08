package com.eldercare.backend.nutrition.web;

import com.eldercare.backend.nutrition.internal.sample.service.RetainedSampleService;
import com.eldercare.backend.nutrition.internal.sample.service.RetainedSampleService.Command;
import com.eldercare.backend.nutrition.internal.sample.service.RetainedSampleService.DishEntry;
import com.eldercare.backend.nutrition.web.request.RetainedSampleRequest;
import com.eldercare.backend.nutrition.web.response.NutritionProblem;
import com.eldercare.backend.nutrition.web.response.RetainedSampleResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lưu mẫu thức ăn của api-011 (thẻ {@code review-and-sample}; FR-060 → FR-062, BR-M08-15). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "review-and-sample")
public class RetainedSampleController {

    private static final String PROBLEM = "application/problem+json";

    private final RetainedSampleService samples;

    RetainedSampleController(RetainedSampleService samples) {
        this.samples = samples;
    }

    @GetMapping("/daily-meals/{id}/retained-samples")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    public RetainedSampleResponse view(@PathVariable long id) {
        return RetainedSampleResponse.of(samples.view(RetainedSampleService.target(id), id));
    }

    @PostMapping("/daily-meals/{id}/retained-samples")
    @ApiResponse(responseCode = "201", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> record(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable long id,
            @Valid @RequestBody RetainedSampleRequest body) {
        samples.recordSample(
                RetainedSampleService.target(id),
                id,
                new Command(
                        body.storageLocation(),
                        body.dishes().stream()
                                .map(d -> new DishEntry(d.dishId(), d.retained(), d.notRetainedReason()))
                                .toList()));
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/retained-samples/{id}/discard")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    @ApiResponse(
            responseCode = "409",
            description = "Bị chặn",
            content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = NutritionProblem.class)))
    public ResponseEntity<Void> discard(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @PathVariable long id) {
        samples.discard(RetainedSampleService.target(id), id);
        return ResponseEntity.ok().build();
    }
}
